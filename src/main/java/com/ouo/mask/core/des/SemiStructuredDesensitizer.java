package com.ouo.mask.core.des;

import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.semi.SemiStructuredMapper;
import lombok.RequiredArgsConstructor;

/***********************************************************
 * 半结构化脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public abstract class SemiStructuredDesensitizer<T> implements Desensitizer<T> {
    // 脱敏执行器
    protected final DesensitizationExecutor executor;
    // json处理器
    protected final SemiStructuredMapper mapper;

    @Override
    public DesensitizationExecutor getDesensitizationExecutor() {
        return executor;
    }
}
