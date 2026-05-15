package com.cmt.NexusAi.modules.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmt.NexusAi.modules.blog.model.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
    void batchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap);

}
