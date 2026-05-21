package com.cmt.NexusAi.modules.audit.L2b.model.vo;

public record AuditResult(

        /**
         * true 表示合规，false 表示违规
         */
        Boolean isPass,

        /**
         * 主要违规类型（如 "人身攻击"），合规时填 null
         */
        String violationType,

        /**
         * 判定依据（简明客观，≤30字）
         */
        String reason,

        /**
         * 建议（如 "pass" | "block" | "review"）
         */
        String action,

        /**
         * 💥 新增：实际调用的物理模型名称 (如 qwen-turbo, qwen-max)
         * 此字段不参与大模型返回的 JSON 反序列化，由系统在调用后注入
         */
        String modelName
) {
    /**
     * 构建降级结果（超时或异常时使用）
     * 降级策略：设为 review，让系统放行并转为异步终审
     */
    public static AuditResult degrade(String reason) {
        // 降级时，模型名称标识为系统降级
        return new AuditResult(true, null, reason, "review", "SYSTEM_DEGRADE");
    }

    /**
     * 💥 新增：用于在解析出大模型结果后，注入系统元数据 (不可变对象的修改方式)
     * 因为 record 是不可变的，所以我们通过拷贝原有字段并替换 modelName 来生成新对象
     */
    public AuditResult withModelName(String modelName) {
        return new AuditResult(this.isPass, this.violationType, this.reason, this.action, modelName);
    }
}