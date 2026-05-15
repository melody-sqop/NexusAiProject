package com.cmt.NexusAi.modules.audit.L2a.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmt.NexusAi.modules.audit.L2a.entity.ShadowLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ShadowLogMapper extends BaseMapper<ShadowLog> {

    @Select("SELECT distance, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN is_diff = 1 THEN 1 ELSE 0 END) as mismatch " +
            "FROM shadow_log " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY distance " +
            "ORDER BY distance")
    List<Map<String, Object>> selectMismatchStats();

}