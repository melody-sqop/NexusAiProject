package com.cmt.NexusAi.modules.notice.controller;

import com.cmt.NexusAi.modules.notice.dto.NoticeQueryDTO;
import com.cmt.NexusAi.modules.notice.entity.Notice;
import com.cmt.NexusAi.modules.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知中心 Controller
 * 提供通知列表查询、已读标记等基础 API
 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 查询通知列表（游标分页下拉加载）
     * 前端首次请求不传 lastTime，后续请求传入上一页最后一条记录的 createTime
     */
    @GetMapping("/list")
    public List<Notice> queryList(NoticeQueryDTO dto) {
        // 实际项目中返回统一的 Result 包装类，这里为了演示清晰直接返回 List
        return noticeService.queryList(dto);
    }

    /**
     * 单条通知标记已读
     * @param noticeId 通知ID
     */
    @PostMapping("/read/{noticeId}")
    public void readOne(@PathVariable Long noticeId) {
        noticeService.readOne(noticeId);
    }

    /**
     * 全部通知标记已读
     */
    @PostMapping("/readAll")
    public void readAll() {
        noticeService.readAll();
    }
}