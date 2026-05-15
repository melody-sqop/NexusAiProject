package com.cmt.NexusAi.modules.user.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("user_role")
public class UserRole implements Serializable {

    private Long uid;

    private Long rid;
}