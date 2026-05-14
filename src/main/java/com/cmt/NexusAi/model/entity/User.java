package com.cmt.NexusAi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("user")
@Schema(description = "用户实体")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户主键ID",example = "1",hidden = true)
    private Long id;

    @Schema(description = "用户名",hidden = true)
    private String username;

    @Schema(description = "手机号",example = "12787876660")
    private String phone;

    @Schema(description = "邮箱",hidden = true)
    private String email;

    @Schema(description = "密码",example = "123456")
    private String password;

    @Schema(description = "注册时间")
    private LocalDateTime registerTime;  // 注册时间
}