package com.ouo.mask.core.des;

import cn.hutool.core.map.MapUtil;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.rule.DesensitizationProperties;
import com.ouo.mask.core.rule.DesensitizationRule;
import com.ouo.mask.util.DesensitizedUtil;
import com.ouo.mask.util.StrUtil;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/***********************************************************
 * 字符串脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public class StringDesensitizer implements Desensitizer<String> {
    // 脱敏执行器
    private final DesensitizationExecutor executor;
    // 半结构化脱敏器
    private final SemiStructuredDesensitizer<String> desensitizer;
    // 全局脱敏规则
    private final DesensitizationProperties properties;

    @Override
    public DesensitizationExecutor getDesensitizationExecutor() {
        return this.executor;
    }

    @Override
    public String desensitize(String data, DesensitizationContext context) {
        // 判断是否时半结构化数据
        if (StrUtil.isTypeJson(data) || StrUtil.isTypeXml(data)) {
            return this.desensitizer.desensitize(data, context);
        }

        if (StrUtil.hasEmpty(context.getFieldName(), data)) return data;
        // 根据注解脱敏规则进行局部且精确脱敏
        if (context.getAnnotation() instanceof Empty)
            return DesensitizedUtil.emptyDesensitized((Empty) context.getAnnotation(),
                    context.getFieldName(), data);
        if (context.getAnnotation() instanceof Hash)
            return DesensitizedUtil.hashDesensitized((Hash) context.getAnnotation(),
                    context.getFieldName(), data);
        if (context.getAnnotation() instanceof Regex)
            return DesensitizedUtil.regexDesensitized((Regex) context.getAnnotation(),
                    context.getFieldName(), data);
        if (context.getAnnotation() instanceof Repl)
            return DesensitizedUtil.replDesensitized((Repl) context.getAnnotation(),
                    context.getFieldName(), data);
        if (context.getAnnotation() instanceof Mask)
            return DesensitizedUtil.maskDesensitized((Mask) context.getAnnotation(),
                    context.getFieldName(), data);
        // 根据配置中全局脱敏规则进行脱敏
        Map<String, DesensitizationRule> rules = null;
        if (null == this.properties || MapUtil.isEmpty(rules = this.properties.getRules()))
            return data;
        // 基于全局且按命名方式匹配脱敏
        String fieldName = StrUtil.toCamelCase2(context.getFieldName());
        return DesensitizedUtil.desensitized(rules.get(fieldName), fieldName, data);
    }
}
