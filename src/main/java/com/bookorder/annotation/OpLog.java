package com.bookorder.annotation;

import java.lang.annotation.*;

/**
 * 标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {
    /** 模块名（book/borrow/user/category） */
    String module();
    /** 操作类型（CREATE/UPDATE/DELETE/RETURN/RENEW 等） */
    String operation();
}
