package com.cmt.yutumblike.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("permission")
public class Permission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long pid;

    private String pname;

    private String code;
}