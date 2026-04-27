package com.cmt.yutumblike.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("role_permission")
public class RolePermission implements Serializable {

    private Long rid;

    private Long pid;
}