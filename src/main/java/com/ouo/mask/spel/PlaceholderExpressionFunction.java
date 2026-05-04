package com.ouo.mask.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;

/***********************************************************
 * 占位符表达式回调处理器
 *
 * Author:   ouo
 * Date:     2025/12/7
 ***********************************************************/
@FunctionalInterface
public interface PlaceholderExpressionFunction<R> {

    /**
     * 处理占位符表达式执行后的结果
     *
     * @param expression SpEL模板表达式
     * @param context    表达式执行上下文
     * @param result     表达式执行结果
     * @param e          表达式执行异常
     * @return 返回新的表达式结果
     */
    R process(PlaceholderExpression expression, EvaluationContext context, R result, EvaluationException e);
}
