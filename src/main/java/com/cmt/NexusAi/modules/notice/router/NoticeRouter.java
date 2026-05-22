package com.cmt.NexusAi.modules.notice.router;

import com.cmt.NexusAi.modules.notice.dto.NotificationEvent;
import com.cmt.NexusAi.modules.notice.enums.NoticeRoute;
import com.cmt.NexusAi.modules.notice.enums.NoticeType;
import org.springframework.stereotype.Component;

/**
 * 通知路由器
 * 作用：根据通知事件类型，决定该事件走哪条处理链路。
 * 1. HIGH_DIRECT: 高优先级直达链路，直接入库（如评论、回复）。
 * 2. AGGREGATE: 聚合链路，先入Redis滑动窗口，定时落盘（如点赞）。
 * 3. 预留降级：当MQ积压时，可将AGGREGATE降级为不落盘纯攒Redis。
 */
@Component
public class NoticeRouter {

    /**
     * 计算事件的路由策略
     * @param event 通知事件
     * @return 路由枚举
     */
    public NoticeRoute route(NotificationEvent event) {
        if (event.getType() == NoticeType.LIKE_BLOG) {
            // TODO 未来可在此处增加降级判断逻辑，例如读取配置中心开关
            // if (isDegrade) return NoticeRoute.FANOUT;
            return NoticeRoute.AGGREGATE;
        }

        if (event.getType() == NoticeType.COMMENT_BLOG || event.getType() == NoticeType.REPLY_COMMENT) {
            return NoticeRoute.HIGH_DIRECT;
        }

        // 兜底策略：未知类型默认直达
        return NoticeRoute.HIGH_DIRECT;
    }
}