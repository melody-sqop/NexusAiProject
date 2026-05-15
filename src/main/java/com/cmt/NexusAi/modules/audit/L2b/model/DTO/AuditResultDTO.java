package com.cmt.NexusAi.modules.audit.L2b.model.DTO;

import lombok.Data;

@Data
public class AuditResultDTO {
    private int level;
    private String reason;
    private double confidence; // AI置信度 0.0~1.0
}