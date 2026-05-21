package com.cmt.NexusAi.modules.notice.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知列表查询请求参数
 * 采用游标分页，彻底避免深分页慢查询问题
 */
@Data
public class NoticeQueryDTO {

    /**
     * 游标：上一次查询最后一条通知的创建时间
     * 第一次查询（首页）时不传或传空，后续下拉加载时传上一页最后一条记录的 createTime
     */
    private LocalDateTime lastTime;

    /**
     * 每页拉取数量
     * 默认 20 条，严禁前端随意放大此值
     */
    private Integer pageSize = 20;
}