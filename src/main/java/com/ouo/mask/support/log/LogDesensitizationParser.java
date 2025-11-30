package com.ouo.mask.support.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.spel.SpelExpressionMetaData;
import com.ouo.mask.spel.SpelExpressionResolver;
import com.ouo.mask.util.SpringUtil;

import java.util.Objects;

/***********************************************************
 * 日志脱敏解析器
 *
 * Author:   刘春
 * Date:     2024/4/26
 ***********************************************************/
public interface LogDesensitizationParser {

    /**
     * 解析日志模板，并进行脱敏，其脱敏条件参数必须例如：
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
        DesensitizationHandler desensitizedhandler = SpringUtil.getBean(DesensitizationHandler.class);
        if (StrUtil.isBlank(template) || ArrayUtil.isEmpty(args) || null == desensitizedhandler) {
            return template;
        }
        SpelExpressionResolver expressionResolver = SpringUtil.getBean(SpelExpressionResolver.class);
        final Object[] results = desensitizedhandler.desensitized(loggerName, SceneEnum.LOG, args);
        return expressionResolver.getValueByLogTemplate(template, new SpelExpressionMetaData(results), (e, i, c) -> {
            Object val = null;
            if (StrUtil.isEmpty(e)) { // 只含{}占位符
                val = i < results.length ? results[i] : "{}";
            } else if (1 == results.length || i < results.length) { // 当参数仅且只有一个时，则无论多少个占位符，都始终指向该参数取值，否则有效占位符表达式的值为与之对应的参数
                int n = 1 == results.length ? 0 : i; //索引位
                Object p = results[n]; // 参数
                val = BeanUtil.getFieldValue(p, e); // 获取对象属性
                if (null == val) { // 若获取为空，则判断是否是字符类型
                    try {
                        val = !(p instanceof CharSequence) || StrUtil.equals((CharSequence) p, Objects.toString(args[n], ""))
                                ? desensitizedhandler.desensitized(SceneEnum.LOG, e, p) : p;
                    } catch (RuntimeException ex) {

                    }
                }
            } else return "{}";
            return Objects.toString(val, null);
        });
    }
}
