package com.ouo.mask.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;

public class SpelExpressionWrap extends ExpressionWrap {
    /**
     * Construct an expression, only used by the parser.
     *
     * @param placeholderIndex
     * @param expression
     */
    public SpelExpressionWrap(int placeholderIndex, Expression expression) {
        super(placeholderIndex, expression);
    }

    @Nullable
    public Object getValue(@Nullable PlaceholderExpressionParser expressionParser) {
        try {
            return this.getValue();
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, null);
            throw e;
        }
    }

    @Nullable
    public Object getValue(@Nullable Object rootObject, @Nullable PlaceholderExpressionParser expressionParser) {
        try {
            return this.getValue(rootObject);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex,
                        SimpleEvaluationContext.forReadOnlyDataBinding().withRootObject(rootObject).build());
            throw e;
        }
    }

    @Nullable
    public Object getValue(EvaluationContext context, @Nullable PlaceholderExpressionParser expressionParser) {
        try {
            return this.getValue(context);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
            throw e;
        }
    }


    @Nullable
    public Object getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable PlaceholderExpressionParser expressionParser) {
        try {
            return this.getValue(context, rootObject);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
            throw e;
        }
    }


    @Nullable
    public <T> T getValue(@Nullable Class<T> expectedResultType, @Nullable PlaceholderExpressionParser<T> expressionParser) {
        try {
            return this.getValue(expectedResultType);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, null);
            throw e;
        }
    }

    @Nullable
    public <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> expectedResultType,
                          @Nullable PlaceholderExpressionParser<T> expressionParser) {
        try {
            return this.getValue(rootObject, expectedResultType);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex,
                        SimpleEvaluationContext.forReadOnlyDataBinding().withRootObject(rootObject).build());
            throw e;
        }
    }

    @Nullable
    public <T> T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType,
                          @Nullable PlaceholderExpressionParser<T> expressionParser) {
        try {
            return this.getValue(context, expectedResultType);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
            throw e;
        }
    }

    @Nullable
    public <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> expectedResultType,
                          @Nullable PlaceholderExpressionParser<T> expressionParser) {
        try {
            return this.getValue(context, rootObject, expectedResultType);
        } catch (EvaluationException e) {
            if (null != expressionParser)
                return expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
            throw e;
        }
    }
}
