package com.cmt.NexusAi.ai.mapper;

import com.cmt.NexusAi.ai.model.entity.SensitiveWord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {

    @Select("SELECT id, word, risk_level, category, enabled " +
            "FROM sensitive_word WHERE enabled = 1 ORDER BY id")
    List<SensitiveWord> selectAllEnabled();

    @Insert("INSERT INTO sensitive_word(word, risk_level, category) " +
            "VALUES(#{word}, #{riskLevel}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SensitiveWord word);
}