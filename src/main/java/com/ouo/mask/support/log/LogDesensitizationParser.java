package com.ouo.mask.support.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.semi.SemiStructuredMapper;
import com.ouo.mask.spel.BraceSpelExpressionEvaluator;
import com.ouo.mask.spel.SpelEvaluationContext;
import com.ouo.mask.spel.SpelExpressionEvaluator;
import com.ouo.mask.util.SpringUtil;
import com.ouo.mask.util.StrUtil;
import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;

/***********************************************************
 * 日志脱敏解析器
 *
 * Author:   ouo
 * Date:     2024/4/26
 ***********************************************************/
public interface LogDesensitizationParser {
    /**
     * 解析日志模板，采取轮询查找，即轮询从参数中取值，直到所有参数中都无法取到，那么此时取相应位置参数作为值，
     * 若占位符索引超过参数个数，则有占位符视为null，否则视为{}；其次并对获取到值进行脱敏，例如：
     * 1）log.info("模板1即key-value成对：{#p0}、{#p1}...", val1, val2) --输出：val1、val2
     * 2）log.info("模板2即多key一个bean：{}、{#p0.name}...", bean) --输出：bean、bean的name属性值
     * 3）log.info("模板3即无key：{}、{}...", bean, val2) --输出：bean、val2
     * <p>
     *
     * @param loggerName 日志器的名称
     * @param template 日志格式，支持spel表达式
     * @param args 日志参数
     * @return 返回脱敏后数据
     */
    default String resolvePlaceholder(String loggerName, String template, final Object[] args) {
        if (StrUtil.isBlank(template) || ArrayUtil.isEmpty(args)) {
            return template;
        }
        final SpelExpressionEvaluator evaluator = SpringUtil.getBean(BraceSpelExpressionEvaluator.class, true);
        final SemiStructuredMapper mapper = SpringUtil.getBean(SemiStructuredMapper.class, true);

        if (null != evaluator && null != mapper) {
            final DesensitizationExecutor executor = SpringUtil.getBean(DesensitizationExecutor.class, true);

            return evaluator.exe(template, String.class, SpelEvaluationContext.builder().args(args).build(),
                    (expression, context, result, e) -> {
                Object val = null;  // 脱敏后值
                int index = expression.getPlaceholderIndex();
                // 空表达式
                if (StrUtil.isBlank(expression.getExpressionString())) {
                    val = index >= args.length ? "{}" : null == executor ? args[index] :
                            executor.desensitize(args[index]);
                } else {
                    Object currentObj = null; // 当前属性/字段所取值的对象
                    Object propVal = null;  // 当前属性/字段的值
                    // Spring SpEl可以正常解析
                    if (null == e && null != result) {
                        propVal = result;
                        // 若非单层引用，如#p0.，则此时所获取到当前属性/字段取值的对象会存在问题！！
                        currentObj = ObjUtil.defaultIfNull(context.lookupVariable(StrUtil.toObjName(
                                expression.getExpressionString())), context.getRootObject().getValue());
                    } else {
                        // Spring SpEl无法正常解析，则从半结构化数据中采用Hutool#BeanUtil进行获取
                        for (Object arg : args) {
                            try {
                                currentObj = (arg instanceof String) ? mapper.toBean((String) arg, Object.class) : arg;
                                // 采用Hutool#BeanUtil获取表达式值
                                propVal = BeanUtil.getProperty(currentObj, expression.getExpressionString());
                            } catch (RuntimeException ignore) {
                                // 非json或xml字符串
                            }

                            // 取到值
                            if (ArrayUtil.isArray(propVal)) {
                                if (!ArrayUtil.isAllNull((Object[]) propVal)) break;
                            } else if (propVal instanceof Collection) {
                                if (!CollUtil.allMatch((Collection<?>) propVal, Objects::isNull)) break;
                            } else if (!ObjUtil.isAllEmpty(propVal)) break;
                        }

                        // 都无法解析，则采取轮询按位置取值，若轮询后取不到值，则使用表达式索引位置取对应值，然后进行脱敏
                        if (null == propVal && index < args.length) {
                            try {
                                currentObj = (args[index] instanceof String) ? mapper.toBean((String) args[index],
                                        Object.class) : args[index];
                                // 采用Hutool#BeanUtil获取表达式值
                                propVal = BeanUtil.getProperty(currentObj, expression.getExpressionString());
                                if (ArrayUtil.isArray(propVal)) {
                                    if (ArrayUtil.isAllNull((Object[]) propVal)) propVal = args[index];
                                } else if (propVal instanceof Collection) {
                                    if (CollUtil.allMatch((Collection) propVal, Objects::isNull))
                                        propVal = args[index];
                                } else if (null == propVal) propVal = args[index];
                            } catch (Exception ignore) {
                                // 非json或xml字符串
                                propVal = args[index];
                            }
                        }
                    }

                    if (null == executor || ObjUtil.isEmpty(propVal)) {
                        val = propVal;
                    } else {
                        // 从表达式中获取属性/字段
                        String propName = StrUtil.toPropName(expression.getExpressionString());
                        Field field = null;
                        if (null != currentObj) {
                            try {
                                field = ReflectUtil.getField(currentObj.getClass(), propName);
                            } catch (SecurityException ignore) {
                            }
                        }

                        val = executor.desensitize(propVal, DesensitizationContext.builder().fieldName(propName)
                                .field(field).build());
                    }
                }

                return StrUtil.toStringOrNull(val);
            });
        }
        return MessageFormatter.arrayFormat(template, args).getMessage();
    }
}
