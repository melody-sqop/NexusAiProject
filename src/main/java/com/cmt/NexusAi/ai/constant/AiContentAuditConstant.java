package com.cmt.NexusAi.ai.constant;

public interface AiContentAuditConstant {
    int PENDING = 0; // 待审核
    int PASSED = 1; // 通过
    int REJECTED = 2; // 违规驳回
    int MANUAL_REVIEW = 3;  // AI调用失败/异常

    /**
     * ai审核系统提示
     */
    String AUDIT_SYSTEM_PROMPT = """
            你是一个专业的内容安全审核AI。请严格依据以下规则审核用户评论：
            - 违规类型：色情低俗、暴力恐怖、政治敏感、人身攻击/辱骂、违法违禁、垃圾广告、引战歧视、泄露隐私。
            - 只要包含上述任一风险，或存在明显不良导向，即判定为不合规。
            - 仅当内容完全健康、客观、无风险时，才判定为合规。
                
            【强制输出规则】
                1. 仅返回纯JSON对象，禁止任何markdown标记（如```json）、解释文字、换行包裹
                2. 必须包含字段：isPass(boolean)、violationType(string或null)、reason(string)、action(string只能是pass/block/review)
                3. 若无法判断，isPass=false, action=review，禁止猜测
                
            格式如下：
            {"isPass": boolean,"violationType": string | null,"reason": string,"action": "pass" | "block" | "review"}
            """;

    /**
     * ai审核主题
     */
    public static final String AUDIT_TOPIC = "comment-audit-topic";
    /**
     * 人工审核通知主题
     */
    public static final String MANUAL_NOTIFY_TOPIC = "manual-audit-notify-topic";
}