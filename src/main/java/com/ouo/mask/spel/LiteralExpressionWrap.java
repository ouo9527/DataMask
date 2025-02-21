package com.ouo.mask.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;

public class LiteralExpressionWrap extends ExpressionWrap {

    public LiteralExpressionWrap(int placeholderIndex, String literalValue) {
        super(placeholderIndex, new LiteralExpression(literalValue));
    }

    @Override
    @Nullable
    public Object getValue(@Nullable PlaceholderExpressionParser expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue() :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, null);
    }

    @Override
    @Nullable
    public Object getValue(@Nullable Object rootObject, @Nullable PlaceholderExpressionParser expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(rootObject) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex,
                        SimpleEvaluationContext.forReadOnlyDataBinding().withRootObject(rootObject).build());
    }

    @Override
    @Nullable
    public Object getValue(EvaluationContext context, @Nullable PlaceholderExpressionParser expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(context) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
    }

    @Override
    @Nullable
    public Object getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable PlaceholderExpressionParser expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(context, rootObject) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
    }

    @Override
    @Nullable
    public <T> T getValue(@Nullable Class<T> expectedResultType, @Nullable PlaceholderExpressionParser<T> expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(expectedResultType) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, null);
    }

    @Override
    @Nullable
    public <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> expectedResultType,
                          @Nullable PlaceholderExpressionParser<T> expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(rootObject, expectedResultType) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex,
                        SimpleEvaluationContext.forReadOnlyDataBinding().withRootObject(rootObject).build());
    }

    @Override
    @Nullable
    public <T> T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType,
                          @Nullable PlaceholderExpressionParser<T> expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(context, expectedResultType) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
    }

    @Override
    @Nullable
    public <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> expectedResultType,
                          @Nullable PlaceholderExpressionParser<T> expressionParser) {
        return 0 > placeholderIndex || null == expressionParser ? this.getValue(context, rootObject, expectedResultType) :
                expressionParser.handleExpression(this.getExpressionString(), placeholderIndex, context);
    }
}
