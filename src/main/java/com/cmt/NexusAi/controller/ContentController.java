package com.cmt.NexusAi.controller;

import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.common.ResultUtils;
import com.cmt.NexusAi.model.dto.CommentAddDTO;
import com.cmt.NexusAi.model.dto.ReportRequestDTO;
import com.cmt.NexusAi.model.vo.CommentVO;
import com.cmt.NexusAi.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/comment")
@RestController
@RequiredArgsConstructor
public class ContentController {

    private final CommentService commentService;

    /**
     * 创建评论 并调用api 由ai审核是否合规
     * @param dto
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<CommentVO> add(@RequestBody @Valid CommentAddDTO dto) {
        CommentVO commentVO = commentService.addComment(dto);
        return ResultUtils.success(commentVO);
    }

    /**
     * 查询博客下的评论列表（只返回 audit_status = PASSED 的）
     */
    @GetMapping("/list/{blogId}")
    public BaseResponse<List<CommentVO>> list(@PathVariable Long blogId) {
        List<CommentVO> list = commentService.getCommentsByBlogId(blogId);
        return ResultUtils.success(list);
    }

    /**
     * 用户举报评论（L1.5 事后兜底）
     */
    @PostMapping("/{commentId}/report")
    public BaseResponse<Void> report(
            @PathVariable Long commentId,
            @RequestBody @Valid ReportRequestDTO request) {
        commentService.reportComment(commentId, request);
        return ResultUtils.success(null);
    }

}