package com.ouo.mask.spel;

import cn.hutool.core.collection.CollUtil;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.lang.Nullable;

import java.util.List;

/***********************************************************
 * 多个模板表达式处理
 * Author:   刘春
 * Date:     2025/12/7
 ***********************************************************/
class CompositeStringExpressionWrap extends CompositeStringExpression {

    private List<Object> expressions;

    public CompositeStringExpressionWrap(String templateExpression, List<Object> expressions) {
        super(templateExpression, new Expression[]{});
        this.expressions = expressions;

    }

    /**
     * 从多个模板表达式获取值
     *
     * @param context    表达式上下文
     * @param resultType 期望结果类型
     * @param callback   模板表达式回调处理
     * @param <R>        返回类型
     * @return 返回表达式执行结果
     */
    public <R> R getValue(EvaluationContext context, @Nullable Class<R> resultType, @Nullable TemplateExpressionCallback callback) {
        if (CollUtil.isEmpty(this.expressions)) return null;
        Object val = null;
        if (1 == this.expressions.size()) val = this.getValue(this.expressions.get(0), context, resultType, callback);
        else {
            StringBuilder sb = new StringBuilder();
            for (Object expression : this.expressions) {
                sb.append(this.getValue(expression, context, resultType, callback));
            }
            val = sb.toString();
        }
        return ExpressionUtils.convertTypedValue(context, new TypedValue(val), resultType);
    }

    private <R> R getValue(Object expression, EvaluationContext context, @Nullable Class<R> resultType, @Nullable TemplateExpressionCallback callback) {
        if (expression instanceof TemplateExpression) {
            ((TemplateExpression) expression).setContext(context);
            return ((TemplateExpression) expression).getValue(resultType, callback);
        } else if (expression instanceof Expression) {
            if (null == context) return ((Expression) expression).getValue(resultType);
            else return ((Expression) expression).getValue(context, resultType);
        }
        throw new ExpressionException("the expression is not of type Expression or TemplateExpression, nor of their subclasses.");
    }
}
