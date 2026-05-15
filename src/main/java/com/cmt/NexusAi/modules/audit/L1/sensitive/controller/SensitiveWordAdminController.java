package com.cmt.NexusAi.modules.audit.L1.sensitive.controller;

import com.cmt.NexusAi.modules.audit.L1.sensitive.content.ContentAuditor;
import com.cmt.NexusAi.modules.audit.L1.sensitive.mapper.SensitiveWordMapper;
import com.cmt.NexusAi.modules.audit.L1.sensitive.model.entity.SensitiveWord;
import com.cmt.NexusAi.common.BaseResponse;
import com.cmt.NexusAi.common.ResultUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/sensitive-word")
public class SensitiveWordAdminController {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private ContentAuditor contentAuditor;

    /**
     * //TODO 这里的refreshTrie是查询整个数据库 性能很低 如果添加的花会重新构成trie树 因为trie数只能重建不能新增
     * //TODO 如果要优化可以通过 异步数据库 但是有延迟  还有一种方法 异步+内存双写 审核的时候去内存再查一遍 然后异步慢慢入库
     * 添加敏感词（运营后台调用）
     * 添加后立即生效，无需重启服务
     */
    @PostMapping
    public BaseResponse<String> add(@RequestBody SensitiveWord word) {
        // 1. 入库
        sensitiveWordMapper.insert(word);

        // 2. 触发 trie 树重建（热更新）
        contentAuditor.refreshTrie();

        return ResultUtils.success("添加成功并已生效");
    }
}