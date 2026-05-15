package com.cmt.NexusAi.modules.audit.L1.sensitive.model.entity;

import lombok.Data;

@Data
public class SensitiveHomophone {
    private Long id;
    private String variant;   // 变体字：根
    private String standard;  // 标准字：功
    private Integer enabled;
}