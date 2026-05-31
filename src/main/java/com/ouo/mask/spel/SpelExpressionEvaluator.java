package com.ouo.mask.spel;

import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/***********************************************************
 * SpEL表达式执行器，例如：#name、#p0、#a0
 *
 * Author:   ouo
 * Date:     2024/11/28
 ***********************************************************/
@Getter
public abstract class SpelExpressionEvaluator {
    // SpEL表达式解析器
    private final SpelExpressionParser parser;

    /**
     * Create a new instance with the specified {@link SpelExpressionParser}.
     */
    public SpelExpressionEvaluator(SpelExpressionParser parser) {
        Assert.notNull(parser, "SpelExpressionParser must not be null");
        this.parser = parser;
    }

    /**
     * Create a new instance with a default {@link EscapeSpelExpressionParser}.
     */
    public SpelExpressionEvaluator() {
        this(new EscapeSpelExpressionParser());
    }

    /**
     * a context for influencing this expression parsing routine (optional)
     *
     * @return
     */
    public abstract ParserContext getParserContext();


    /**
     * 执行模板或普通表达式值
     *
     * @param expression  模板或普通表达式
     * @param resultType  结果类型
     * @param evalContext 表达式执行上下文
     * @param fn          SpEL表达式执行结果回调处理
     * @return 返回SpEL表达式执行结果
     */
    public <R> R exe(String expression, Class<R> resultType, @Nullable EvaluationContext evalContext, @Nullable PlaceholderExpressionFunction<R> fn) {
        Expression exp = this.getParser().parseExpression(expression, this.getParserContext());
        if (exp instanceof TemplateStringExpression) {
            return ((TemplateStringExpression) exp).getValue(evalContext, resultType, fn);
        }

        return exp.getValue(evalContext, resultType);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression  模板或普通表达式
     * @param resultType  结果类型
     * @param evalContext 表达式执行上下文
     * @return 返回SpEL表达式执行结果
     */
    public <R> R exe(String expression, Class<R> resultType, @Nullable EvaluationContext evalContext) {
        return this.exe(expression, resultType, evalContext, null);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression  模板或普通表达式
     * @param evalContext 表达式执行上下文
     * @param fn          SpEL表达式执行结果回调处理
     * @return 返回SpEL表达式执行结果
     */
    public Object exe(String expression, @Nullable EvaluationContext evalContext, @Nullable PlaceholderExpressionFunction<Object> fn) {
        return this.exe(expression, Object.class, evalContext, fn);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression 模板或普通表达式
     * @return 返回SpEL表达式执行结果
     */
    public Object exe(String expression, @Nullable EvaluationContext evalContext) {
        return this.exe(expression, Object.class, evalContext, null);
    }
}
