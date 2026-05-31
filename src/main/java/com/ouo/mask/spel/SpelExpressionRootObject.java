package com.ouo.mask.spel;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

@Getter
@Builder(builderClassName = "Builder")
class SpelExpressionRootObject {
    // 目标对象（可以是代理对象）
    private final Object target;
    // 目标类Class（原始类Class）
    private final Class targetClass;
    // 被调用方法
    private final Method method;
    // 被调用方法实参
    private final Object[] args;

    /**
     * 获取目标类名称（包含包名）
     *
     * @return 返回目标类名称
     */
    public String getTargetName() {
        return this.targetClass.getName();
    }

    /**
     * 获取目标类名称（不包含包名）
     *
     * @return 返回目标类名称
     */
    public String getTargetSimpleName() {
        return this.targetClass.getSimpleName();
    }

    /**
     * 获取被调用方法名
     *
     * @return 返回方法名称
     */
    public String getMethodName() {
        return this.method.getName();
    }

    /**
     * 获取被调用方法参数名
     *
     * @return 返回方法参数名称
     */
    public String[] getParameterNames() {
        ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
        return discoverer.getParameterNames(this.method);
    }
}
