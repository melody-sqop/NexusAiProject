package com.cmt.NexusAi.modules.audit.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AuditConfig {

    @Value("${audit.sampling.rate:30}")
    private int samplingRate;

    @Value("${audit.shadow.mode:true}")
    private boolean shadowMode;

    @Value("${audit.simhash.threshold:2}")
    private int threshold;
}