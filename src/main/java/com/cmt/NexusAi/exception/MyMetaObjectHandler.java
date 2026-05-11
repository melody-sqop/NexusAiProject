package com.cmt.NexusAi.exception;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充配置
 * 作用：给 marked 的字段（createTime/updateTime）自动赋值
 */
@Component // 必须加！让Spring管理
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入数据时，自动填充的字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // mybatisPlus插入时自动填充当前时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        // mybatisPlus插入时自动填充当前时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }



    /**
     * 更新数据时
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}