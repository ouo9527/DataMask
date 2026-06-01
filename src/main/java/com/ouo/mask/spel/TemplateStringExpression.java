package com.ouo.mask.spel;

import cn.hutool.core.util.ArrayUtil;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.lang.Nullable;

/***********************************************************
 * 模板表达式处理
 *
 * Author:   ouo
 * Date:     2025/12/7
 ***********************************************************/
class TemplateStringExpression extends CompositeStringExpression {

    public TemplateStringExpression(String templateExpression, Expression[] expressions) {
        super(templateExpression, expressions);
    }

    /**
     * 从多个模板表达式获取值
     *
     * @param context 表达式上下文
     * @param fn 表达式回调处理
     * @return 返回表达式执行结果
     */
    public <R> R getValue(EvaluationContext context, Class<R> resultType,
                          @Nullable PlaceholderExpressionFunction<R> fn) {
        Expression[] expressions = this.getExpressions();
        if (ArrayUtil.isEmpty(expressions)) return null;

        StringBuilder sb = new StringBuilder();
        for (Expression expression : expressions) {
            if (null == expression) continue;
            Object val = (expression instanceof PlaceholderExpression)
                    ? ((PlaceholderExpression) expression).getValue(context, resultType, fn)
                    : expression.getValue(context, resultType);
            if (null != val) sb.append(val);
        }
        return ExpressionUtils.convertTypedValue(context, new TypedValue(sb), resultType);
    }
}
