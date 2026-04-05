package com.ouo.mask.core;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.enums.SceneEnum;
import com.ouo.mask.core.rule.DesensitizationProperties;
import com.ouo.mask.core.rule.DesensitizationRule;
import com.ouo.mask.util.DesensitizedUtil;
import com.ouo.mask.util.StrUtil;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.Map;

/***********************************************************
 * 抽象脱敏器（提供字段/属性脱敏）
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public abstract class AbstractDesensitizer implements Desensitizer {

    // 全局脱敏规则
    protected final DesensitizationProperties desensitizationProperties;

    /**
     * key-val字符类型脱敏
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param val        待脱敏数据
     * @param annotation 注解式脱敏规则
     * @return 返回已脱敏数据
     */
    protected String desensitized(SceneEnum scene, String fieldName, String val, Annotation annotation) {
        if (StrUtil.hasEmpty(fieldName, val)) return val;
        scene = ObjUtil.defaultIfNull(scene, SceneEnum.ALL);
        // 根据注解脱敏规则进行局部且精确脱敏
        if (annotation instanceof Empty)
            return DesensitizedUtil.emptyDesensitized(scene, (Empty) annotation,
                    fieldName, val);
        if (annotation instanceof Hash)
            return DesensitizedUtil.hashDesensitized(scene, (Hash) annotation,
                    fieldName, val);
        if (annotation instanceof Regex)
            return DesensitizedUtil.regexDesensitized(scene, (Regex) annotation,
                    fieldName, val);
        if (annotation instanceof Repl)
            return DesensitizedUtil.replDesensitized(scene, (Repl) annotation,
                    fieldName, val);
        if (annotation instanceof Mask)
            return DesensitizedUtil.maskDesensitized(scene, (Mask) annotation,
                    fieldName, val);
        // 根据配置中全局脱敏规则进行脱敏
        Map<String, DesensitizationRule> rules = null;
        if (null == desensitizationProperties || MapUtil.isEmpty(rules = desensitizationProperties.getRules()))
            return val;
        // 基于全局且按命名方式匹配脱敏
        String field = StrUtil.toCamelCase2(fieldName);
        return DesensitizedUtil.desensitized(scene, rules.get(field), field, val);
    }
}
