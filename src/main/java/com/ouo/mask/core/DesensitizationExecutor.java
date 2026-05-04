package com.ouo.mask.core;

/***********************************************************
 * 脱敏执行器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public interface DesensitizationExecutor {

    /**
     * 数据脱敏
     *
     * @param data 待脱敏的Java Bean、Map、XML、JSON、集合等任意类型数据
     * @return 返回已脱敏后数据
     */
    default <T> T desensitize(T data) {
        return this.desensitize(data, null);
    }

    /**
     * 数据脱敏
     *
     * @param data    待脱敏的Java Bean、Map、XML、JSON、集合等任意类型数据
     * @param context 脱敏上下
     * @return 返回已脱敏后数据
     */
    <T> T desensitize(T data, DesensitizationContext context);
}
