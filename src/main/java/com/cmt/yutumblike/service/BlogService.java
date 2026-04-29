package com.cmt.yutumblike.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.yutumblike.model.dto.BlogCreateDTO;
import com.cmt.yutumblike.model.entity.Blog;
import com.cmt.yutumblike.model.vo.BlogVO;

import java.util.List;

public interface BlogService extends IService<Blog> {

    BlogVO getBlogVOById(long blogId);
    List<BlogVO> getBlogVOList(List<Blog> blogList);
    Blog createBlog(BlogCreateDTO dto);
}
