package com.cmt.NexusAi.modules.audit.L1.sensitive.mapper;
import com.cmt.NexusAi.modules.audit.L1.sensitive.model.dto.SensitiveHomophoneDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SensitiveHomophoneMapper {

    @Select("SELECT variant, standard FROM sensitive_homophone WHERE enabled = 1")
    List<SensitiveHomophoneDTO> selectAllEnabled();
}
