package com.forensic.docintel.model;

import java.util.List;
import java.util.Map;

public record DocumentRecord(String id, String fileName, String documentType, String source,
                             String sha256, String processingStatus, int pageCount, String extractionMethod,
                             Map<String, FieldValue> fields, List<EvidenceLine> evidence, List<String> observations) {
    public record FieldValue(String value, double confidence, List<String> evidenceIds) {}
    public record EvidenceLine(String id, int page, int line, String text, double x, double y, double width,
                               double height, String sourceLocator, String method) {}
}
