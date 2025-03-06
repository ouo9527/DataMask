package com.ouo.mask.spel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;

import java.util.Objects;

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
            else if (String.class.equals(expectedResultType)) return (T) this.tryObj2Str(this.getValue());
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
            else if (String.class.equals(expectedResultType)) return (T) this.tryObj2Str(this.getValue(rootObject));
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
            else if (String.class.equals(expectedResultType)) return (T) this.tryObj2Str(this.getValue(context));
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
            else if (String.class.equals(expectedResultType))
                return (T) this.tryObj2Str(this.getValue(context, rootObject));
            throw e;
        }
    }

    /**
     * 尝试对象转字符串：先尝试转json字符串，否则才调用toString()
     *
     * @param obj
     * @return
     */
    private String tryObj2Str(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper()
                // 配置空对象序列化不报错
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                // 配置null值的字段不序列化
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return Objects.toString(obj, "");
        }
    }
}
