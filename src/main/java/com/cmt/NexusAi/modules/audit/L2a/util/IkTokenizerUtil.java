package com.cmt.NexusAi.modules.audit.L2a.util;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class IkTokenizerUtil {

    public static List<String> segment(String text) {
        List<String> words = new ArrayList<>();
        try {
            IKSegmenter ik = new IKSegmenter(new StringReader(text), true);
            Lexeme lexeme;
            while ((lexeme = ik.next()) != null) {
                words.add(lexeme.getLexemeText());
            }
        } catch (Exception e) {
            throw new RuntimeException("IK分词失败: " + text, e);
        }
        return words;
    }
}