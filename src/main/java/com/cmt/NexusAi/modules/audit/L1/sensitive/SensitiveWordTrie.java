package com.cmt.NexusAi.modules.audit.L1.sensitive;

import com.cmt.NexusAi.modules.audit.L1.sensitive.model.entity.SensitiveWord;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SensitiveWordTrie {

    private volatile Trie trie;
    private volatile Map<String, SensitiveWord> wordMap = new HashMap<>();

    /**
     * 构建/重建 trie 树
     * 支持热更新：运营加词后重新调用
     */
    public void build(List<SensitiveWord> words) {
        Trie.TrieBuilder builder = Trie.builder();
        Map<String, SensitiveWord> newMap = new HashMap<>();

        for (SensitiveWord word : words) {
            String normalized = word.getWord().trim();
            builder.addKeyword(normalized);
            newMap.put(normalized, word);
        }

        this.trie = builder.build();
        this.wordMap = newMap;
    }

    public Trie getTrie() {
        return trie;
    }

    public SensitiveWord getMeta(String keyword) {
        return wordMap.get(keyword);
    }
}
