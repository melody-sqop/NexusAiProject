package com.cmt.NexusAi.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cmt.NexusAi.mapper.ManualAuditTaskMapper;
import com.cmt.NexusAi.model.entity.ManualAuditTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualAuditTaskService {

    private final ManualAuditTaskMapper taskMapper;

    /**
     * 保存人工审核任务（带重试）
     */
    public ManualAuditTask saveWithRetry(Long commentId, String content,String aiReason) {
        ManualAuditTask task = new ManualAuditTask();
        task.setCommentId(commentId);
        task.setContent(content);
        task.setAiReason(aiReason);  // ← 新增
        task.setAuditStatus(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        for (int i = 0; i < 3; i++) {
            try {
                taskMapper.insert(task);
                log.info("[人工审核任务] 写入成功，taskId={}, commentId={}", task.getId(), commentId);
                return task;
            } catch (Exception e) {
                log.error("[人工审核任务] 第{}次写入失败，commentId={}", i + 1, commentId, e);
                if (i == 2) {
                    log.error("[FATAL] 人工审核任务彻底丢失 commentId={} content={}", commentId, content);
                    throw new RuntimeException("人工审核任务无法保存，数据库不可用", e);
                }
                try {
                    Thread.sleep(1000L * (i + 1));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return task;
    }

    public List<ManualAuditTask> getPendingTasks(int limit) {
        return taskMapper.selectList(
                new QueryWrapper<ManualAuditTask>()
                        .eq("audit_status", 0)
                        .orderByAsc("create_time")
                        .last("LIMIT " + limit)
        );
    }

    public void completeTask(Long taskId, String result, String reason, Long auditorId) {
        ManualAuditTask update = new ManualAuditTask();
        update.setId(taskId);
        update.setAuditStatus(1);
        update.setAuditResult(result);
        update.setAuditReason(reason);
        update.setAuditorId(auditorId);
        update.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(update);
    }
}