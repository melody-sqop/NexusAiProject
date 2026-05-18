package com.cmt.NexusAi.modules.audit.L2a.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimHashResult {

    private Long simHash;
    private List<String> segmentKeys;

    /** 违规标签（替代原 riskLevel 数字）：P0_BLACKLIST / P1_CONTACT / AI_REJECT */
    private String violationTag;

    private String auditSource;
    private String auditResult;
    private String auditComment;
    private String sampleText;
    private String createTime;

    @JsonIgnore
    private Integer matchDistance;

    @JsonIgnore
    public boolean isValid() {
        if (createTime == null || simHash == null) return false;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            LocalDateTime cachedTime = LocalDateTime.parse(createTime, formatter);
            return LocalDateTime.now().minusHours(24).isBefore(cachedTime);
        } catch (Exception e) {
            return false;
        }
    }
}