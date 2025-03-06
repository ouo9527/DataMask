package com.ouo.mask.support.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.spel.SpelExpressionMetaData;
import com.ouo.mask.spel.SpelExpressionResolver;
import com.ouo.mask.util.SpringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/***********************************************************
 * TODO:     日志脱敏解析器
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
    default String resolvePlaceholder(String loggerName, String template, Object[] args) {
        if (StrUtil.isBlank(template) || ArrayUtil.isEmpty(args)) {
            return template;
        }
        DesensitizationHandler desensitizedhandler = SpringUtil.getBean(DesensitizationHandler.class);
        SpelExpressionResolver expressionResolver = SpringUtil.getBean(SpelExpressionResolver.class);
        // 先对日志参数脱敏 Collectors.toMap中key和value都不支持null，否则报空指针，因此需进行如转换：Collectors.toMap(i -> "p" + i, i -> null == args[i] ? "null" : args[i])
        final Map<String, Object> data = desensitizedhandler.desensitized(loggerName, SceneEnum.LOG, null,
                IntStream.range(0, args.length).boxed().collect(HashMap::new, (m, i) -> m.put("p" + i, args[i]), HashMap::putAll));

        return expressionResolver.getValueByLogTemplate(template, new SpelExpressionMetaData(data.values().toArray()), (e, i, c) -> {
            Object val = null;
            if (StrUtil.isEmpty(e)) { // 只含{}占位符
                val = i < args.length ? data.get("p" + i) : "{}";
            } else if (1 == args.length || i < args.length) { // 当参数仅且只有一个时，则无论多少个占位符，都始终指向该参数取值，否则有效占位符表达式的值为与之对应的参数
                Object p = data.get("p" + (1 == args.length ? 0 : i)); // 参数
                val = BeanUtil.getFieldValue(p, e); // 获取对象属性
                if (null == val) { // 若获取为空，则判断是否是简单值类型或简单值类型的数组
                    if (null != p && ClassUtil.isSimpleTypeOrArray(p.getClass()))
                        val = desensitizedhandler.desensitized(loggerName, SceneEnum.LOG, e, p);
                    else return null; // 否则返回null
                }
            } else return "{}";
            return Objects.toString(val, null);
        });
    }
}
