package com.cmt.NexusAi.modules.audit.L2a.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmt.NexusAi.modules.audit.L2a.entity.ShadowLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShadowLogMapper extends BaseMapper<ShadowLog> {

    /**
     * 插入影子日志
     */
    int insert(ShadowLog record);

    /**
     * 统计漏放数（is_diff=1，AI判违规但SimHash预期安全）
     */
    @Select("SELECT COUNT(*) FROM shadow_log WHERE is_diff = 1 AND create_time >= #{startTime}")
    long countMiss(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计判断正确数（is_diff=0，AI判安全且SimHash预期安全）
     */
    @Select("SELECT COUNT(*) FROM shadow_log WHERE is_diff = 0 AND create_time >= #{startTime}")
    long countCorrect(@Param("startTime") LocalDateTime startTime)



    ;
}