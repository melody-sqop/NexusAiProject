package com.cmt.NexusAi.modules.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmt.NexusAi.modules.notice.dto.NoticeQueryDTO;
import com.cmt.NexusAi.modules.notice.entity.Notice;
import com.cmt.NexusAi.modules.notice.mapper.NoticeMapper;
import com.cmt.NexusAi.modules.notice.service.NoticeService;
import com.cmt.NexusAi.modules.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {



    @Override
    public List<Notice> queryList(NoticeQueryDTO dto) {
        // 模拟获取当前用户ID，实际项目中使用 SecurityUtil.getCurrentUserId()
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 限制 pageSize 最大为 20，防止前端恶意传参导致慢查询
        int pageSize = dto.getPageSize() == null || dto.getPageSize() > 20 ? 20 : dto.getPageSize();

        // 使用 MyBatis-Plus 的 LambdaQuery 构造游标分页查询
        return this.lambdaQuery()
                .eq(Notice::getRecipientId, currentUserId) // 只查当前用户的通知
                .lt(dto.getLastTime() != null, Notice::getCreateTime, dto.getLastTime()) // 游标：查早于 lastTime 的记录
                .orderByDesc(Notice::getCreateTime) // 按时间倒序，最新的在前面
                .last("LIMIT " + pageSize) // 物理限制条数，浅分页
                .list();
    }

    @Override
    public void readOne(Long noticeId) {
        // 模拟获取当前用户ID
        Long currentUserId = 1001L;

        // 使用 LambdaUpdateWrapper 精确更新，确保只能已读自己的通知
        LambdaUpdateWrapper<Notice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notice::getId, noticeId) // 指定通知ID
                .eq(Notice::getRecipientId, currentUserId) // 安全校验：必须是我的通知
                .eq(Notice::getReadStatus, 0) // 性能优化：如果已经是已读状态就不执行更新
                .set(Notice::getReadStatus, 1); // 设为已读

        this.update(updateWrapper);
    }

    @Override
    public void readAll() {
        // 模拟获取当前用户ID
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 直接一条 SQL 将该用户所有未读通知改为已读
        // 由于架构上有"滑动窗口聚合"防轰炸，未读数不会离谱，一条 SQL 完全扛得住
        LambdaUpdateWrapper<Notice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notice::getRecipientId, currentUserId) // 指定当前用户
                .eq(Notice::getReadStatus, 0) // 只更新未读的
                .set(Notice::getReadStatus, 1); // 设为已读

        this.update(updateWrapper);
    }
}