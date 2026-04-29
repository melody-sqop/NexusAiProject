package com.cmt.yutumblike.model.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.util.Date;

/**
 * 前端视图层评论
 * 返回给前端显示
 */
@Data
public class CommentVO {
    /**
     * 评论id
     */
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 博客id
     */
    private Long blogId;
    /**
     * 父评论id
     */
    private Long parentId;
    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 评论审核状态 0-待审核 1-通过 2-驳回 3-人工复核
     */
    private Integer auditStatus;

    // 前端可以根据这个字段判断显示文案
    // 比如：审核中/已删除/或者正常显示
    /**
     * 审核描述 前端可以根据这个字段判断显示文案
     *        比如：审核中/已删除/或者正常显示
     */
    private String auditDesc;
}
