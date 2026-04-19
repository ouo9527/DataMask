package com.ouo.mask.core;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.enums.SceneEnum;
import com.ouo.mask.core.rule.DesensitizationProperties;
import com.ouo.mask.core.rule.DesensitizationRule;
import com.ouo.mask.kryo.DesensitizingKryo;
import com.ouo.mask.semi.SemiStructType;
import com.ouo.mask.semi.StringMapper;
import com.ouo.mask.util.DesensitizedUtil;
import com.ouo.mask.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class DefaultDesensitizer implements Desensitizer {
    // 全局脱敏规则
    private final DesensitizationProperties desensitizationProperties;
    // 半结构化处理器
    private final StringMapper stringMapper;
    // Kryo对象处理
    private final ThreadLocal<DesensitizingKryo> kryoThreadLocal;

    public DefaultDesensitizer(DesensitizationProperties desensitizationProperties, StringMapper stringMapper) {
        this.desensitizationProperties = desensitizationProperties;
        this.stringMapper = stringMapper;
        // FST（线程安全）
        //FSTConfiguration fstConfiguration = FSTConfiguration.createDefaultConfiguration();
        //fstConfiguration.setShareReferences(false); // 关闭对象引用共享，减少元数据开销
        //fstConfiguration.setForceSerializable(true); // 允许未实现Serializable or externalizable

        //fstConfiguration.setInstantiator(new FSTDefaultClassInstantiator()); // 避免反射创建实例

        //fstConfiguration.registerSerializer(Object.class, new FSTBasicObjectSerializer());

        this.kryoThreadLocal = ThreadLocal.withInitial(() -> {
            // Kryo（线程不安全）：copy方法支持transient修饰属性拷贝（但序列化不支持），不支持非静态内部类和只依赖无参构造函数，可以使用objenesis框架的StdInstantiatorStrategy策略解决
            DesensitizingKryo kryo = new DesensitizingKryo(this);

            return kryo;
        });
    }

    @Override
    public <T> T desensitized(SceneEnum scene, String fieldName, T data, Annotation annotation) {
        // 字符类型进行脱敏
        if (data instanceof CharSequence) {
            if (data instanceof String) {
                // 判断是否是JSON或XML字符串
                if (StrUtil.isTypeJson((String) data) || StrUtil.isTypeXml((String) data)) {
                    try {
                        return (T) stringMapper.transform((String) data, SceneEnum.LOG.equals(scene),
                                obj -> this.desensitized(scene, fieldName, obj, annotation));
                    } catch (Exception e) {
                        // 半结构化(如：JSON/XML)处理异常
                        log.warn("【{}】字段半结构化(如：JSON/XML)处理异常：{}", fieldName, e.getMessage());
                    }
                }

                if (data instanceof String) {
                    return (T) this.desensitized(scene, annotation, fieldName, (String) data);
                }
            } else {
                // CharSequence的具体类型是否具备String类型参数的构造方法，若具备则可重新创建原对象
                Constructor<?> constructor = ObjUtil.defaultIfNull(ReflectUtil.getConstructor(data.getClass(), String.class)
                        , ReflectUtil.getConstructor(data.getClass(), String[].class));
                if (null != constructor) {
                    try {
                        constructor.setAccessible(true);
                        CharSequence val = this.desensitized(scene, fieldName, Convert.convert(String.class, data), annotation);

                        return (T) constructor.newInstance(ArrayUtil.firstNonNull(constructor.getParameterTypes()).isArray() ? new CharSequence[]{val} : val);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        // String转换异常
                        log.warn("【{}】字段String转【{}】异常：{}", fieldName, constructor.getName(), e.getMessage());
                    }
                }

                return data;
            }
        }

        // 简单值类型(除字符类型即String、other CharSequenc外)不脱敏，包含原始类型、Number、Date、URI、URL、Locale、Class
        if (null == data || ClassUtil.isSimpleValueType(data.getClass())) {
            return data;
        }

        // Iterator一次性迭代器类型
        if (data instanceof Iterator) {
            return (T) this.desensitized(scene, fieldName, IterUtil.toList((Iterator) data), annotation).iterator();
        }

        // JsonNode类型(会经历JsonNode->序例化成XML字符串->反序列化成Object->序例化成XML字符串->反序列化成JsonNode)
        if (data instanceof JsonNode) {
            return (T) this.stringMapper.toBean(this.desensitized(scene, fieldName, data.toString(), annotation), data.getClass());
        }

        // DOC (会经历Node->序例化成XML字符串->反序列化成Map->序例化成XML字符串->反序列化成Node)
        if (data instanceof Node) {
            return (T) this.stringMapper.toBean(this.desensitized(scene, fieldName,
                    this.stringMapper.toString(data, SemiStructType.XML), annotation), data.getClass());
        }

        // 其他类型
        try {
            return kryoThreadLocal.get().sceneEnum(scene).curFieldName(fieldName).curAnnotRule(annotation).copy(data);
        } catch (Throwable e) {
            log.warn("【{}】类脱敏异常：{}", ClassUtil.getClassName(data, false), e.getMessage());
            return data;
        }
    }

    /**
     * key-val字符类型脱敏
     *
     * @param scene      脱敏场景
     * @param annotation 注解式脱敏规则
     * @param fieldName  待脱敏字段
     * @param val        待脱敏数据
     * @return 返回已脱敏数据
     */
    private String desensitized(SceneEnum scene, Annotation annotation, String fieldName, String val) {
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
