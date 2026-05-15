package com.cmt.NexusAi.modules.blog.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.modules.blog.model.dto.BlogCreateDTO;
import com.cmt.NexusAi.modules.blog.model.entity.Blog;
import com.cmt.NexusAi.modules.blog.model.vo.BlogVO;

import java.util.List;

public interface BlogService extends IService<Blog> {

    BlogVO getBlogVOById(long blogId);
    List<BlogVO> getBlogVOList(List<Blog> blogList);
    Blog createBlog(BlogCreateDTO dto);
}
