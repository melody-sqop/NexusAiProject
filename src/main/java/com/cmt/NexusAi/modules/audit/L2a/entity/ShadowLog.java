package com.cmt.NexusAi.modules.audit.L2a.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shadow_log")
public class ShadowLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    @TableField("cached_level")
    private String cachedLevel;

    @TableField("ai_level")
    private String aiLevel;

    private Integer distance;

    @TableField("is_diff")
    private Integer isDiff;

    @TableField("create_time")
    private LocalDateTime createTime;
}