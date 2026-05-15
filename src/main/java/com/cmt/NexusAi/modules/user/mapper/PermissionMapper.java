package com.cmt.NexusAi.modules.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmt.NexusAi.modules.security.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

}
