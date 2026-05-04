package com.ouo.mask.core.annotation;

import lombok.Getter;

import java.lang.annotation.*;

/***********************************************************
 * Hash脱敏注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Hash {
    //Hash算法
    AlgorithmEnum algorithm() default AlgorithmEnum.SM3;

    //盐
    String salt() default "";

    //Hash算法
    @Getter
    enum AlgorithmEnum {
        SM3(10),
        MD5(20),
        HASH256(30);

        private int code;

        AlgorithmEnum(int code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
