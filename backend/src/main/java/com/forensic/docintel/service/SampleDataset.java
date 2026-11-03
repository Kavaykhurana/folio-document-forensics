package com.forensic.docintel.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.*;

@Component
public class SampleDataset {
    public Path generate(Path directory) throws Exception {
        Files.createDirectories(directory);
        make(directory.resolve("01_application.pdf"),List.of("HACKTHECORE DEMONSTRATION — SYNTHETIC DATA","Document type: Application form","Application reference: DEMO-2026-041","Applicant name: Asha Mehta","Date of birth: 14 February 1992","National ID: DEMO 4821 6703","Address: 18 Sample Lane, Pune, Maharashtra","Declared income: INR 1,200,000 per annum","Document date: 02 November 2026","Signature: Asha Mehta","All identities and values on this page are fictional."));
        make(directory.resolve("02_identity.pdf"),List.of("HACKTHECORE DEMONSTRATION — SYNTHETIC DATA","Document type: Identity document","Applicant name: Asha Mehta","Date of birth: 14 February 1992","National ID: DEMO 4821 6703","Address: 18 Sample Lane, Pune, Maharashtra","Document date: 12 August 2024","All identities and values on this page are fictional."));
        make(directory.resolve("03_income_proof_altered.pdf"),List.of("HACKTHECORE DEMONSTRATION — SYNTHETIC DATA","Document type: Income proof","Applicant name: Asha Mehta","Account number: DEMO-AC-1938","Declared income: INR 2,100,000 per annum","Document date: 01 November 2026","This synthetic fixture is intentionally altered: the declared income is inconsistent and uses a different font style. It is not a real customer document."));
        return directory;
    }
    private void make(Path path,List<String> lines)throws Exception{Files.deleteIfExists(path);try(PDDocument d=new PDDocument()){PDPage page=new PDPage();d.addPage(page);try(PDPageContentStream c=new PDPageContentStream(d,page)){float y=750;for(int i=0;i<lines.size();i++){c.beginText();boolean styleOutlier=path.getFileName().toString().startsWith("03_")&&i==4; c.setFont(new PDType1Font(i==0||styleOutlier?Standard14Fonts.FontName.HELVETICA_BOLD:Standard14Fonts.FontName.HELVETICA),i==0?14:11);c.newLineAtOffset(56,y);c.showText(lines.get(i));c.endText();y-=30;}}d.save(path.toFile());}}
}
