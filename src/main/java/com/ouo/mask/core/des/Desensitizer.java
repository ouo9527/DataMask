package com.ouo.mask.core.des;

import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;

/***********************************************************
 * 脱敏器（按类型脱敏处理）
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public interface Desensitizer<T> {
    /**
     * 获取脱敏执行器
     *
     * @return 返回脱敏执行器
     */
    DesensitizationExecutor getDesensitizationExecutor();

    /**
     * 脱敏转换
     *
     * @param data    原始数据（类型已由 supports 保证）
     * @param context 上下文（可选）
     * @return 脱敏后的值
     */
    T desensitize(T data, DesensitizationContext context);
}
