package com.ouo.mask.spel;

import org.springframework.expression.EvaluationException;

/***********************************************************
 * 模板表达式回调处理器
 * Author:   刘春
 * Date:     2025/12/7
 ***********************************************************/
public interface TemplateExpressionCallback {

    /**
     * 处理模板表达式执行后的结果
     *
     * @param expression SpEL模板表达式
     * @param result     表达式执行结果
     * @param e          表达式执行异常
     * @return 返回新的表达式结果
     */
    Object process(TemplateExpression expression, Object result, EvaluationException e);
}
