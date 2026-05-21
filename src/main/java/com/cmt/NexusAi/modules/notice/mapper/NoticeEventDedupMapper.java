package com.cmt.NexusAi.modules.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmt.NexusAi.modules.notice.entity.NoticeEventDedup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等去重 Mapper
 */
@Mapper
public interface NoticeEventDedupMapper extends BaseMapper<NoticeEventDedup> {
}