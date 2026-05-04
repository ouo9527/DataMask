package com.ouo.mask.core;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.rule.DesensitizationProperties;
import lombok.Builder;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

/***********************************************************
 * 脱敏上下文
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Getter
@Builder(builderClassName = "Builder")
public class DesensitizationContext {
    private final String fieldName; // 当前脱敏字段/属性名称
    private final Annotation annotation; // 当前脱敏字段脱敏注解规则
    // 全局脱敏配置
    private final DesensitizationProperties properties;

    //private DesensitizationContext parent;

    DesensitizationContext(String fieldName, Annotation annotation, DesensitizationProperties properties, DesensitizationContext parent) {
        this.fieldName = StrUtil.blankToDefault(fieldName, null == parent ? null : parent.getFieldName());
        this.annotation = ObjUtil.defaultIfNull(annotation, (null == parent ? null : parent.getAnnotation()));
        this.properties = ObjUtil.defaultIfNull(properties, null == parent ? null : parent.getProperties());
        //this.parent = parent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(DesensitizationContext context) {
        return new Builder(context);
    }

    public static class Builder {
        private String fieldName;
        private Annotation annotation;
        private DesensitizationProperties properties;
        private DesensitizationContext parent;

        Builder() {
        }

        Builder(DesensitizationContext parent) {
            this.parent = parent;
        }

        public Builder field(Field field) {
            if (null != field) {
                this.fieldName = field.getName();
                this.annotation = Arrays.stream(field.getAnnotations())
                        .filter(a -> a instanceof Empty || a instanceof Hash || a instanceof Regex
                                || a instanceof Repl || a instanceof Mask)
                        .findAny().orElse(null);
            }

            return this;
        }

        public DesensitizationContext build() {
            return new DesensitizationContext(this.fieldName, this.annotation, this.properties, this.parent);
        }
    }
}
