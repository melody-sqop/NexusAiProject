package com.cmt.NexusAi.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cmt.NexusAi.model.dto.BlogCreateDTO;
import com.cmt.NexusAi.model.entity.Blog;
import com.cmt.NexusAi.model.vo.BlogVO;

import java.util.List;

public interface BlogService extends IService<Blog> {

    BlogVO getBlogVOById(long blogId);
    List<BlogVO> getBlogVOList(List<Blog> blogList);
    Blog createBlog(BlogCreateDTO dto);
}
