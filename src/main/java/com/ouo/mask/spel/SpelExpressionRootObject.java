package com.ouo.mask.spel;

import lombok.Builder;
import lombok.Getter;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

@Getter
@Builder(builderClassName = "Builder")
class SpelExpressionRootObject {
    private final Object target; // 目标对象（可以是代理对象）
    private final Class targetClass; // 目标类Class（原始类Class）
    private final Method method; // 被调用方法
    private final Object[] args; // 被调用方法实参

    /**
     * 获取目标类名称（包含包名）
     *
     * @return
     */
    public String getTargetName() {
        return this.targetClass.getName();
    }

    /**
     * 获取目标类名称（不包含包名）
     *
     * @return
     */
    public String getTargetSimpleName() {
        return this.targetClass.getSimpleName();
    }

    /**
     * 获取被调用方法名
     *
     * @return
     */
    public String getMethodName() {
        return this.method.getName();
    }

    /**
     * 获取被调用方法参数名
     *
     * @return
     */
    public String[] getParameterNames() {
        ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
        return discoverer.getParameterNames(this.method);
    }
}
