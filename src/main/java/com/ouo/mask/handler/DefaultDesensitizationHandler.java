package com.ouo.mask.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.*;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlTokenStream;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.ouo.mask.DesensitizationProperties;
import com.ouo.mask.annotation.*;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.kryo.DesensitizationFieldSerializerFactory;
import com.ouo.mask.properties.DesensitizationStrategy;
import com.ouo.mask.util.DesensitizedUtil;
import com.ouo.mask.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.w3c.dom.Document;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/***********************************************************
 * 数据脱敏处理器
 * Author:   刘春
 * Date:     2022/12/3
 ***********************************************************/
@Slf4j
public class DefaultDesensitizationHandler implements DesensitizationHandler {

    private final static String DEFAULT_ROOT_NAME = "_"; // 默认根节点
    private final static String DEFAULT_START_ROOT_NODE = "<" + DEFAULT_ROOT_NAME + ">"; // 默认开始根节点
    private final static String DEFAULT_END_ROOT_NODE = "</" + DEFAULT_ROOT_NAME + ">"; // 默认结束根节点
    // 对象处理
    private final ThreadLocal<Kryo> kryoThreadLocal;
    // json处理器
    private ObjectMapper objectMapper;
    // xml处理器
    private XmlMapper xmlMapper;
    // 全局脱敏规则
    private DesensitizationProperties desensitizationProperties;

    public DefaultDesensitizationHandler(DesensitizationProperties desensitizationProperties) {
        /**
         * ToXmlGenerator.Feature.UNWRAP_ROOT_OBJECT_NODE：用于序列化（对象转 XML）时控制是否生成根节点。默认认启用
         * DeserializationFeature.UNWRAP_ROOT_VALUE：通常用于反序列化为POJO，而不是JsonNode树模型或Map类型。默认是禁用
         *
         * Jackson默认是使用get/set方法进行序列化/反序列化或反射、不支持基础类型包装/解包装
         */
        // setSerializationInclusion(JsonInclude.Include.NON_NULL)：全局忽略null的属性序列化
        // enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)：序列化BigDecimal时不使用科学计数法输出

        this.kryoThreadLocal = ThreadLocal.withInitial(() -> {
            // Kryo（线程不安全）：copy方法支持transient修饰属性拷贝（但序列化不支持），不支持非静态内部类和只依赖无参构造函数，可以使用objenesis框架的StdInstantiatorStrategy策略解决
            Kryo kryo = new Kryo();
            kryo.setReferences(true); // 开启序列化时引用共享（避免死循环）
            kryo.setCopyReferences(true); // 启用拷贝时引用共享（深拷贝场景）
            kryo.setRegistrationRequired(false); // 关闭强制注册
            //kryo.setOptimizedGenerics(true);  // 启用泛型推导即自动推断泛型类型，避免重复写入类信息（需启用优化）（默认开启）
            // 使用 Objenesis 策略（支持非静态内部类和只依赖无参构造函数）
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

            // kryo.register：自定义序列化器，可以替换DefaultSerializers中定义内置（基本类型+string类型）序列化器，但不能替换UnsafeField中定义内置序列化器
            // kryo.setDefaultSerializer：自定义序列化器，可以替换FieldSerializerFactory中定义内置序列化器，如：替换UnsafeField序列化器
            // kryo.addDefaultSerializer：自定义序列化器，可以替换非register和setDefaultSerializer注册的序列化器，如：StringBuilder、StringBuffer、Map、集合、数组等
            return kryo;
        });

        // 当Jackson启用Afterburner时，性能差不多接近Kryo
        this.objectMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        /*SimpleModule simpleModule = new SimpleModule();
        // 将&lt;xx>&lt;/xx>转<![CDATA[]]>处理
        simpleModule.addSerializer(String.class, new StdSerializer<String>(String.class) {
            @Override
            public void serialize(String serializable, JsonGenerator gen, SerializerProvider provider) throws IOException {
                // gen.currentValue() 当前序列化对象
                JsonStreamContext context = gen.getOutputContext();
                // 当前字段名称
                String fieldName = context.getCurrentName();
                // 父级对象
                Object parent = context.getParent().getCurrentValue();
                // 当前字段所属对象
                Object obj = parent;
                if (parent instanceof Map) {
                    obj = ((Map) parent).get(context.getParent().getCurrentName());
                }

                if (StringUtil.isTypeXml(serializable)) {
                    gen.writeRawValue("<![CDATA[" + serializable + "]]>");
                } else {
                    gen.writeString(serializable);
                }
            }
        });
        this.xmlMapper.registerModule(simpleModule);*/
        AfterburnerModule afterburnerModule = new AfterburnerModule();
        this.objectMapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModule(afterburnerModule);

        this.xmlMapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModule(afterburnerModule);
        this.desensitizationProperties = desensitizationProperties;
    }

    /**
     * 从Jackson中XmlMapper获取xml字符串根节点
     *
     * @param xmlParser
     * @return
     */
    private static String getXmlRoot(FromXmlParser xmlParser) {
        if (null == xmlParser.currentToken()) {
            // 尝试获取下个Token
            try {
                if (xmlParser.nextToken() == JsonToken.START_OBJECT) {
                    // 反射获取
                    XmlTokenStream _xmlTokens = (XmlTokenStream) ReflectUtil.getFieldValue(xmlParser, "_xmlTokens");
                    return _xmlTokens.getLocalName();
                }
            } catch (Exception e) {
                // 获取不到根节点
                log.debug("获取根节点异常：", e);
            }
        }
        log.debug("Missing name, in state: {}", xmlParser.currentToken());
        return null;
    }

    @Override
    public <T> T desensitized(SceneEnum scene, Field field, T data) {
        // 静态字段属于类属性即类成员共享，若修改后会造成共享不一致问题，其次常量字段即编译时常量，若修改后会造成不可见问题即通过get方法访问和直接访问字段，其值是不一样的，故脱敏都不建议修改
        if (null == field || null == data || ModifierUtil.isStatic(field)
                || ModifierUtil.hasModifier(field, ModifierUtil.ModifierType.FINAL)) return data;
        return this.desensitized(scene, field.getName(), data, Arrays.stream(field.getAnnotations())
                .filter(a -> a instanceof Empty || a instanceof Hash || a instanceof Regex
                        || a instanceof Repl || a instanceof Mask)
                .findAny().orElse(null));
    }

    /**
     * 根据脱敏策略验证是否支持脱敏
     *
     * @param context 待脱敏对象所被使用的上下文即在那个类中使用
     * @return
     */
    @Override
    public boolean supports(String context) {
        // 若全局脱敏策略不为空
        if (null != desensitizationProperties && null != desensitizationProperties.getStrategy()) {
            // 若无配置脱敏范围或上下文 context 需要在脱敏范围内，则可脱敏
            DesensitizationStrategy strategy = desensitizationProperties.getStrategy();
            if (ArrayUtil.isNotEmpty(strategy.getPackages()) &&
                    !StrUtil.startWithAny(context, strategy.getPackages())) return false;
            // 验证脱敏有效期内不脱敏
            Date effectDate = strategy.getEffectDate();
            Date expiryDate = strategy.getExpiryDate();
            Date currentDate = new Date();
            // new Date(System.currentTimeMillis() + 1) 是由于执行过快，时间片一样，此时为false
            return currentDate.before(null == effectDate ? new Date(System.currentTimeMillis() + 1) : effectDate) ||
                    currentDate.after(null == expiryDate ? new Date(System.currentTimeMillis() + 1) : expiryDate);
        }
        return true;
    }

    @Override
    public <T> T desensitized(SceneEnum scene, String fieldName, T data) {
        return this.desensitized(scene, fieldName, data, null);
    }

    /**
     * key-data任意类型脱敏
     *
     * @param scene         脱敏场景
     * @param fieldName     待脱敏字段
     * @param data          待脱敏数据
     * @param annotation    注解式脱敏规则
     * @return 已脱敏数据
     */
    private <T> T desensitized(SceneEnum scene, String fieldName, T data, Annotation annotation) {
        // 字符类型进行脱敏
        if ((data instanceof CharSequence)) {
            if (data instanceof String) {
                return (T) this.desensitized(scene, fieldName, (String) data, annotation);
            }
            // CharSequence的具体类型是否具备String类型参数的构造方法，若具备则可重新创建原对象
            Constructor<?> constructor = ReflectUtil.getConstructor(data.getClass(), String.class);
            if (null != constructor) {
                try {
                    constructor.setAccessible(true);
                    return (T) constructor.newInstance(this.desensitized(scene, fieldName, Convert.convert(String.class, data), annotation));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    log.debug("String转{}异常：", constructor.getName(), e);
                }
            }
            return data;
        }
        // Jackson 数组类型循环递归处理
        if (data instanceof ArrayNode) {
            return (T) this.desensitized(scene, fieldName, (ArrayNode) data, annotation);
        }
        // 数组类型循环递归处理
        if (ArrayUtil.isArray(data)) {
            // 基础类型的数组不能强转为Object类型数组，而是要转成相应的包装类型即使用
            //Arrays.asList()：基于原数组实现，不支持添加或删除元素，且与原数组共享数据，修改会互相影响
            //return (T) Convert.convert(data.getClass(), this.desensitized(scene, fieldName, CollUtil.newArrayList(data), annotation));
            return (T) Convert.convert(data.getClass(), this.desensitized(scene, fieldName, ArrayUtil.wrap(data), annotation));
        }
        // 集合类型循环递归处理
        if (data instanceof Collection) {
            return (T) this.desensitized(scene, fieldName, (Collection<T>) data, annotation);
        }
        // 迭代器类型循环递归处理
        if (data instanceof Iterator) {
            return (T) this.desensitized(scene, fieldName, (Iterator<T>) data, annotation);
        }

        return this.desensitized(data, scene);
    }

    /**
     * key-val字符类型脱敏
     *
     * @param scene         脱敏场景
     * @param fieldName     待脱敏字段
     * @param val           待脱敏数据
     * @param annotation    注解式脱敏规则
     * @return 已脱敏数据
     */
    private String desensitized(SceneEnum scene, String fieldName, String val, Annotation annotation) {
        if (StrUtil.isBlank(val)) return val;
        boolean isNext = false; // 是否往下
        try {
            JsonNode jsonNode = objectMapper.readTree(val);
            if (jsonNode.isArray() || jsonNode.isObject()) {
                Object data = this.desensitized(scene, fieldName, jsonNode, annotation);
                if (SceneEnum.LOG.equals(scene)) {
                    return System.lineSeparator() + objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(data);
                }
                return objectMapper.writeValueAsString(data);
            }
        } catch (IOException e) {
            //log.debug("【{}】JSON脱敏异常：", val, e);
            isNext = true;
        }

        if (isNext) {
            try (FromXmlParser parser = (FromXmlParser) xmlMapper.createParser(val)) {
                String rootName = getXmlRoot(parser);
                // 包装XML片段
                if (StrUtil.isBlank(rootName)) {
                    try {
                        parser.close();
                    } catch (IOException e) {
                        //log.debug("【{}】XML片段流关闭异常：", val, e);
                    }

                    String v = wrapXml(val);
                    return desensitized(scene, fieldName, v, annotation);
                }

                Object result = this.desensitized(scene, fieldName, (JsonNode) parser.readValueAsTree(), annotation); //parser.readValueAs(Map.class)

                // 移除XML片段包装(不换行)
                if (DEFAULT_ROOT_NAME.equals(rootName)) {
                    return StrUtil.strip(xmlMapper.writer().withRootName(rootName).writeValueAsString(result),
                            DEFAULT_START_ROOT_NODE, DEFAULT_END_ROOT_NODE);

                }
                if (SceneEnum.LOG.equals(scene)) {
                    return System.lineSeparator() + xmlMapper.writer().withDefaultPrettyPrinter()
                            .withRootName(rootName)
                            .writeValueAsString(result);
                }
                return xmlMapper.writer().withRootName(rootName).writeValueAsString(result);
            } catch (IOException e) {
                //log.debug("【{}】XML脱敏异常：", val, e);
            }
        }

        if (StrUtil.isBlank(fieldName)) return val;

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
        if (null == desensitizationProperties || MapUtil.isEmpty(desensitizationProperties.getRules())) return val;
        // 基于全局且按命名方式匹配脱敏
        String field = StringUtil.toCamelCase2(fieldName);
        return DesensitizedUtil.desensitized(scene, desensitizationProperties.getRules().get(field), field, val);
    }

    /**
     * 迭代器类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene         脱敏场景
     * @param fieldName     待脱敏字段
     * @param data          待脱敏数据
     * @param annotation    注解式脱敏规则
     * @return 已脱敏数据
     */
    private <T> Iterator<T> desensitized(SceneEnum scene, String fieldName, Iterator<T> data, Annotation annotation) {
        // 拷贝一份
        final List<T> results = new ArrayList<>();
        while (data.hasNext()) {
            results.add(this.desensitized(scene, fieldName, data.next(), annotation));
        }
        return results.iterator();
    }

    /**
     * 集合类型脱敏处理：在data数据中查找fieldName字段值
     *
     * @param scene     脱敏场景
     * @param fieldName 待脱敏字段
     * @param data      待脱敏数据
     * @return 已脱敏数据
     */
    private <T> Collection<T> desensitized(SceneEnum scene, String fieldName, Collection<T> data, Annotation annotation) {
        // 拷贝一份
        /*return (Collection) Collections.synchronizedCollection(data)
                .parallelStream() // 所传进来的map參数不是线程安全的，并行操作时会存在数据一致性问题。因此需要将线程不安全的map转成线程安全的，如Collections.synchronizedCollection(data)等
                .map(o -> this.desensitized(scene, fieldName, o, annotation))
                .collect(data instanceof Set ? Collectors.toSet() : Collectors.toList());*/
        Collection<T> results = CollUtil.create(data.getClass());
        for (T o : data) {
            results.add(this.desensitized(scene, fieldName, o, annotation));
        }
        return results;
    }

    /**
     * 数组类型脱敏处理
     *
     * @param scene         脱敏场景
     * @param fieldName     待脱敏字段
     * @param data          待脱敏数据
     * @param annotation    注解式脱敏规则
     * @return 已脱敏数据
     */
    private <T> T[] desensitized(SceneEnum scene, String fieldName, T[] data, Annotation annotation) {
        // 拷贝一份
        // 1万条内使用for处理；1万～10万条之间使用stream流式处理；10万～100万条时，采用parallel多线程并行处理
        /*return new CopyOnWriteArrayList(data)
                .parallelStream() // 所传进来的map參数不是线程安全的，并行操作时会存在数据一致性问题。因此需要将线程不安全的map转成线程安全的，如Collections.synchronizedList(data)或new CopyOnWriteArrayList(data)等
                .map(o -> this.desensitized(scene, fieldName, o, annotation))
                .toArray();*/
        //ArrayUtil.newArray(data.getClass(), data.length); 不可直接使用
        T[] results = (T[]) new Object[data.length];//Arrays.copyOf(data, data.length);
        for (int i = 0; i < data.length; i++) {
            results[i] = this.desensitized(scene, fieldName, data[i], annotation);
        }
        return results; //(T) ArrayUtil.cast(data.getClass(), vals);
    }

    /**
     * 映射类型脱敏处理
     *
     * @param scene 脱敏场景
     * @param data  待脱敏数据
     * @return 已脱敏数据
     */
    private Map desensitized(SceneEnum scene, Map<?, ?> data) {
        // 拷贝一份
        /*return Collections.synchronizedMap(data).entrySet()
                .parallelStream() // 所传进来的map參数不是线程安全的，并行操作时会存在数据一致性问题。因此需要将线程不安全的map转成线程安全的，如Collections.synchronizedMap(data)
                .filter(o -> null != o.getValue())
                .map(e -> e.setValue(this.desensitized(scene, StrUtil.toStringOrNull(e.getKey()), e.getValue(), annotation)));return e;})
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));*/
        Map result = MapUtil.createMap(data.getClass());
        for (Map.Entry e : data.entrySet()) {
            if (e.getKey() instanceof CharSequence)
                result.put(e.getKey(), this.desensitized(scene, StrUtil.toStringOrNull(e.getKey()), e.getValue()));
            else result.put(e.getKey(), e.getValue());
        }
        return result;
    }

    /**
     * Jackson 数组类型脱敏处理
     *
     * @param scene      脱敏场景
     * @param fieldName  待脱敏字段
     * @param nodes      待脱敏数据
     * @param annotation 注解式脱敏规则
     * @return 已脱敏数据
     */
    private ArrayNode desensitized(SceneEnum scene, String fieldName, ArrayNode nodes, Annotation annotation) {
        ArrayNode results = new ArrayNode(objectMapper.getNodeFactory(), nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode jsonNode = nodes.get(i);
            if (jsonNode instanceof TextNode) {
                results.add(new TextNode(this.desensitized(scene, fieldName, jsonNode.asText(), annotation)));
            } else results.add(this.desensitized(scene, fieldName, jsonNode, annotation));
        }
        return results;
    }

    /**
     * Jackson 数组类型脱敏处理
     *
     * @param scene 脱敏场景
     * @param node  待脱敏数据
     * @return 已脱敏数据
     */
    private ObjectNode desensitized(SceneEnum scene, ObjectNode node) {
        ObjectNode result = new ObjectNode(objectMapper.getNodeFactory());
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (null != entry) {
                JsonNode jsonNode = entry.getValue();
                if (jsonNode instanceof TextNode) {
                    result.set(entry.getKey(), new TextNode(this.desensitized(scene, entry.getKey(), jsonNode.asText())));
                } else result.set(entry.getKey(), this.desensitized(scene, entry.getKey(), jsonNode));
            }
        }
        return result;
    }

    /**
     * 包装XML或XML片段
     *
     * @param xml
     * @return
     */
    private String wrapXml(String xml) {
        return new StrBuilder()
                .append(DEFAULT_START_ROOT_NODE)
                .append(ReUtil.replaceAll(xml, "(\\s*<\\?xml.*\\?>)?", ""))
                .append(DEFAULT_END_ROOT_NODE)
                .toString();
    }

    /**
     * 其他对象类型脱敏处理
     *
     * @param scene 脱敏场景
     * @param data  待脱敏数据
     * @return 已脱敏数据
     */
    private <T> T desensitized(T data, SceneEnum scene) {
        // 简单值类型(除字符类型即String、other CharSequenc)不脱敏，包含原始类型、Number、Date、URI、URL、Locale、Class
        if (null == data || ClassUtil.isSimpleValueType(data.getClass())) {
            return data;
        }
        // 映射类型循环递归处理
        if (data instanceof Map) {
            return (T) this.desensitized(scene, (Map) data);
        }
        // Jackson Map类型循环递归处理
        if (data instanceof ObjectNode) {
            return (T) this.desensitized(scene, (ObjectNode) data);
        }
        // XML格式处理
        if (data instanceof Document) {
            try {
                return (T) xmlMapper.readValue(this.desensitized(scene, null, xmlMapper.writeValueAsString(data), null), data.getClass());
            } catch (JsonProcessingException e) {
                // 无法转xml
            }
        }
        try {
            Kryo kryo = kryoThreadLocal.get();
            // 对象序列化
            kryo.setDefaultSerializer(new DesensitizationFieldSerializerFactory(scene, this));
            return kryo.copy(data);
        } finally {
            kryoThreadLocal.remove();
        }
    }
}
