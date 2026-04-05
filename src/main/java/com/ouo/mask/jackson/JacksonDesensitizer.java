package com.ouo.mask.jackson;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.ouo.mask.core.AbstractDesensitizer;
import com.ouo.mask.core.Desensitizer;
import com.ouo.mask.core.enums.SceneEnum;
import com.ouo.mask.core.rule.DesensitizationProperties;
import com.ouo.mask.semi.SemiStructType;
import com.ouo.mask.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/***********************************************************
 * Jackson脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
public final class JacksonDesensitizer extends AbstractDesensitizer {

    // 半结构化(如：JSON/XML)映射器
    private final JacksonObjectMapper objectMapper;

    public JacksonDesensitizer(DesensitizationProperties desensitizationProperties, JacksonObjectMapper objectMapper) {
        super(desensitizationProperties);
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T desensitized(SceneEnum scene, String fieldName, T data) {
        // 简单值类型，包含原始类型、Number、Date、URI、URL、Locale、Class
        if (null == data || ClassUtil.isSimpleValueType(data.getClass())) {
            return super.desensitized(scene, fieldName, data);
        }

        // 其他类型
        try {
            // 先将其他对象序列化成JSON字符串，然后在反序列化成对象
            /*return (T) JSON.parseObject(JSON.toJSONString(data, (ValueFilter) (object, name, value) -> {
                        try {
                            return this.desensitized(scene, ReflectUtil.getField(object.getClass(), name), value);
                        } catch (Throwable ignore) {
                            return value;
                        }
                    }, JSONWriter.Feature.ReferenceDetection, JSONWriter.Feature.PrettyFormat), data.getClass());*/
            Properties prop = new Properties();
            if (null != scene) {
                prop.put(Desensitizer.class.getName() + "#_scene", scene);
            }
            return (T) this.objectMapper.toBean(this.objectMapper.toString(data, SemiStructType.JSON
                    , prop), data.getClass());
        } catch (Throwable e) {
            //log.warn("【{}】类脱敏异常：{}", ClassUtil.getClassName(data, false), e.getMessage());
            return data;
        }
    }

    /**
     * key-data任意类型脱敏
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param data       待脱敏数据
     * @param annotation 注解式脱敏规则
     * @return 返回已脱敏数据
     */
    @Override
    public <T> T desensitized(SceneEnum scene, String fieldName, T data, Annotation annotation) {
        // 字符类型进行脱敏
        if (data instanceof CharSequence) {
            if (data instanceof String) {
                try {
                    return (T) this.transform(scene, fieldName, (String) data, annotation);
                } catch (Exception e) {
                    // 半结构化(如：JSON/XML)处理异常
                    //log.warn("【{}】字段半结构化(如：JSON/XML)处理异常：{}", fieldName, e.getMessage());
                }

                if (data instanceof String) {
                    return (T) this.desensitized(scene, fieldName, (String) data, annotation);
                }
            } else {
                // CharSequence的具体类型是否具备String类型参数的构造方法，若具备则可重新创建原对象
                Constructor<?> constructor = ObjUtil.defaultIfNull(ReflectUtil.getConstructor(data.getClass(), String.class)
                        , ReflectUtil.getConstructor(data.getClass(), String[].class));
                if (null != constructor) {
                    try {
                        constructor.setAccessible(true);
                        CharSequence val = this.desensitized(scene, fieldName, (CharSequence) Convert.convert(String.class, data), annotation);

                        return (T) constructor.newInstance(ArrayUtil.firstNonNull(constructor.getParameterTypes()).isArray() ? new CharSequence[]{val} : val);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        // String转换异常
                        //log.warn("【{}】字段String转【{}】异常：{}", fieldName, constructor.getName(), e.getMessage());
                    }
                }

                return data;
            }
        }

        return this.desensitized(scene, fieldName, data);
    }

    /**
     * 脱敏序例化字段的值
     *
     * @param value    待脱敏的序例化字段值
     * @param gen      JSON/XML格式生成器
     * @param provider 序列化提供器
     * @return 返回已脱敏的序例化字段值
     */
    public CharSequence desensitized(CharSequence value, JsonGenerator gen, SerializerProvider provider) {
        // gen.getCurrentValue()：获取当前序列化对象
        CharSequence newVal = null;

        SceneEnum scene = Convert.convertQuietly(SceneEnum.class, provider.getAttribute(Desensitizer.class.getName() + "#_scene"));
        Object field = ObjUtil.defaultIfNull(getField(gen.getOutputContext())
                , provider.getAttribute(Desensitizer.class.getName() + "#_fieldName"));

        if (field instanceof String) {
            Object annotation = provider.getAttribute(Desensitizer.class.getName() + "#_annotation");
            if (annotation instanceof Annotation) {
                newVal = this.desensitized(scene, (String) field, value, (Annotation) annotation);
            } else {
                newVal = this.desensitized(scene, (String) field, value);
            }
        } else if (field instanceof Field) {
            newVal = this.desensitized(scene, (Field) field, value);
        } else {
            newVal = this.desensitized(scene, value);
        }

        return ObjUtil.defaultIfNull(newVal, value);
    }

    /**
     * 转换半结构化数据
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param str        半结构化数据
     * @param annotation 注解式脱敏规则
     * @return 返回已转换后数据
     */
    private String transform(SceneEnum scene, String fieldName, String str, Annotation annotation) throws IOException {
        // 判断是否是JSON或XML字符串
        Properties prop = new Properties();
        if (null != scene) {
            prop.put(Desensitizer.class.getName() + "#_scene", scene);
        }
        if (StrUtil.isNotBlank(fieldName)) {
            prop.put(Desensitizer.class.getName() + "#_fieldName", fieldName);
        }
        if (null != annotation) {
            prop.put(Desensitizer.class.getName() + "#_annotation", annotation);
        }

        return StrUtil.isTypeJson(str) ? this.transformJson(str, prop, SceneEnum.LOG.equals(scene))
                : this.transformXml(str, prop, SceneEnum.LOG.equals(scene));
    }

    /**
     * 转换JSON字符串
     *
     * @param json     JSON字符串
     * @param prop     上下文属性
     * @param isPretty 是否美化
     * @return 返回转换后JSON字符串
     */
    private String transformJson(String json, Properties prop, boolean isPretty) {
        String val = (isPretty ? this.objectMapper.toPrettyString(json, SemiStructType.JSON, prop)
                : this.objectMapper.toString(json, SemiStructType.JSON, prop));

        return isPretty ? StrUtil.removeSuffix(StrUtil.startWith(val, System.lineSeparator())
                ? val : System.lineSeparator() + val, System.lineSeparator()) : val;
    }

    /**
     * 转换XML字符串
     *
     * @param xml      XML字符串
     * @param prop     上下文属性
     * @param isPretty 是否美化
     * @return 返回转换后XML字符串
     */
    private String transformXml(String xml, Properties prop, boolean isPretty) throws IOException {
        try (FromXmlParser parser = (FromXmlParser) this.objectMapper.getXmlMapper().createParser(xml)) {
            // 获取xml字符串根节点
            String rootName = JacksonObjectMapper.getXmlRoot(parser);
            // 包装XML片段
            if (StrUtil.isBlank(rootName)) {
                try {
                    parser.close();
                } catch (IOException ignore) {
                    //log.debug("【{}】XML片段流关闭异常：", val, e);
                }

                return this.transformXml(StrUtil.wrapXml(xml), prop, isPretty);
            }

            String val = (isPretty && !StrUtil.DEFAULT_ROOT_NAME.equals(rootName)
                    ? this.objectMapper.toPrettyString(parser.readValueAs(Object.class), SemiStructType.XML, rootName, prop)
                    : this.objectMapper.toString(parser.readValueAs(Object.class), SemiStructType.XML, rootName, prop));

            // 移除XML片段包装(不换行)
            if (StrUtil.DEFAULT_ROOT_NAME.equals(rootName)) {
                return StrUtil.strip(val, StrUtil.DEFAULT_START_ROOT_NODE,
                        StrUtil.DEFAULT_END_ROOT_NODE);
            }

            return isPretty ? StrUtil.removeSuffix(StrUtil.startWith(val, System.lineSeparator())
                    ? val : System.lineSeparator() + val, System.lineSeparator()) : val;
        }
    }

    /**
     * 获取当前序列化的字段
     *
     * @param context
     * @return
     */
    private Object getField(JsonStreamContext context) {
        if (null == context || context.inRoot()) return null;

        if (context.inObject()) {
            try {
                return ObjUtil.defaultIfNull(null == context.getCurrentValue() ? null
                                : ReflectUtil.getField(context.getCurrentValue().getClass(), context.getCurrentName())
                        , context.getCurrentName());
            } catch (RuntimeException ignore) {
                // 无法获取到字段
                return context.getCurrentName();
            }
        }

        return getField(context.getParent());
    }
}
