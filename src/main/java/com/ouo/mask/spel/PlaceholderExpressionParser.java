package com.ouo.mask.spel;

import org.springframework.expression.EvaluationContext;

@FunctionalInterface
public interface PlaceholderExpressionParser<T> {

    /**
     * 处理不符合spel规范的表达式，并返回表达式执行结果
     *
     * @param expression       解析后表达式
     * @param placeholderIndex 占位符所在模板位置索引
     * @param context          表达式上下文参数
     * @return
     */
    T handleExpression(String expression, int placeholderIndex, EvaluationContext context);
}
