package com.ouo.mask.support.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.core.Desensitizer;
import com.ouo.mask.core.enums.SceneEnum;
import com.ouo.mask.semi.SemiStructMapper;
import com.ouo.mask.spel.BraceSpelExpressionResolver;
import com.ouo.mask.spel.ExpressionResolver;
import com.ouo.mask.util.SpringUtil;
import org.slf4j.helpers.MessageFormatter;

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
     * @param loggerName
     * @param template   日志格式，支持spel表达式
     * @param args       日志参数
     * @return
     */
    default String resolvePlaceholder(String loggerName, String template, final Object[] args) {
        if (StrUtil.isBlank(template) || ArrayUtil.isEmpty(args)) {
            return template;
        }
        Desensitizer desensitizer = SpringUtil.getBean(Desensitizer.class, true);
        // 对原数据进行脱敏
        final Object[] results = null == desensitizer ? args : desensitizer.desensitized(SceneEnum.LOG, args);
        ExpressionResolver expressionResolver = SpringUtil.getBean(BraceSpelExpressionResolver.class, true);
        if (null != expressionResolver) {
            return expressionResolver.exe(template, 1 == results.length ? results[0] : results, String.class, (expression, result, e) -> {
                int index = expression.getPlaceholderIndex();
                if (StrUtil.isBlank(expression.getExpressionString()))
                    return index >= results.length ? "{}" : results[index];
                if (null == e && null != result) return result; // Spring SpEl可以解析
                Object val = null;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] instanceof CharSequence) {
                        try {
                            val = BeanUtil.getProperty(SpringUtil.getBean(SemiStructMapper.class).toBean((String) results[i], Object.class)
                                    , expression.getExpressionString());
                        } catch (Exception ex) {
                            // 非json或xml字符串
                        }
                    }
                    // 取到值
                    if (null != val) break;
                }
                // 轮询后还是取不到值，使用表达式索引位置取对应值，然后进行脱敏
                if (null == val && index < results.length) {
                    String fieldName = StrUtil.subAfter(expression.getExpressionString(), ".", true);
                    fieldName = StrUtil.subBetween(StrUtil.blankToDefault(fieldName, expression.getExpressionString()), "[", "]");
                    val = desensitizer.desensitized(SceneEnum.LOG, StrUtil.trim(StrUtil.blankToDefault(fieldName,
                            expression.getExpressionString())), results[index]);
                }

                return val;
            });
        }
        return MessageFormatter.arrayFormat(template, results).getMessage();
    }
}
