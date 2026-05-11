package com.cmt.NexusAi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("role")
public class Role implements Serializable {

    /**
     * 角色id
     */
    @TableId(type = IdType.AUTO)
    private Long rid;

    /**
     * 角色名称
     */
    private String rname;

    /**
     * 角色编码：USER / CREATOR / ADMIN / AUDITOR
     */
    private String rcode;
}