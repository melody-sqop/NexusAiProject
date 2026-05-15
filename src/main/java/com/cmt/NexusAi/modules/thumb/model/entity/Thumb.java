package com.cmt.NexusAi.modules.thumb.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("thumb")
@Schema(description = "点赞记录实体")
public class Thumb {

    @TableId(type = IdType.AUTO)
    @Schema(description = "点赞记录主键ID")
    private Long id;

    @Schema(description = "点赞用户ID")
    private Long userId;

    @Schema(description = "被点赞博客ID")
    private Long blogId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "点赞时间")
    private LocalDateTime createTime;
}