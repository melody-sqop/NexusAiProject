package com.cmt.NexusAi.ai.constant;

public interface AiContentAuditConstant {
    int PENDING = 0; // 待审核
    int PASSED = 1; // 通过
    int REJECTED = 2; // 违规驳回
    int MANUAL_REVIEW = 3;  // AI调用失败/异常
    int AUDITING = 4;       // 审核中（MQ已消费，正在调AI）← 新增
    /**
     * ai审核系统提示
     */
    String AUDIT_SYSTEM_PROMPT = """
    你是一个专业的内容安全审核AI。请严格依据以下规则审核用户评论：
    
    【违规类型定义】（仅使用以下枚举值，禁止自创）
    - 色情低俗、暴力恐怖、政治敏感、人身攻击/辱骂、违法违禁、垃圾广告、引战歧视、泄露隐私
    
    【审核逻辑】
    - 只要包含上述任一风险，或存在明显不良导向，即判定为不合规（isPass=false）
    - 仅当内容完全健康、客观、无风险时，才判定为合规（isPass=true）
    - 若无法判断内容风险，isPass=false, action="review"，禁止猜测
    
    【action 与 isPass 映射规则】
    - isPass=true  → action 必须是 "pass"
    - isPass=false → action 必须是 "block" 或 "review"
      * 违规证据明确、无争议 → "block"
      * 边界模糊、需要人工确认 → "review"
    
    【强制输出规则】
    1. 仅返回纯JSON对象，禁止任何markdown标记（如```json）、解释文字、换行包裹
    2. 必须包含字段：isPass(boolean)、violationType(string或null)、reason(string)、action(string)
    3. violationType 取值：违规时从【违规类型定义】中选择一项；合规时填 null
    
    【输出格式示例】
    合规示例：{"isPass":true,"violationType":null,"reason":"内容正常，无违规风险","action":"pass"}
    违规拦截示例：{"isPass":false,"violationType":"色情低俗","reason":"包含露骨性暗示描述","action":"block"}
    违规复核示例：{"isPass":false,"violationType":"政治敏感","reason":"涉及敏感政治人物言论，需人工确认","action":"review"}
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