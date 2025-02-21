package com.ouo.mask.spel;

import lombok.Getter;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Method;
import java.util.Properties;

/***********************************************************
 * TODO:     spel表达式元数据
 * Author:   刘春
 * Date:     2024/11/28
 ***********************************************************/
@Getter
public class SpelExpressionMetaData {
    //@Accessors
    private Properties sys; //系统环境变量
    private Properties env; //应用配置
    private Object target; //目标对象
    private Class targetClass; //目标类
    private String targetCName; //目标类名称（包含包名）
    private String targetCSName; //目标类名称（不包含包名）
    private Method method; //被调用的方法
    private String methodName; //被调用的方法名
    private Object[] args; //被调用的方法实参
    private String[] parameterNames; //被调用的方法参数名

    public SpelExpressionMetaData(Object target, Method method, Properties env, Object[] args) {
        this.target = target;
        this.targetClass = null == target ? null : target.getClass();
        this.method = method;
        this.methodName = null == method ? "" : method.getName();
        this.env = env;
        this.sys = System.getProperties();
        this.args = args;

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

    public SpelExpressionMetaData(Object target, Method method, ConfigurableEnvironment env, Object[] args) {
        this(target, method, args);
        // 获取所有文件
        if (null != env) {
            this.env = new Properties();
            env.getPropertySources().stream().forEach(ps ->
                    this.env.putAll(((MapPropertySource) ps).getSource()));
        }
    }

    public SpelExpressionMetaData(Object target, Method method, Object[] args) {
        this(target, method, (Properties) null, args);
    }

    public SpelExpressionMetaData(Object[] args) {
        this(null, null, (Properties) null, args);
    }
}
