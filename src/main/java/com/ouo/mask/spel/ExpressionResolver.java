package com.ouo.mask.spel;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/***********************************************************
 * 模板或普通表达式解析器
 * 1）对于root object 根上下对象（推荐Java Bean），可以使用#root.属性或直接使用属性，
 *    例如：Bean对象：#root.name或name；Map对象：#root[name]或[name]或[0]
 * 2）对于evaluation context 上下文属性，需要使用#引用，例如：#name
 * Author:   刘春
 * Date:     2024/11/28
 ***********************************************************/
public interface ExpressionResolver {

    /**
     * 获取模板表达式对象
     *
     * @return 返回模板表达式对象
     */
    TemplateParserContext getTemplateParserContext();

    /**
     * 执行模板或普通表达式值
     *
     * @param expression 普通SpEL表达式
     * @return 返回SpEL表达式执行结果
     */
    default Object exe(String expression) {
        return this.exe(expression, Object.class);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression        普通SpEL表达式
     * @param ctxData           表达式上下文，可以是rootObj根上下对象或variables上下文属性
     * @return 返回SpEL表达式执行结果
     */
    default <T> Object exe(String expression, T ctxData) {
        return this.exe(expression, ctxData, Object.class);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression 普通SpEL表达式
     * @param resultType 结果类型
     * @return 返回SpEL表达式执行结果
     */
    default <R> R exe(String expression, Class<R> resultType) {
        return this.exe(expression, null, resultType);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression        普通SpEL表达式
     * @param ctxData           表达式上下文，可以是rootObj根上下对象或variables上下文属性
     * @param resultType        结果类型
     * @return 返回SpEL表达式执行结果
     */
    default <T, R> R exe(String expression, T ctxData, Class<R> resultType) {
        return this.exe(expression, ctxData, resultType, null);
    }

    /**
     * 执行模板或普通表达式值
     *
     * @param expression        SpEL模板表达式
     * @param ctxData           表达式上下文，可以是rootObj根上下对象或variables上下文属性
     * @param resultType        结果类型
     * @param callback          SpEL表达式执行结果回调处理
     * @return 返回SpEL表达式执行结果
     */
    default <T, R> R exe(String expression, T ctxData, Class<R> resultType, @Nullable TemplateExpressionCallback callback) {
        if (StrUtil.isBlank(expression)) return null;

        Object rootObj = null;
        Object[] args = null;
        if (ctxData instanceof SpelExpressionMetaData) {
            rootObj = ctxData;
            args = ((SpelExpressionMetaData) ctxData).getArgs();
        } else if (ArrayUtil.isArray(ctxData)) args = ArrayUtil.cast(Object.class, ctxData);
        else if (ctxData instanceof Collection) args = ArrayUtil.toArray((Collection) ctxData, Object.class);
        else if (ctxData instanceof Iterator) args = ArrayUtil.toArray((Iterator) ctxData, Object.class);
            //else if (ctxData instanceof Iterable) args = ArrayUtil.toArray((Iterable) ctxData, Object.class);
        else {
            rootObj = ctxData;
            args = new Object[]{ctxData};
        }

        EvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withRootObject(rootObj) // 设置根上下文对象
                .build();

        // 设置上下文属性
        if (ArrayUtil.isNotEmpty(args)) {
            for (int i = 0; i < args.length; i++) {
                if (null == args[i]) continue;
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
        }

        if (ctxData instanceof Map) {
            for (Object entry : ((Map) ctxData).entrySet()) {
                if (entry instanceof Map.Entry) {
                    context.setVariable(ObjUtil.toString(((Map.Entry) entry).getKey()), ((Map.Entry) entry).getValue());
                }
            }
        }

        Expression exp = new EscapeSpelExpressionParser().parseExpression(expression, getTemplateParserContext());
        if (exp instanceof CompositeStringExpressionWrap) {
            return ((CompositeStringExpressionWrap) exp).getValue(context, resultType, callback);
        }

        return exp.getValue(context, resultType);
    }
}
