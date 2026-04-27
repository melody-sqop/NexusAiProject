package com.cmt.yutumblike.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("blog")
@Schema(description = "博客文章实体")
public class Blog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "博客主键ID")
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "博客标题")
    private String title;

    @Schema(description = "封面图片地址")
    private String coverImg;

    @Schema(description = "文章正文内容")
    private String content;

    @Schema(description = "点赞数量")
    private Integer thumbCount;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}