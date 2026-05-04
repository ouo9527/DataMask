package com.ouo.mask.core.annotation;

import java.lang.annotation.*;

/***********************************************************
 * 置空脱敏注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Empty {
}
