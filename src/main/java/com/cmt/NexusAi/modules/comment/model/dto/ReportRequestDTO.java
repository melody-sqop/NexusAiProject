package com.cmt.NexusAi.modules.comment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequestDTO {
    @NotNull(message = "举报原因不能为空")
    private Integer reason;  // 1-涉政 2-色情 3-广告 4-人身攻击 5-其他

    private String desc;     // 补充描述，可选
}