package com.forensic.docintel.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.forensic.docintel.model.Investigation;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class SampleBundleGenerator {
    public static void main(String[] args) throws Exception {
        Path root=Path.of("..").toAbsolutePath().normalize(); Path sampleDir=root.resolve("samples/generated");
        new SampleDataset().generate(sampleDir);
        DocumentAnalyzer analyzer=new DocumentAnalyzer(); set(analyzer,"tesseract","tesseract");
        Path data=root.resolve("backend/data"); Files.createDirectories(data);
        InvestigationService service=new InvestigationService(analyzer,JsonMapper.builder().addModule(new JavaTimeModule()).build()); set(service,"storage",data.toString());
        List<MultipartFile> files=new ArrayList<>();for(Path p:Files.list(sampleDir).sorted().toList())files.add(new FileUpload(p));
        Investigation inv=service.create(files);
        Path dossier=root.resolve("samples/dossiers/sample-investigation.md");Files.createDirectories(dossier.getParent());Files.writeString(dossier,inv.dossierMarkdown());
        Files.writeString(root.resolve("samples/dossiers/sample-investigation.json"),JsonMapper.builder().addModule(new JavaTimeModule()).build().writerWithDefaultPrettyPrinter().writeValueAsString(inv));
        System.out.println("Generated " + files.size() + " synthetic PDFs and dossier " + dossier);
        System.out.println(inv.summary());
    }
    private static void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private record FileUpload(Path path) implements MultipartFile {
        public String getName(){return "files";} public String getOriginalFilename(){return path.getFileName().toString();} public String getContentType(){return "application/pdf";} public boolean isEmpty(){return false;} public long getSize(){try{return Files.size(path);}catch(Exception e){return 0;}} public byte[] getBytes()throws IOException{return Files.readAllBytes(path);} public InputStream getInputStream()throws IOException{return Files.newInputStream(path);} public void transferTo(File file)throws IOException{Files.copy(path,file.toPath());}
    }
}
