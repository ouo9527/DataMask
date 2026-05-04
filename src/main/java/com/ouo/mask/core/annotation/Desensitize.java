package com.ouo.mask.core.annotation;

import java.lang.annotation.*;

/***********************************************************
 * 脱敏注解
 *  只针对于WEB接口启用或停用，以防止与第二或三方交易响应时脱敏数据
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Desensitize {
    //是否启用脱敏，只针对于WEB接口
    boolean enabled() default true;
}
