package com.ouo.mask.spel;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

public abstract class ExpressionWrap implements Expression {
    // placeholder position index
    protected int placeholderIndex;
    private Expression expression;

    /**
     * Construct an expression, only used by the parser.
     *
     * @param placeholderIndex
     * @param expression
     */
    public ExpressionWrap(int placeholderIndex, Expression expression) {
        this.placeholderIndex = placeholderIndex;
        this.expression = expression;
    }

    @Nullable
    public abstract Object getValue(@Nullable PlaceholderExpressionParser expressionParser);

    @Nullable
    public abstract Object getValue(@Nullable Object rootObject, @Nullable PlaceholderExpressionParser expressionParser);

    @Nullable
    public abstract Object getValue(EvaluationContext context, @Nullable PlaceholderExpressionParser expressionParser);

    @Nullable
    public abstract Object getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable PlaceholderExpressionParser expressionParser);


    @Nullable
    public abstract <T> T getValue(@Nullable Class<T> expectedResultType, @Nullable PlaceholderExpressionParser<T> expressionParser);

    @Nullable
    public abstract <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> expectedResultType, @Nullable PlaceholderExpressionParser<T> expressionParser);

    @Nullable
    public abstract <T> T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType, @Nullable PlaceholderExpressionParser<T> expressionParser);

    @Nullable
    public abstract <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> expectedResultType, @Nullable PlaceholderExpressionParser<T> expressionParser);

    @Override
    public String getExpressionString() {
        return expression.getExpressionString();
    }

    @Override
    public Object getValue() throws EvaluationException {
        return expression.getValue();
    }

    @Override
    public <T> T getValue(Class<T> desiredResultType) throws EvaluationException {
        return expression.getValue(desiredResultType);
    }

    @Override
    public Object getValue(Object rootObject) throws EvaluationException {
        return expression.getValue(rootObject);
    }

    @Override
    public <T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException {
        return expression.getValue(rootObject, desiredResultType);
    }

    @Override
    public Object getValue(EvaluationContext context) throws EvaluationException {
        return expression.getValue(context);
    }

    @Override
    public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
        return expression.getValue(context, rootObject);
    }

    @Override
    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException {
        return expression.getValue(context, desiredResultType);
    }

    @Override
    public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType) throws EvaluationException {
        return expression.getValue(context, rootObject, desiredResultType);
    }

    @Override
    public Class<?> getValueType() throws EvaluationException {
        return expression.getValueType();
    }

    @Override
    public Class<?> getValueType(Object rootObject) throws EvaluationException {
        return expression.getValueType(rootObject);
    }

    @Override
    public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
        return expression.getValueType(context);
    }

    @Override
    public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
        return expression.getValueType(context, rootObject);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
        return expression.getValueTypeDescriptor();
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
        return expression.getValueTypeDescriptor(rootObject);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
        return expression.getValueTypeDescriptor(context);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException {
        return expression.getValueTypeDescriptor(context, rootObject);
    }

    @Override
    public boolean isWritable(Object rootObject) throws EvaluationException {
        return expression.isWritable(rootObject);
    }

    @Override
    public boolean isWritable(EvaluationContext context) throws EvaluationException {
        return expression.isWritable(context);
    }

    @Override
    public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
        return expression.isWritable(context, rootObject);
    }

    @Override
    public void setValue(Object rootObject, Object value) throws EvaluationException {
        expression.setValue(rootObject, value);
    }

    @Override
    public void setValue(EvaluationContext context, Object value) throws EvaluationException {
        expression.setValue(context, value);
    }

    @Override
    public void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException {
        expression.setValue(context, rootObject, value);
    }
}
