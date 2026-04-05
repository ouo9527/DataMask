package com.ouo.mask.jackson;

import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlTokenStream;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.ouo.mask.semi.SemiStructMapper;
import com.ouo.mask.semi.SemiStructType;
import com.ouo.mask.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/***********************************************************
 * Jackson映射器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Getter
@Setter
public class JacksonObjectMapper implements SemiStructMapper {
    // json处理器
    private final JsonMapper jsonMapper;
    // xml处理器
    private final XmlMapper xmlMapper;

    public JacksonObjectMapper(Optional<JsonMapper> jsonMapperOptional
            , Optional<XmlMapper> xmlMapperOptional) {

        // 当Jackson启用Afterburner时，性能差不多接近Kryo
        AfterburnerModule afterburnerModule = new AfterburnerModule();
        // Json脱敏序列化
        SimpleModule simpleModule = new SimpleModule()
                .setSerializerModifier(new CharSequenceSerializerModifier());
        //.setDeserializerModifier(new CharSequenceDeserializerModifier(this))
        //.addDeserializer(CharSequence.class, new CharSequenceJsonDeserializer());
        // 将&lt;xx>&lt;/xx>转<![CDATA[]]>处理
        /*simpleModule.addSerializer(String.class, new StdSerializer<String>(String.class) {
            @Override
            public void serialize(String serializable, JsonGenerator gen, SerializerProvider provider) throws Exception {
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
        });*/

        /**
         * 1、默认只是开启循环引用检测，即当检测到循环/自引用时抛出
         *    JsonMappingException或InvalidDefinitionException，
         *    从而避免递归导致堆栈溢出，但不会处理循环/自引用。
         * 2、解决循环/自引用方式
         *      1）父子双引用：在父子关系明确的场景下，推荐使用 @JsonManagedReference 和 @JsonBackReference，
         *                   可以完全忽略反向引用端，不输出 $ref
         *      2）复杂对象引用：无论非父子双引用，还是自引用或其他循环引用场景，都可以使用@JsonIdentityInfo生成对象ID，
         *                   基于唯一标识标记对象，当重复引用时仅输出ID即可，从而避免递归导致堆栈溢出。
         *      注：
         *          ‌ID唯一性‌：确保property指向的字段全局唯一
         *          空值安全‌：当对象ID字段若非String，则不可为null，否则报JsonMappingException: (was java.lang.NullPointerException)
         *          集合类型特殊处理：当循环引用发生于集合时，则还需要在集合属性上增加@JsonIdentityReference(alwaysAsId = true)
         */

        this.jsonMapper = (JsonMapper) jsonMapperOptional
                .map(JsonMapper::copy)
                .orElseGet(JsonMapper::new)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                .enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL) // 自引用设置为null，但需要搭配@JsonIdentityInfo注解使用
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID) // 启用引用标识处理(利用对象相等性判断，但非内存地址)，但需要搭配@JsonIdentityInfo注解使用，自动用 "@id" 和 "@ref" 标记重复对象，
                //.enable(SerializationFeature.FAIL_ON_SELF_REFERENCES) // 启用循环引用检测（默认开启，不建议关闭，可能会导致堆栈溢出）
                //.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModules(afterburnerModule, simpleModule);

        this.xmlMapper = (XmlMapper) xmlMapperOptional
                .map(XmlMapper::copy)
                .orElseGet(XmlMapper::new)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                .enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL) // 自引用设置为null，但需要搭配@JsonIdentityInfo注解使用
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID) // 启用引用标识处理(利用对象相等性判断，但非内存地址)，但需要搭配@JsonIdentityInfo注解使用，自动用 "@id" 和 "@ref" 标记重复对象，
                //.enable(SerializationFeature.FAIL_ON_SELF_REFERENCES) // 启用循环引用检测（默认开启，不建议关闭，可能会导致堆栈溢出）
                //.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModules(afterburnerModule, simpleModule);

    }

    /**
     * 从Jackson中XmlMapper获取xml字符串根节点
     *
     * @param xmlParser
     * @return
     */
    public static String getXmlRoot(FromXmlParser xmlParser) {
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
            }
        }
        return null;
    }

    @Override
    public <T> T toBean(String str, Class<T> valueType) {
        if (StrUtil.isTypeJson(str)) {
            try {
                return this.jsonMapper.readValue(str, valueType);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }

        try {
            return this.readValueAs(str, valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString(Object obj, SemiStructType type, String rootName, Properties prop) {
        return this.toString(obj, type, rootName, prop, false);
    }

    @Override
    public String toPrettyString(Object obj, SemiStructType type, String rootName, Properties prop) {
        return this.toString(obj, type, rootName, prop, true);
    }

    /**
     * 将XML字符串转Java Bean
     *
     * @param str       XML字符串
     * @param valueType Bean类型
     * @return 返回Bean实例
     */
    private <T> T readValueAs(String str, Class<T> valueType) throws IOException {
        try (FromXmlParser parser = (FromXmlParser) xmlMapper.createParser(str)) {
            String rootName = getXmlRoot(parser);
            // 包装XML片段
            if (StrUtil.isBlank(rootName)) {
                try {
                    parser.close();
                } catch (IOException e) {
                }

                return readValueAs(StrUtil.wrapXml(str), valueType);
            }

            return parser.readValueAs(valueType);
        }
    }

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj      任意对象
     * @param type     半结构化类型
     * @param rootName 根节点名称（只针对像XML结构才有用）
     * @param prop     上下文属性
     * @param isPretty 是否美化
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    private String toString(Object obj, SemiStructType type, String rootName, Properties prop, boolean isPretty) {
        try {
            ObjectWriter writer = null;
            Object prettyObj = null;
            if (SemiStructType.JSON.equals(type)) {
                prettyObj = obj instanceof String ? this.jsonMapper.readValue((String) obj, Object.class) : obj;
                writer = isPretty ? this.jsonMapper.writerWithDefaultPrettyPrinter() : this.jsonMapper.writer();
            } else if (SemiStructType.XML.equals(type)) {
                prettyObj = obj instanceof String ? this.xmlMapper.readValue((String) obj, Object.class) : obj;
                writer = isPretty ? this.xmlMapper.writerWithDefaultPrettyPrinter() : this.xmlMapper.writer();
            } else prettyObj = obj;

            return null == writer ? StrUtil.toStringOrNull(obj) : writer
                    .withRootName(rootName)
                    .withAttributes(prop)
                    .writeValueAsString(prettyObj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
