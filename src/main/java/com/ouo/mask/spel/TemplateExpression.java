package com.ouo.mask.spel;

import cn.hutool.core.util.ObjUtil;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.lang.Nullable;

/***********************************************************
 * 模板表达式
 *
 * Author:   ouo
 * Date:     2025/12/7
 ***********************************************************/
public class TemplateExpression {
    // SPEL表达式
    private Expression expression;
    // placeholder position index
    private int placeholderIndex;
    // 表达式执行上下文
    @Nullable
    private EvaluationContext context;

    /**
     * Construct an expression, only used by the parser.
     *
     * @param expression
     * @param placeholderIndex
     */
    public TemplateExpression(Expression expression, int placeholderIndex) {
        this(expression, placeholderIndex, null);
    }

    public TemplateExpression(Expression expression, int placeholderIndex, EvaluationContext context) {
        this.expression = expression;
        this.placeholderIndex = placeholderIndex;
        this.context = context;
    }

    public Expression getExpression() {
        return expression;
    }

    @Nullable
    public EvaluationContext getContext() {
        return context;
    }

    public void setContext(@Nullable EvaluationContext context) {
        this.context = context;
    }

    public int getPlaceholderIndex() {
        return placeholderIndex;
    }

    public String getExpressionString() {
        return this.expression.getExpressionString();
    }

    /**
     * 获取模板表达式值
     *
     * @param resultType 期望结果类型
     * @param callback   模板表达式回调处理
     * @param <R>        返回类型
     * @return 返回表达式执行结果
     */
    @Nullable
    public <R> R getValue(@Nullable Class<R> resultType, @Nullable TemplateExpressionCallback callback) {
        Object val = null;
        EvaluationException exception = null;
        try {
            if (null == context) val = expression.getValue();
            else val = expression.getValue(context);
        } catch (EvaluationException e) {
            exception = e;
            if (null == callback) throw e;
        }
        if (null != callback) val = callback.process(this, val, exception);
        if (String.class.equals(resultType)) { // CharSequence.class.isAssignableFrom(resultType)
            return null == val ? null : (R) ObjUtil.toString(val);
        }
        return ExpressionUtils.convertTypedValue(context, new TypedValue(val), resultType);
    }
}
