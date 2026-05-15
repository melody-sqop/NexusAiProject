    package com.cmt.NexusAi.modules.blog.model.dto;
    import jakarta.validation.constraints.NotBlank;
    import lombok.Data;

    /**
     * 创建博客 前端参数DTO
     * 仅保留前端需要传递的字段
     */
    @Data
    public class BlogCreateDTO {

        /**
         * 文章标题（必传）
         */
        @NotBlank(message = "文章标题不能为空")
        private String title;

        /**
         * 封面图（可选）
         */
        private String coverImg;

        /**
         * 文章内容（必传）
         */
        @NotBlank(message = "文章内容不能为空")
        private String content;
    }