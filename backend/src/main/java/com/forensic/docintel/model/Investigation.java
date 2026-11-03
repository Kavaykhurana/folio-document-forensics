package com.forensic.docintel.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Investigation(UUID id, Instant createdAt, String status, List<DocumentRecord> documents,
                            List<Finding> findings, String summary, String dossierMarkdown) {}
