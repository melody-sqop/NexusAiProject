package com.cmt.NexusAi.modules.audit.L2a.util;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class IkTokenizerUtil {

    /**
     * IK智能分词
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return tokens;
        }

        try (StringReader reader = new StringReader(text)) {
            IKSegmenter ik = new IKSegmenter(reader, true); // true=智能分词模式
            Lexeme lexeme;
            while ((lexeme = ik.next()) != null) {
                tokens.add(lexeme.getLexemeText());
            }
        } catch (Exception e) {
            // 降级：按字切分
            for (char c : text.toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    tokens.add(String.valueOf(c));
                }
            }
        }

        return tokens;
    }
}