package com.ouo.mask.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.ExpressionUtils;

import java.util.List;

public class CompositeStringExpressionWrap extends ExpressionWrap {

    private List<Expression> expressions;

    public CompositeStringExpressionWrap(String templateExpression, List<Expression> expressions) {
        super(-1, new CompositeStringExpression(templateExpression, expressions.toArray(new Expression[]{})));
        this.expressions = expressions;
    }

    @Override
    public Object getValue(PlaceholderExpressionParser expressionParser) {
        StringBuilder sb = new StringBuilder();
        for (Expression expression : this.expressions) {
            Object value = null;
            if (expression instanceof ExpressionWrap) {
                value = ((ExpressionWrap) expression).getValue(expressionParser);
            } else value = expression.getValue();
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public <T> T getValue(Class<T> expectedResultType, PlaceholderExpressionParser<T> expressionParser) {
        Object value = getValue(expressionParser);
        return ExpressionUtils.convertTypedValue(null, new TypedValue(value), expectedResultType);
    }

    @Override
    public Object getValue(Object rootObject, PlaceholderExpressionParser expressionParser) {
        StringBuilder sb = new StringBuilder();
        for (Expression expression : this.expressions) {
            Object value = null;
            if (expression instanceof ExpressionWrap) {
                value = ((ExpressionWrap) expression).getValue(rootObject, expressionParser);
            } else value = expression.getValue(rootObject);
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public <T> T getValue(Object rootObject, Class<T> expectedResultType, PlaceholderExpressionParser<T> expressionParser) {
        Object value = getValue(rootObject, expressionParser);
        return ExpressionUtils.convertTypedValue(null, new TypedValue(value), expectedResultType);
    }

    @Override
    public Object getValue(EvaluationContext context, PlaceholderExpressionParser expressionParser) {
        StringBuilder sb = new StringBuilder();
        for (Expression expression : this.expressions) {
            Object value = null;
            if (expression instanceof ExpressionWrap) {
                value = ((ExpressionWrap) expression).getValue(context, expressionParser);
            } else value = expression.getValue(context);
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public <T> T getValue(EvaluationContext context, Class<T> expectedResultType, PlaceholderExpressionParser<T> expressionParser) {
        Object value = getValue(context, expressionParser);
        return ExpressionUtils.convertTypedValue(context, new TypedValue(value), expectedResultType);
    }

    @Override
    public Object getValue(EvaluationContext context, Object rootObject, PlaceholderExpressionParser expressionParser) {
        StringBuilder sb = new StringBuilder();
        for (Expression expression : this.expressions) {
            Object value = null;
            if (expression instanceof ExpressionWrap) {
                value = ((ExpressionWrap) expression).getValue(context, rootObject, expressionParser);
            } else value = expression.getValue(context, rootObject);
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> expectedResultType, PlaceholderExpressionParser<T> expressionParser) {
        Object value = getValue(context, rootObject, expressionParser);
        return ExpressionUtils.convertTypedValue(context, new TypedValue(value), expectedResultType);
    }
}
