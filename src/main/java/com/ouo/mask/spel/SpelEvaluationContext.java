package com.ouo.mask.spel;

import cn.hutool.core.util.ArrayUtil;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.*;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/***********************************************************
 * spel表达式上下文
 *
 * Author:   ouo
 * Date:     2024/11/28
 ***********************************************************/
public class SpelEvaluationContext implements EvaluationContext {

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    /**
     * Indicate that there is no result variable.
     */
    private static final Object NO_RESULT = new Object();
    /**
     * Indicate that the result variable cannot be used at all.
     */
    private static final Object RESULT_UNAVAILABLE = new Object();
    /**
     * The name of the variable holding the result object.
     */
    private static final String RESULT_VARIABLE = "result";
    private final Set<String> unavailableVariables = new HashSet<>(1);

    //private static final String SYS_VARIABLE = "sys";

    //private static final String ENV_VARIABLE = "env";
    private final EvaluationContext evaluationContext;

    @lombok.Builder(builderClassName = "Builder")
    SpelEvaluationContext(Object target, Method method, Object[] args, Class<?> targetClass, @Nullable Object result) {
        this.evaluationContext = this.createEvaluationContext(target, method, args, targetClass, result);
    }

    /**
     * Add the specified variable name as unavailable for that context.
     * Any expression trying to access this variable should lead to an exception.
     * <p>This permits the validation of expressions that could potentially a
     * variable even when such variable isn't available yet. Any expression
     * trying to use that variable should therefore fail to evaluate.
     */
    public void addUnavailableVariable(String name) {
        this.unavailableVariables.add(name);
    }

    @Override
    public TypedValue getRootObject() {
        return evaluationContext.getRootObject();
    }

    @Override
    public List<PropertyAccessor> getPropertyAccessors() {
        return evaluationContext.getPropertyAccessors();
    }

    @Override
    public List<ConstructorResolver> getConstructorResolvers() {
        return evaluationContext.getConstructorResolvers();
    }

    @Override
    public List<MethodResolver> getMethodResolvers() {
        return evaluationContext.getMethodResolvers();
    }

    @Override
    public BeanResolver getBeanResolver() {
        return evaluationContext.getBeanResolver();
    }

    @Override
    public TypeLocator getTypeLocator() {
        return evaluationContext.getTypeLocator();
    }

    @Override
    public TypeConverter getTypeConverter() {
        return evaluationContext.getTypeConverter();
    }

    @Override
    public TypeComparator getTypeComparator() {
        return evaluationContext.getTypeComparator();
    }

    @Override
    public OperatorOverloader getOperatorOverloader() {
        return evaluationContext.getOperatorOverloader();
    }

    @Override
    public void setVariable(String name, Object value) {
        evaluationContext.setVariable(name, value);
    }

    /**
     * Load the param information only when needed.
     */
    @Override
    @Nullable
    public Object lookupVariable(String name) {
        if (this.unavailableVariables.contains(name)) {
            throw new EvaluationException(name + " not available");
        }
        return evaluationContext.lookupVariable(name);
    }

    /**
     * Create an {@link EvaluationContext}.
     *
     * @param target      the target object
     * @param method      the method
     * @param args        the method arguments
     * @param targetClass the target class
     * @param result      the return value (can be {@code null}) or
     *                    {@link #NO_RESULT} if there is no return at this time
     * @return the evaluation context
     */
    private EvaluationContext createEvaluationContext(Object target, Method method, Object[] args,
                                                      Class<?> targetClass, @Nullable Object result) {

        SpelExpressionRootObject rootObject = SpelExpressionRootObject.builder()
                .target(target)
                .method(method)
                .args(args)
                .targetClass(targetClass)
                .build();

        EvaluationContext evaluationContext = null;

        if (null == method) {
            evaluationContext = SimpleEvaluationContext
                    .forReadOnlyDataBinding()
                    .withRootObject(rootObject) // 设置根上下文对象
                    .build();

            if (result == RESULT_UNAVAILABLE) {
                this.addUnavailableVariable(RESULT_VARIABLE);
            } else if (result != NO_RESULT) {
                evaluationContext.setVariable(RESULT_VARIABLE, result);
            }
            // 设置上下文属性
            for (int i = 0; i < ArrayUtil.length(args); i++) {
                evaluationContext.setVariable("p" + i, args[i]);
                evaluationContext.setVariable("a" + i, args[i]);
            }
        } else {
            evaluationContext = new MethodBasedEvaluationContext(
                    rootObject, method, args, parameterNameDiscoverer);
            if (result == RESULT_UNAVAILABLE) {
                this.addUnavailableVariable(RESULT_VARIABLE);
            } else if (result != NO_RESULT) {
                evaluationContext.setVariable(RESULT_VARIABLE, result);
            }
        }

        return evaluationContext;
    }

    public static class Builder {
        private Object target; // 目标对象（可以是代理对象）
        private Class targetClass; // 目标类Class（原始类Class）
        private Method method; // 被调用方法
        private Object[] args; // 被调用方法实参
        private Object result; // 方法执行结果

        public Builder args(Object... args) {
            this.args = args;
            return this;
        }
    }
}
