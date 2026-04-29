package com.cmt.yutumblike.controller;

import com.cmt.yutumblike.common.BaseResponse;
import com.cmt.yutumblike.common.ResultUtils;
import com.cmt.yutumblike.model.dto.CommentAddDTO;
import com.cmt.yutumblike.model.vo.CommentVO;
import com.cmt.yutumblike.service.CommentService;
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