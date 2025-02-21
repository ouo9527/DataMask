package com.ouo.mask.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ouo.mask.annotation.*;
import com.ouo.mask.config.DesensitizationSource;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.rule.DesensitizationStrategy;
import com.ouo.mask.util.DesensitizedUtil;
import com.ouo.mask.util.StringUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;


/***********************************************************
 * TODO:     数据脱敏处理器
 * Author:   刘春
 * Date:     2022/12/3
 ***********************************************************/
@Slf4j
public class DefaultDesensitizationHandler implements DesensitizationHandler {

    // TODO: 全局脱敏规则
    @Setter
    private DesensitizationSource desensitizationSource;

    @Setter
    private ObjectMapper objectMapper;

    @Setter
    private XmlMapper xmlMapper;

    @Override
    public <T> T desensitized(String context, SceneEnum scene, String fieldName, T data) {
        // 根据策略进行校验是否支持脱敏
        if (this.supports(context, data)) {
            try {
                return this.desensitized(scene, fieldName, data, null);
            } catch (RuntimeException e) {
                log.debug("脱敏异常：", e);
            }
        }
        return data;
    }

    /**
     * 在data数据中查找fieldName字段值
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param data       待脱敏数据
     * @param annotation 待脱敏字段注解脱敏规则
     * @return 已脱敏数据
     */
    private <T> T desensitized(SceneEnum scene, String fieldName, T data, Annotation annotation) {
        // TODO: 字符类型进行脱敏
        if (data instanceof CharSequence) {
            return (T) this.desensitized(scene, fieldName, (CharSequence) data, annotation);
        }
        // TODO: 简单值类型(除字符类型即String、other CharSequenc)不脱敏，包含原始类型、Number、Date、URI、URL、Locale、Class
        if (null == data || ClassUtil.isSimpleValueType(data.getClass())) {
            return data;
        }
        // TODO: 数组类型循环递归处理
        if (ArrayUtil.isArray(data)) {
            // TODO：基础类型的数组不能强转为Object类型数组，而是要转成相应的包装类型
            return (T) this.desensitized(scene, fieldName, ArrayUtil.wrap(data), annotation);
        }
        // TODO: 集合类型循环递归处理
        if (data instanceof Collection) {
            return (T) this.desensitized(scene, fieldName, (Collection) data, annotation);
        }
        // TODO: 迭代器类型循环递归处理
        if (data instanceof Iterator) {
            return (T) this.desensitized(scene, fieldName, (Iterator) data, annotation);
        }
        // TODO: 映射类型循环递归处理
        if (data instanceof Map) {
            return (T) this.desensitized(scene, fieldName, (Map) data, annotation);
        }
        // TODO: XML格式处理
        /*if (data instanceof Document) {
            return (T) this.desensitized(scene, fieldName, (Document) data);
        }*/
        // TODO: 其他对象类型脱敏处理
        return this.desensitized(scene, fieldName, data);
    }

    /**
     * 其他对象类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene     脱敏场景
     * @param fieldName 待脱敏字段
     * @param data      待脱敏数据
     * @return 已脱敏数据
     */
    private <T> T desensitized(SceneEnum scene, String fieldName, T data) {
        try {
            // TODO: 获取对象中属性是否有脱敏规则注解修饰，当fieldName为空时，表示对象所有字段都需脱敏
            Field[] fields = ReflectUtil.getFields(data.getClass());
            // 深拷贝（通过反射+递归，即需要存在无参构造器）
            T result = (T) ReflectUtil.newInstance(data.getClass()); // SerializeUtil.clone(data); // 深拷贝（通过序列化即需要实现Serializable）
            if (null == result) return data;
            Arrays.stream(fields).forEach(f -> {
                if (null != f) {
                    Object v = ReflectUtil.getFieldValue(data, f);
                    if (null != v) {
                        try {
                            ReflectUtil.setFieldValue(result, f.getName(), this.desensitized(scene, f.getName(), v,
                                    Arrays.stream(f.getAnnotations())
                                            .filter(a -> a instanceof Empty || a instanceof Hash || a instanceof Regex
                                                    || a instanceof Repl || a instanceof Mask)
                                            .findAny().orElse(null)));
                        } catch (RuntimeException e) {
                            log.debug("【{}】对象【{}】字段脱敏异常：", data.getClass().getName(), f.getName(), e);
                        }
                    }
                }
            });

            return result;

        } catch (RuntimeException e) {
            // 安全性约束、字段不存在等原因无法操作字段属性
            log.debug("【{}】对象脱敏异常：", data.getClass().getName(), e);
            return data;
        }
    }

    /**
     * XML格式脱敏处理：在data数据中查找fieldName字段值
     * @param scene 脱敏场景
     * @param fieldName 待脱敏字段
     * @param data 待脱敏数据
     * @return 已脱敏数据
     */
    /*private Document desensitized(SceneEnum scene, String fieldName, Document data) {
        // TODO: 拷贝一份
        Document dom = null;
        try {
            dom = DocumentHelper.parseText((data).asXML());
        } catch (DocumentException e) {
            //log.debug("非法XML：", e);
        }
        if (null != dom && null != desensitizationSource && MapUtil.isNotEmpty(desensitizationSource.getRules())) {
            // TODO: 获取XML对象中属性是否有脱敏规则注解修饰，当fieldName为空时，表示XML对象所有字段都需脱敏
            Set<String> fields = StrUtil.isBlank(fieldName) ? desensitizationSource.getRules().keySet() : new HashSet<>(1);
            if (CollUtil.isEmpty(fields) && StrUtil.isNotBlank(fieldName)) fields.add(fieldName);
            for (String field : fields) {
                // TODO: 通过XPath处理，忽略大小写匹配
                List<Node> nodes = dom.selectNodes("//*[translate(local-name(), " +
                        "'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') = '"
                        + StrUtil.trimToEmpty(field).toUpperCase() + "']");
                //dom.selectNodes("//" + StrUtil.trimToEmpty(entry.getKey()));
                if (CollUtil.isNotEmpty(nodes)) {
                    for (Node node : nodes) {
                        if (null == node) continue;
                        node.setText(Objects.toString(this.desensitized(scene, field, node.getText(), null), null));
                    }
                }
            }
        }
        return dom;
    }*/


    /**
     * 迭代器类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene     脱敏场景
     * @param fieldName 待脱敏字段
     * @param data      待脱敏数据
     * @return 已脱敏数据
     */
    private Iterator desensitized(SceneEnum scene, String fieldName, Iterator data, Annotation annotation) {
        // TODO: 拷贝一份
        final List<Object> copyDatas = new ArrayList<>();
        while (data.hasNext()) {
            copyDatas.add(this.desensitized(scene, fieldName, data.next(), annotation));
        }
        return copyDatas.iterator();
    }

    /**
     * 集合类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene     脱敏场景
     * @param fieldName 待脱敏字段
     * @param data      待脱敏数据
     * @return 已脱敏数据
     */
    private Collection desensitized(SceneEnum scene, String fieldName, Collection data, Annotation annotation) {
        // TODO: 拷贝一份
        /*return (Collection) Collections.synchronizedCollection(data)
                .parallelStream() // 所传进来的map參数不是线程安全的，并行操作时会存在数据一致性问题。因此需要将线程不安全的map转成线程安全的，如Collections.synchronizedCollection(data)等
                .map(o -> this.desensitized(scene, fieldName, o, annotation))
                .collect(data instanceof Set ? Collectors.toSet() : Collectors.toList());*/
        Collection result = CollUtil.create(data.getClass());
        for (Object o : data) {
            result.add(this.desensitized(scene, fieldName, o, annotation));
        }
        return result;
    }

    /**
     * 数组类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene     脱敏场景
     * @param fieldName 待脱敏字段
     * @param data      待脱敏数据
     * @return 已脱敏数据
     */
    private Object[] desensitized(SceneEnum scene, String fieldName, Object[] data, Annotation annotation) {
        // TODO: 拷贝一份
        // 1万条内使用for处理；1万～10万条之间使用stream流式处理；10万～100万条时，采用parallel多线程并行处理
        /*return new CopyOnWriteArrayList(data)
                .parallelStream() // 所传进来的map參数不是线程安全的，并行操作时会存在数据一致性问题。因此需要将线程不安全的map转成线程安全的，如Collections.synchronizedList(data)或new CopyOnWriteArrayList(data)等
                .map(o -> this.desensitized(scene, fieldName, o, annotation))
                .toArray();*/
        List result = new ArrayList(data.length);
        for (Object o : data) {
            result.add(this.desensitized(scene, fieldName, o, annotation));
        }
        return result.toArray();
    }

    /**
     * 映射类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param data       待脱敏数据
     * @param annotation 待脱敏字段注解脱敏规则
     * @return 已脱敏数据
     */
    private Map desensitized(SceneEnum scene, String fieldName, Map<?, ?> data, Annotation annotation) {
        // TODO: 拷贝一份
        /*return Collections.synchronizedMap(data).entrySet()
                .parallelStream() // 所传进来的map參数不是线程安全的，并行操作时会存在数据一致性问题。因此需要将线程不安全的map转成线程安全的，如Collections.synchronizedMap(data)
                .filter(o -> null != o.getValue())
                .map(e -> e.setValue(this.desensitized(scene, StrUtil.toStringOrNull(e.getKey()), e.getValue(), annotation)));return e;})
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));*/
        Map result = MapUtil.createMap(data.getClass());
        for (Map.Entry e : data.entrySet()) {
            result.put(e.getKey(), this.desensitized(scene, StrUtil.toStringOrNull(e.getKey()), e.getValue(), annotation));
        }
        return result;
    }

    /**
     * 字符类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param data       待脱敏数据
     * @param annotation 待脱敏字段注解脱敏规则
     * @return 已脱敏数据
     */
    private CharSequence desensitized(SceneEnum scene, String fieldName, CharSequence data, Annotation annotation) {
        final String val = Convert.convert(String.class, data);

        if (StringUtil.isTypeJSONArray(val)) {
            try {
                // TODO: 是否是JSON，若是则转JSON处理
                return objectMapper.writeValueAsString(this.desensitized(scene, fieldName, objectMapper.readValue(val, List.class), annotation));
            } catch (JsonProcessingException e) {
                log.debug("【{}】JSON脱敏异常：", val, e);
            }
        } else if (StringUtil.isTypeJSONObject(val)) {
            try {
                // TODO: 是否是JSON，若是则转JSON处理
                return objectMapper.writeValueAsString(this.desensitized(scene, fieldName, objectMapper.readValue(val, Map.class), annotation));
            } catch (JsonProcessingException e) {
                log.debug("【{}】JSON脱敏异常：", val, e);
            }
        } else if (StringUtil.isTypeXml(val)) {
            try {
                // TODO: 是否是XML，若是则转XML处理，需去除<?xml version="1.0" encoding="UTF-8"?>时，则调用getRootElement()
                Map result = this.desensitized(scene, fieldName, xmlMapper.readValue(
                        "<root>" + ReUtil.replaceAll(val, "(\\s*<\\?xml.*\\?>)?", "") + "</root>", Map.class)
                        , annotation);
                String rootNodeName = result.getClass().getSimpleName();
                return StrUtil.strip(xmlMapper.writeValueAsString(result), "<" + rootNodeName + ">", "</" + rootNodeName + ">");
            } catch (JsonProcessingException e) {
                log.debug("【{}】XML脱敏异常：", val, e);
            }
        } else if (StrUtil.isBlank(fieldName)) return data;

        // TODO: 根据注解脱敏规则进行局部且精确脱敏
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
        // TODO: 根据配置中全局脱敏规则进行脱敏
        if (null == desensitizationSource || MapUtil.isEmpty(desensitizationSource.getRules())) return data;
        // 基于全局且按命名方式匹配脱敏
        String field = StringUtil.toCamelCase2(fieldName);
        return DesensitizedUtil.desensitized(scene, desensitizationSource.getRules().get(field), field, val);
    }


    /**
     * todo: 根据脱敏策略验证是否支持脱敏
     *
     * @param context 待脱敏对象所被使用的上下文即在那个类中使用
     * @param data    待脱敏数据
     * @return
     */
    @Override
    public <T> boolean supports(String context, T data) {
        if (null == data) return false;
        // TODO: 是否为简单值类型除字符串外，则不脱敏，其实脱敏针对字符串类型
        if (!(data instanceof CharSequence) && ClassUtil.isSimpleValueType(data.getClass())) return false;

        // TODO: 若全局脱敏策略不为空
        if (null != desensitizationSource && null != desensitizationSource.getStrategy()) {
            // TODO: 若无配置脱敏范围或上下文 context 需要在脱敏范围内，则可脱敏
            DesensitizationStrategy strategy = desensitizationSource.getStrategy();
            if (ArrayUtil.isNotEmpty(strategy.getPackages()) &&
                    !StrUtil.startWithAny(context, strategy.getPackages())) return false;
            // TODO: 验证脱敏有效期内不脱敏
            Date effectDate = strategy.getEffectDate();
            Date expiryDate = strategy.getExpiryDate();
            Date currentDate = new Date();
            // TODO: new Date(System.currentTimeMillis() + 1) 是由于执行过快，时间片一样，此时为false
            return currentDate.before(null == effectDate ? new Date(System.currentTimeMillis() + 1) : effectDate) ||
                    currentDate.after(null == expiryDate ? new Date(System.currentTimeMillis() + 1) : expiryDate);
        }
        return true;
    }
}
