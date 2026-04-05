package com.ouo.mask.spel;

import lombok.Getter;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Method;
import java.util.Properties;

/***********************************************************
 * spel表达式元数据
 *
 * Author:   ouo
 * Date:     2024/11/28
 ***********************************************************/
@Getter
public class SpelExpressionMetaData {
    //@Accessors
    private final Properties sys; // 系统环境变量
    private Properties env; // 应用配置
    private final Object target; // 目标对象
    private final Class targetClass; // 目标类
    private String targetCName; // 目标类名称（包含包名）
    private String targetCSName; // 目标类名称（不包含包名）
    private final Method method; // 被调用方法
    private final String methodName; // 被调用方法名
    private final Object[] args; // 被调用方法实参
    private final Object result; // 被调用方法返回值
    private String[] parameterNames; // 被调用方法参数名

    /**
     *
     * @param target    目标对象
     * @param method    被调用方法
     * @param args      被调用方法实参
     * @param result    被调用方法返回值
     */
    public SpelExpressionMetaData(Object target, Method method, Object[] args, Object result) {
        this.target = target;
        this.targetClass = null == target ? null : target.getClass();
        this.method = method;
        this.methodName = null == method ? "" : method.getName();
        this.sys = System.getProperties();
        this.args = args;
        this.result = result;

        // 获取类信息
        if (null != targetClass) {
            targetCName = targetClass.getName();
            targetCSName = targetClass.getSimpleName();
        }

        // 获取方法参数名称
        if (null != method) {
            ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
            this.parameterNames = discoverer.getParameterNames(method);
        }
    }

    /**
     * @param target 目标对象
     * @param method 被调用方法
     * @param env    应用配置
     * @param args   被调用方法实参
     * @param result 被调用方法返回值
     */
    public SpelExpressionMetaData(Object target, Method method, Properties env, Object[] args, Object result) {
        this(target, method, args, result);
        this.env = env;
    }

    /**
     * @param target 目标对象
     * @param method 被调用方法
     * @param env    应用配置
     * @param args   被调用方法实参
     */
    public SpelExpressionMetaData(Object target, Method method, Properties env, Object[] args) {
        this(target, method, env, args, null);
    }

    /**
     * @param target 目标对象
     * @param method 被调用方法
     * @param env    应用配置
     * @param args   被调用方法实参
     * @param result 被调用方法返回值
     */
    public SpelExpressionMetaData(Object target, Method method, ConfigurableEnvironment env
            , Object[] args, Object result) {
        this(target, method, args, result);
        // 获取所有文件
        if (null != env) {
            this.env = new Properties();
            env.getPropertySources().stream().forEach(ps ->
                    this.env.putAll(((MapPropertySource) ps).getSource()));
        }
    }

    /**
     *
     * @param target    目标对象
     * @param method    被调用方法
     * @param env       应用配置
     * @param args      被调用方法实参
     */
    public SpelExpressionMetaData(Object target, Method method, ConfigurableEnvironment env, Object[] args) {
        this(target, method, env, args, null);
    }

    /**
     *
     * @param target    目标对象
     * @param method    被调用方法
     * @param args      被调用方法实参
     */
    public SpelExpressionMetaData(Object target, Method method, Object[] args) {
        this(target, method, args, null);
    }

    /**
     *
     * @param args 被调用方法实参
     */
    public SpelExpressionMetaData(Object[] args) {
        this(null, null, args);
    }

    /**
     *
     * @param arg 被调用方法实参
     */
    public SpelExpressionMetaData(Object arg) {
        this(new Object[]{ arg });
    }
}
