package com.cmt.NexusAi.controller;

import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.common.ResultUtils;
import com.cmt.NexusAi.model.dto.CommentAddDTO;
import com.cmt.NexusAi.model.vo.CommentVO;
import com.cmt.NexusAi.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}