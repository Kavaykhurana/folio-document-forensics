package com.forensic.docintel.model;

import java.util.List;

public record Finding(String id, String category, String severity, String status, String title,
                      String explanation, double confidence, List<String> documentIds,
                      List<String> evidenceIds, String recommendedAction) {}
