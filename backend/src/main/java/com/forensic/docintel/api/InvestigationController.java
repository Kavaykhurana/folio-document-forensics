package com.forensic.docintel.api;

import com.forensic.docintel.model.Investigation;
import com.forensic.docintel.service.InvestigationService;
import com.forensic.docintel.service.SampleDataset;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class InvestigationController {
    private final InvestigationService service; private final SampleDataset samples;
    public InvestigationController(InvestigationService service,SampleDataset samples){this.service=service;this.samples=samples;}
    @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","ok","service","folio-forensics","ocr","local Tesseract expected for scanned inputs");}
    @PostMapping(value="/investigations",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Investigation create(@RequestPart("files") List<MultipartFile> files)throws Exception{return service.create(files);}
    @PostMapping("/demo") public Investigation demo()throws Exception{Path dir=samples.generate(Path.of("../samples/generated").toAbsolutePath().normalize());List<MultipartFile> files=new ArrayList<>();for(Path p:Files.list(dir).sorted().toList())files.add(new BytesMultipartFile(p.getFileName().toString(),Files.readAllBytes(p)));return service.create(files);}
    @GetMapping("/investigations/{id}") public Investigation get(@PathVariable UUID id){return service.get(id);}
    @GetMapping("/investigations/{id}/dossier") public ResponseEntity<String> dossier(@PathVariable UUID id){return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/markdown; charset=utf-8")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=investigation-"+id+".md").body(service.get(id).dossierMarkdown());}
    @GetMapping("/investigations/{id}/documents/{docId}/source") public ResponseEntity<byte[]> source(@PathVariable UUID id,@PathVariable String docId)throws Exception{Path p=service.source(id,docId);String type=Files.probeContentType(p);return ResponseEntity.ok().contentType(MediaType.parseMediaType(type==null?"application/octet-stream":type)).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+p.getFileName()+"\"").body(Files.readAllBytes(p));}
    @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<Map<String,String>> notFound(Exception e){return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));}
    @ExceptionHandler({IllegalArgumentException.class,java.io.IOException.class}) public ResponseEntity<Map<String,String>> badRequest(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    private record BytesMultipartFile(String originalFilename,byte[] data) implements MultipartFile {
        public String getName(){return "files";} public String getOriginalFilename(){return originalFilename;} public String getContentType(){return "application/pdf";} public boolean isEmpty(){return data.length==0;} public long getSize(){return data.length;} public byte[] getBytes(){return data;} public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(data);} public void transferTo(java.io.File dest)throws java.io.IOException{java.nio.file.Files.write(dest.toPath(),data);}
    }
}
