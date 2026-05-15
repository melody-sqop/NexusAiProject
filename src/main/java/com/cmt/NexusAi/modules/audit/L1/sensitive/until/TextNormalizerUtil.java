package com.cmt.NexusAi.modules.audit.L1.sensitive.until;

import com.cmt.NexusAi.modules.audit.L1.sensitive.mapper.SensitiveHomophoneMapper;
import com.cmt.NexusAi.modules.audit.L1.sensitive.model.dto.SensitiveHomophoneDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TextNormalizerUtil {

    // 干扰符号：用户加这些来绕过
    private static final String NOISE_CHARS = "*#@$%^&·•◆◇▪\\s";

    // 谐音映射：从数据库加载，启动后填充
    private Map<String, String> homophoneMap = new HashMap<>();

    private final SensitiveHomophoneMapper homophoneMapper;

    /**
     * 系统启动时加载谐音映射
     */
    @PostConstruct
    public void init() {
        List<SensitiveHomophoneDTO> list = homophoneMapper.selectAllEnabled();
        for (SensitiveHomophoneDTO h : list) {
            homophoneMap.put(h.getVariant(), h.getStandard());
        }
    }

    /**
     * 标准化入口
     */
    public String removeNoise(String text) {
        if (text == null) return "";


        // 第1步：大小写（处理 Fa/fA 等英文变体）
        text = text.toLowerCase();

        // 第2步：谐音映射
        text = mapHomophone(text);

        // 第3步：去干扰符号
        return text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
    }

    private String mapHomophone(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            String replacement = homophoneMap.get(String.valueOf(c));
            sb.append(replacement != null ? replacement : c);
        }
        return sb.toString();
    }
}