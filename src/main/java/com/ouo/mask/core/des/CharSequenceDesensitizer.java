package com.ouo.mask.core.des;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/***********************************************************
 * CharSequence字符类型脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
@RequiredArgsConstructor
public class CharSequenceDesensitizer implements Desensitizer<CharSequence> {
    // 脱敏执行器
    private final DesensitizationExecutor executor;

    @Override
    public DesensitizationExecutor getDesensitizationExecutor() {
        return executor;
    }

    @Override
    public CharSequence desensitize(CharSequence data, DesensitizationContext context) {
        // 判断是否时半结构化数据
        // CharSequence的具体类型是否具备String类型参数的构造方法，若具备则可重新创建原对象
        Constructor<?> constructor = ObjUtil.defaultIfNull(ReflectUtil.getConstructor(data.getClass(), String.class)
                , ReflectUtil.getConstructor(data.getClass(), String[].class));
        if (null != constructor) {
            try {
                constructor.setAccessible(true);
                CharSequence val = executor.desensitize(Convert.convert(String.class, data), context);

                return (CharSequence) constructor.newInstance(ArrayUtil.firstNonNull(constructor.getParameterTypes())
                        .isArray() ? new CharSequence[]{val} : val);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                // String转换异常
                log.warn("String转【{}】异常：{}", constructor.getName(), e.getMessage());
            }
        }

        return data;
    }
}
