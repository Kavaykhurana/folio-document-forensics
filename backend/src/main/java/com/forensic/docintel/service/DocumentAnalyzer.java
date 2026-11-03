package com.forensic.docintel.service;

import com.forensic.docintel.model.DocumentRecord;
import com.forensic.docintel.model.Finding;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

@Service
public class DocumentAnalyzer {
    @Value("${app.tesseract:tesseract}") private String tesseract;
    private static final Map<String, Pattern> FIELDS = new LinkedHashMap<>();
    static {
        FIELDS.put("applicant_name", Pattern.compile("(?im)^(?:applicant\\s+name|name|customer\\s+name)\\s*[:#-]\\s*(.+?)\\s*$"));
        FIELDS.put("date_of_birth", Pattern.compile("(?im)^(?:date\\s+of\\s+birth|dob)\\s*[:#-]\\s*(.+?)\\s*$"));
        FIELDS.put("national_id", Pattern.compile("(?im)^(?:national\\s+id|id\\s+number|aadhaar|passport\\s+(?:no|number))\\s*[:#-]\\s*([A-Z0-9 -]{4,})\\s*$"));
        FIELDS.put("address", Pattern.compile("(?im)^(?:address|residential\\s+address)\\s*[:#-]\\s*(.+?)\\s*$"));
        FIELDS.put("account_number", Pattern.compile("(?im)^(?:account\\s+(?:no|number)|account\\s+id)\\s*[:#-]\\s*([A-Z0-9 -]{4,})\\s*$"));
        FIELDS.put("declared_income", Pattern.compile("(?im)^(?:annual\\s+income|declared\\s+income|income)\\s*[:#-]\\s*(((?:INR|USD|EUR|GBP)\\s*)?[₹$€£]?\\s*[\\d,]+(?:\\.\\d{1,2})?(?:\\s*(?:INR|USD|EUR|GBP|per\\s+annum|p\\.?a\\.?))?)\\s*$"));
        FIELDS.put("document_date", Pattern.compile("(?im)^(?:document\\s+date|issue\\s+date|date)\\s*[:#-]\\s*(.+?)\\s*$"));
        FIELDS.put("signature", Pattern.compile("(?im)^.*\\b(signature|signed by)\\b.*$"));
    }

    public DocumentRecord analyze(MultipartFile upload, Path storageDir) throws Exception {
        String safeName = Optional.ofNullable(upload.getOriginalFilename()).orElse("document").replaceAll("[^A-Za-z0-9._-]", "_");
        byte[] bytes = upload.getBytes();
        if (bytes.length == 0) throw new IllegalArgumentException("Empty file: " + safeName);
        if (bytes.length > 15 * 1024 * 1024) throw new IllegalArgumentException("File exceeds 15 MB: " + safeName);
        String ext = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!Set.of("pdf", "png", "jpg", "jpeg", "tif", "tiff", "bmp").contains(ext)) throw new IllegalArgumentException("Unsupported file type for " + safeName + ". Upload PDF, PNG, JPG, TIFF, or BMP.");
        Files.createDirectories(storageDir);
        String id = UUID.randomUUID().toString(); Path original = storageDir.resolve(id + "-" + safeName); Files.write(original, bytes, StandardOpenOption.CREATE_NEW);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        List<DocumentRecord.EvidenceLine> ev = new ArrayList<>(); List<String> obs = new ArrayList<>(); String method; int pages=1;
        if (ext.equals("pdf")) {
            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                pages = pdf.getNumberOfPages(); StringBuilder all = new StringBuilder();
                for (int p=1;p<=pages;p++) { FontInspectionStripper stripper = new FontInspectionStripper(); stripper.setStartPage(p); stripper.setEndPage(p); String text=stripper.getText(pdf); all.append(text).append('\n'); addLines(text,p,ev,"PDF text layer"); for (String outlier : stripper.outlierLines()) obs.add("Potential visual style outlier at page "+p+", line "+outlier+". This is a visual observation only; formatting differences can be intentional."); }
                if (all.toString().replaceAll("\\s", "").length() > 30) method="PDF text layer";
                else { ev.clear(); int dpi=180; PDFRenderer renderer=new PDFRenderer(pdf); for(int p=0;p<pages;p++){ BufferedImage image=renderer.renderImageWithDPI(p,dpi); ev.addAll(ocr(image,p+1,"OCR (Tesseract)")); } method="OCR (Tesseract) on rendered PDF"; obs.add("No usable text layer was found; pages were rendered and sent to local Tesseract OCR."); }
            }
        } else { BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes)); if(image==null) throw new IllegalArgumentException("Could not decode image: " + safeName); ev.addAll(ocr(image,1,"OCR (Tesseract)")); method="OCR (Tesseract)"; }
        if (ev.isEmpty()) obs.add("No text was extracted. Check OCR language data and document image quality.");
        String combined=ev.stream().map(DocumentRecord.EvidenceLine::text).reduce((a,b)->a+"\n"+b).orElse("");
        Map<String,DocumentRecord.FieldValue> fields=extract(combined,ev);
        String type=classify(safeName,combined);
        if (ext.equals("pdf")) obs.add("PDF text positions are not rendered as overlays; the font check is a basic page-level style comparison only.");
        if (type.equals("Unknown / mixed")) obs.add("Document type was not confidently classified; reviewer should confirm the template.");
        return new DocumentRecord(id,safeName,type,"local upload",sha,"processed",pages,method,fields,ev,obs);
    }

    private static class FontInspectionStripper extends PDFTextStripper {
        private final List<Map.Entry<Integer,String>> lines = new ArrayList<>();
        private int lineNumber = 0;
        FontInspectionStripper() throws IOException { super(); }
        @Override protected void writeString(String text, List<TextPosition> positions) throws IOException {
            Map<String,Integer> counts = new HashMap<>();
            for (TextPosition position : positions) counts.merge(position.getFont().getName(), 1, Integer::sum);
            String dominant = counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
            if (text != null && !text.isBlank()) lines.add(Map.entry(++lineNumber, dominant));
            super.writeString(text, positions);
        }
        List<String> outlierLines() {
            Map<String,Integer> pageCounts = new HashMap<>();
            lines.forEach(line -> pageCounts.merge(line.getValue(), 1, Integer::sum));
            String baseline = pageCounts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
            return lines.stream().filter(line -> line.getKey() > 1 && !line.getValue().equals(baseline))
                    .map(line -> line.getKey()+" ("+line.getValue()+" vs predominant "+baseline+")").toList();
        }
    }

    private void addLines(String text,int page,List<DocumentRecord.EvidenceLine> out,String method){ int i=0; for(String s:text.split("\\R")){ String t=s.trim(); if(!t.isEmpty()) out.add(new DocumentRecord.EvidenceLine("E-"+UUID.randomUUID().toString().substring(0,8),page,++i,t,0,0,0,0,"page "+page+", line "+i,method)); } }
    private List<DocumentRecord.EvidenceLine> ocr(BufferedImage image,int page,String method) throws Exception {
        Path in=Files.createTempFile("folio-ocr-",".png"), out=Files.createTempFile("folio-ocr-",".tsv"), log=Files.createTempFile("folio-ocr-",".log");
        try { ImageIO.write(image,"png",in.toFile()); Process p=new ProcessBuilder(tesseract,in.toString(),out.toString().replaceFirst("\\.tsv$",""),"-l","eng","tsv").redirectErrorStream(true).redirectOutput(log.toFile()).start(); if(!p.waitFor(90,java.util.concurrent.TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("Tesseract timed out");} String msg=Files.readString(log,StandardCharsets.UTF_8); if(p.exitValue()!=0) throw new IOException("Tesseract failed: "+msg); String tsv=Files.readString(Path.of(out.toString().replaceFirst("\\.tsv$","" )+".tsv"));
            Map<Integer,List<String>> lines=new TreeMap<>(); Map<Integer,int[]> boxes=new TreeMap<>(); Map<Integer,Double> conf=new TreeMap<>();
            String[] rows=tsv.split("\\R"); for(int i=1;i<rows.length;i++){String[] c=rows[i].split("\\t",12);if(c.length<12||c[11].isBlank())continue; int level=Integer.parseInt(c[0]);if(level!=5)continue;int key=Integer.parseInt(c[4]);int x=Integer.parseInt(c[6]),y=Integer.parseInt(c[7]),w=Integer.parseInt(c[8]),h=Integer.parseInt(c[9]); lines.computeIfAbsent(key,k->new ArrayList<>()).add(c[11]); int[] b=boxes.computeIfAbsent(key,k->new int[]{x,y,x+w,y+h});b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],y);b[2]=Math.max(b[2],x+w);b[3]=Math.max(b[3],y+h);try{conf.put(key,Double.parseDouble(c[10]));}catch(Exception ignored){} }
            List<DocumentRecord.EvidenceLine> result=new ArrayList<>(); int line=0;for(var e:lines.entrySet()){String text=String.join(" ",e.getValue()).trim();if(text.isEmpty())continue;int[] b=boxes.get(e.getKey());String id="E-"+UUID.randomUUID().toString().substring(0,8);double score=Math.max(0,Math.min(1,conf.getOrDefault(e.getKey(),50d)/100));result.add(new DocumentRecord.EvidenceLine(id,page,++line,text,(double)b[0]/image.getWidth(),(double)b[1]/image.getHeight(),(double)(b[2]-b[0])/image.getWidth(),(double)(b[3]-b[1])/image.getHeight(),String.format(Locale.ROOT,"page %d, normalized bbox [%.3f, %.3f, %.3f, %.3f]",page,(double)b[0]/image.getWidth(),(double)b[1]/image.getHeight(),(double)(b[2]-b[0])/image.getWidth(),(double)(b[3]-b[1])/image.getHeight()),method+"; OCR confidence "+Math.round(score*100)+"%"));}
            return result;
        } finally { Files.deleteIfExists(in); Files.deleteIfExists(out); Files.deleteIfExists(log); Files.deleteIfExists(Path.of(out.toString().replaceFirst("\\.tsv$","")+".tsv")); }
    }
    private Map<String,DocumentRecord.FieldValue> extract(String text,List<DocumentRecord.EvidenceLine> ev){Map<String,DocumentRecord.FieldValue> out=new LinkedHashMap<>();for(var e:FIELDS.entrySet()){Matcher m=e.getValue().matcher(text);if(m.find()){String value=e.getKey().equals("signature")?m.group().trim():m.group(1).trim().replaceAll("\\s+"," ");String needle=value.toLowerCase(Locale.ROOT);List<String> refs=ev.stream().filter(x->x.text().toLowerCase(Locale.ROOT).contains(needle)||x.text().toLowerCase(Locale.ROOT).contains(e.getKey().replace('_',' '))).map(DocumentRecord.EvidenceLine::id).limit(3).toList();double confidence=ev.stream().filter(x->refs.contains(x.id())&&x.method().contains("OCR confidence")).mapToDouble(x->{Matcher cm=Pattern.compile("(\\d+)%").matcher(x.method());return cm.find()?Integer.parseInt(cm.group(1))/100d:0.75;}).max().orElse(0.94);out.put(e.getKey(),new DocumentRecord.FieldValue(value,confidence,refs));}}return out;}
    private String classify(String name,String text){String s=(name+" "+text).toLowerCase(Locale.ROOT);Matcher explicit=Pattern.compile("(?im)^document type\\s*[:#-]\\s*(.+)$").matcher(text);if(explicit.find()){String label=explicit.group(1).toLowerCase(Locale.ROOT);if(label.contains("identity")||label.contains("passport"))return "Identity document";if(label.contains("application"))return "Application form";if(label.contains("income"))return "Income proof";if(label.contains("bank"))return "Bank statement";if(label.contains("invoice"))return "Invoice";}if(s.contains("identity")||s.contains("passport")||s.contains("national id")||s.contains("aadhaar"))return "Identity document";if(s.contains("bank")||s.contains("account statement"))return "Bank statement";if(s.contains("application")||s.contains("applicant"))return "Application form";if(s.contains("income")||s.contains("salary"))return "Income proof";if(s.contains("invoice"))return "Invoice";return "Unknown / mixed";}
}
