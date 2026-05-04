package com.ouo.mask.spel;

import lombok.Getter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.*;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/***********************************************************
 * 占位符表达式
 *
 * Author:   ouo
 * Date:     2025/12/7
 ***********************************************************/
@Getter
public class PlaceholderExpression implements Expression {
    // 解析器上下文
    private final ParserContext parserContext;
    // SPEL表达式
    private final Expression expression;
    // placeholder position index
    private final int placeholderIndex;

    /**
     * Construct an expression, only used by the parser.
     *
     * @param expression
     * @param placeholderIndex
     */
    public PlaceholderExpression(ParserContext parserContext, Expression expression, int placeholderIndex) {
        if (expression instanceof PlaceholderExpression) {
            throw new EvaluationException("the expression cannot be a PlaceholderExpression itself.");
        }
        this.parserContext = parserContext;
        this.expression = expression;
        this.placeholderIndex = placeholderIndex;
    }


    public String getExpressionString() {
        return this.expression.getExpressionString();
    }

    @Override
    public Object getValue() throws EvaluationException {
        return this.expression.getValue();
    }

    @Override
    public <T> T getValue(Class<T> desiredResultType) throws EvaluationException {
        return this.expression.getValue(desiredResultType);
    }

    @Override
    public Object getValue(Object rootObject) throws EvaluationException {
        return this.expression.getValue(rootObject);
    }

    @Override
    public <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException {
        return this.expression.getValue(rootObject, desiredResultType);
    }

    @Override
    public Object getValue(EvaluationContext context) throws EvaluationException {
        return this.expression.getValue(context);
    }

    @Override
    public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
        return this.expression.getValue(context, rootObject);
    }

    @Override
    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException {
        return this.expression.getValue(context, desiredResultType);
    }

    @Override
    public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) throws EvaluationException {
        return this.expression.getValue(context, rootObject, desiredResultType);
    }

    @Override
    public Class<?> getValueType() throws EvaluationException {
        return this.expression.getValueType();
    }

    @Override
    public Class<?> getValueType(Object rootObject) throws EvaluationException {
        return this.expression.getValueType(rootObject);
    }

    @Override
    public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
        return this.expression.getValueType(context);
    }

    @Override
    public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
        return this.expression.getValueType(context, rootObject);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
        return this.expression.getValueTypeDescriptor();
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
        return this.expression.getValueTypeDescriptor(rootObject);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
        return this.expression.getValueTypeDescriptor(context);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
        return this.expression.getValueTypeDescriptor(context, rootObject);
    }

    @Override
    public boolean isWritable(Object rootObject) throws EvaluationException {
        return this.expression.isWritable(rootObject);
    }

    @Override
    public boolean isWritable(EvaluationContext context) throws EvaluationException {
        return this.expression.isWritable(context);
    }

    @Override
    public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
        return this.expression.isWritable(context, rootObject);
    }

    @Override
    public void setValue(Object rootObject, Object value) throws EvaluationException {
        this.expression.setValue(rootObject, value);
    }

    @Override
    public void setValue(EvaluationContext context, Object value) throws EvaluationException {
        this.expression.setValue(context, value);
    }

    @Override
    public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
        this.expression.setValue(context, rootObject, value);
    }

    /**
     * 获取模板表达式值
     *
     * @param context    表达式执行上下文
     * @param resultType 期望结果类型
     * @param fn         表达式回调处理
     * @return 返回表达式执行结果
     */
    @Nullable
    public <R> R getValue(EvaluationContext context, @Nullable Class<R> resultType, @Nullable PlaceholderExpressionFunction<R> fn) {
        Assert.notNull(context, "EvaluationContext is required");
        R val = null;
        EvaluationException exception = null;
        try {
            val = expression.getValue(context, resultType);
        } catch (EvaluationException e) {
            if (null == fn) throw e;
            exception = e;
        }
        if (null != fn) val = fn.process(this, context, val, exception);

        return ExpressionUtils.convertTypedValue(context, new TypedValue(val), resultType);
    }
}
