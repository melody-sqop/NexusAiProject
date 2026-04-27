package com.cmt.yutumblike.controller;

import com.cmt.yutumblike.common.BaseResponse;
import com.cmt.yutumblike.common.ResultUtils;
import com.cmt.yutumblike.model.entity.Blog;
import com.cmt.yutumblike.model.vo.BlogVO;
import com.cmt.yutumblike.service.BlogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("blog")
public class BlogController {

    @Resource
    private BlogService blogService;

    /**
     * 查询当前登录用户是否对某个博客点过赞
     *
     * @param blogId
     * @param request
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<BlogVO> get(long blogId, HttpServletRequest request) {
        BlogVO blogVO = blogService.getBlogVOById(blogId, request);
        return ResultUtils.success(blogVO);
    }

    /**
     * 查询当前登录用户对数据库中所有博客的点赞情况
     *
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<BlogVO>> list(HttpServletRequest request) {
        List<Blog> blogList = blogService.list();
        List<BlogVO> blogVOList = blogService.getBlogVOList(blogList, request);
        return ResultUtils.success(blogVOList);
    }


//    @PostMapping("/add")
//    public BaseResponse<Long> add(@RequestBody Blog blog, HttpServletRequest request) {
//        Long blogId = blogService.addBlog(blog, request);
//        return ResultUtils.success(blogId);
//    }
}



