package com.ouo.mask.spel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.Map;

public class SpelTemplateExpressionResolver implements TemplateExpressionResolver {

    @Override
    public <T> T getValue(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables
            , TemplateParserContext placeholder, Class<T> resultType, PlaceholderExpressionParser<T> expressionParser) {
        if (StrUtil.isBlank(expression)) return null;
        EvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withRootObject(metadata)
                .build();
        Object[] args = metadata.getArgs();
        // 设置参数
        if (null != metadata && ArrayUtil.isNotEmpty(args)) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
                String[] ps = metadata.getParameterNames(); // 参数名称
                if (i < ArrayUtil.length(ps)) {
                    context.setVariable(ps[i], args[i]);
                }
            }
        }

        // 设置额外参数
        if (CollUtil.isNotEmpty(contextVariables)) {
            contextVariables.forEach((k, v) -> {
                if (StrUtil.isNotBlank(k)) context.setVariable(k, v);
            });
        }

        Expression exp = new EscapeSpelExpressionParser().parseExpression(expression, placeholder);
        if (exp instanceof ExpressionWrap) {
            return ((ExpressionWrap) exp).getValue(context, resultType, expressionParser);
        }

        return exp.getValue(context, resultType);
    }
}
