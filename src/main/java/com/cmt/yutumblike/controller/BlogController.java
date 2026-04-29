package com.cmt.yutumblike.controller;

import com.cmt.yutumblike.common.BaseResponse;
import com.cmt.yutumblike.common.ResultUtils;
import com.cmt.yutumblike.model.dto.BlogCreateDTO;
import com.cmt.yutumblike.model.entity.Blog;
import com.cmt.yutumblike.model.vo.BlogVO;
import com.cmt.yutumblike.service.BlogService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private BlogService blogService;

    /**
     * 查询单个博客详情（含当前用户点赞状态）
     *
     * @param blogId 博客ID
     * @return 博客VO
     */
    @GetMapping("/get")
    public BaseResponse<BlogVO> get(@RequestParam long blogId) {
        return ResultUtils.success(blogService.getBlogVOById(blogId));
    }

    /**
     * 查询博客列表（含当前用户对所有博客的点赞状态）
     *
     * @return 博客VO列表
     */
    @GetMapping("/list")
    public BaseResponse<List<BlogVO>> list() {
        List<Blog> blogList = blogService.list();
        List<BlogVO> blogVOList = blogService.getBlogVOList(blogList);
        return ResultUtils.success(blogVOList);
    }

    /**
     * 创建博客
     * @param dto
     * @return
     */
    @PostMapping("/create")
    public BaseResponse<Blog> createBlog(@Valid @RequestBody BlogCreateDTO dto) {
        // 直接调用你现有的 BlogService，在里面加 createBlog 方法即可
        Blog blog = blogService.createBlog(dto);
        return ResultUtils.success(blog);
    }

//    @PostMapping("/add")
//    public BaseResponse<Long> add(@RequestBody Blog blog, HttpServletRequest request) {
//        Long blogId = blogService.addBlog(blog, request);
//        return ResultUtils.success(blogId);
//    }
}



