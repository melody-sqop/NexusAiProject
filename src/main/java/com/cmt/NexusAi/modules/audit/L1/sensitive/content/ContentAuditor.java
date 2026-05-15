package com.cmt.NexusAi.modules.audit.L1.sensitive.content;

import com.cmt.NexusAi.modules.audit.L1.sensitive.SensitiveWordTrie;
import com.cmt.NexusAi.modules.audit.L1.sensitive.common.HitResult;
import com.cmt.NexusAi.modules.audit.L1.sensitive.until.TextNormalizerUtil;
import com.cmt.NexusAi.modules.audit.L1.sensitive.mapper.SensitiveWordMapper;
import com.cmt.NexusAi.modules.audit.L1.sensitive.model.entity.SensitiveWord;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ContentAuditor {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SensitiveWordTrie trieHolder;

    @Autowired
    private TextNormalizerUtil textNormalizer;

    /**
     * 系统启动时：从数据库加载敏感词，构建 trie 树
     */
    @PostConstruct
    public void init() {
        refreshTrie();
    }

    /**
     * 热更新：从数据库重新加载，重建 trie 树
     * 运营后台添加新词后调用此方法
     */
    public void refreshTrie() {

        long start = System.currentTimeMillis();

        List<SensitiveWord> words = sensitiveWordMapper.selectAllEnabled();
        trieHolder.build(words);
        System.out.println("[ContentAuditor] trie 树重建完成，敏感词数量：" + words.size());


        long cost = System.currentTimeMillis() - start;
        // 构建成功日志
        log.info("[SENSITIVE-TRIE] trie树重建完成 | 词库数量={} | 构建耗时={}ms | 线程={}",
                words.size(), cost, Thread.currentThread().getName());

        // 抽样打印前5个词，确认数据正确性
        words.stream().limit(5).forEach(w ->
                log.debug("[SENSITIVE-TRIE] 抽样词库 | word={} | riskLevel={} | category={}",
                        w.getWord(), w.getRiskLevel(), w.getCategory())
        );

    }

    /**
     * 审核入口
     *
     * @param text 用户评论原文
     * @return 命中结果列表，空表示未命中
     */
    public List<HitResult> match(String text) {
        if (trieHolder.getTrie() == null) {
            throw new RuntimeException("trie 树未初始化，请先调用 refreshTrie()");
        }

        long start = System.currentTimeMillis();


        // Step 1: 标准化（去符号、谐音替换）
        String normalized = textNormalizer.removeNoise(text);

        // Step 2: AC 自动机匹配（库提供，O(n)）
        Collection<Emit> emits = trieHolder.getTrie().parseText(normalized);

        // Step 3: 封装结果为HitResult集合返回 方便后面进行多敏感词进行比较取最高防范等级
        List<HitResult> results = new ArrayList<>();
        for (Emit emit : emits) {
            SensitiveWord meta = trieHolder.getMeta(emit.getKeyword());


            if (meta == null) {
                log.warn("[SENSITIVE-MATCH] 命中词在元数据中不存在 | keyword={}", emit.getKeyword());
                continue;
            }


            if (meta == null) continue;  // 防御性编程

            HitResult hit = new HitResult();
            hit.setStart(emit.getStart());
            hit.setEnd(emit.getEnd());
            hit.setMatchedWord(meta.getWord());
            hit.setRiskLevel(meta.getRiskLevel());
            hit.setCategory(meta.getCategory());
            results.add(hit);

            log.info("[SENSITIVE-MATCH] 命中敏感词 | word={} | riskLevel={} | category={} | pos=[{},{}]",
                    meta.getWord(), meta.getRiskLevel(), meta.getCategory(), emit.getStart(), emit.getEnd());

        }

        long cost = System.currentTimeMillis() - start;

        if (results.isEmpty()) {
            log.debug("[SENSITIVE-MATCH] 未命中 | textLen={} | cost={}ms", text.length(), cost);
        } else {
            log.info("[SENSITIVE-MATCH] 匹配完成 | 命中数量={} | textLen={} | cost={}ms | riskLevels={}",
                    results.size(), text.length(), cost,
                    results.stream().map(HitResult::getRiskLevel).collect(Collectors.toList()));
        }


        return results;
    }
}