package com.ouo.mask.jackson;

import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlTokenStream;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.ouo.mask.semi.SemiStructType;
import com.ouo.mask.semi.SemiStructuredMapper;
import com.ouo.mask.util.StrUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/***********************************************************
 * Jackson序列化/反序列化映射器（用于处理JSON/XMl半结构化字符串）
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class JacksonObjectMapper implements SemiStructuredMapper {
    // json处理器
    private final JsonMapper jsonMapper;
    // xml处理器
    private final XmlMapper xmlMapper;

    public JacksonObjectMapper() {
        this(Optional.empty(), Optional.empty());
    }

    public JacksonObjectMapper(Optional<JsonMapper> jsonMapperOptional, Optional<XmlMapper> xmlMapperOptional) {
        // 当Jackson启用Afterburner时，性能差不多接近Kryo
        //AfterburnerModule afterburnerModule = new AfterburnerModule();
        BlackbirdModule blackbirdModule = new BlackbirdModule();
        // Json脱敏序列化（注解@JsonSerialize优先于modifySerializer，因此需要重写changeProperties方法）
        SimpleModule simpleModule = new SimpleModule()
                .setSerializerModifier(new JacksonBeanSerializerModifier());
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
         *
         * 3、反序例化配置属性：
         *    1）FAIL_ON_NULL_FOR_PRIMITIVES：基本类型收到null时是否报错，默认不报错
         *    2）FAIL_ON_NUMBERS_FOR_ENUMS：用数字反序列化为枚举（不是枚举名）时是否报错，默认允许数字匹配枚举 ordinal
         *    3）FAIL_ON_INVALID_SUBTYPE：多态类型（@JsonTypeInfo）找不到子类型时报错，默认报错
         *    4）FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY：外部类型 ID（即@JsonTypeInfo(use=Id.EXTERNAL_PROPERTY）缺失时报错，默认报错
         *    5）FAIL_ON_IGNORED_PROPERTIES：被 @JsonIgnore 的字段如果出现在 JSON 中，是否报错，默认不报错
         *    6）FAIL_ON_MISSING_CREATOR_PROPERTIES：构造方法/@JsonCreator参数缺失时（即JSON 没有该字段）是否报错，默认不报错
         *    7）FAIL_ON_READING_DUP_TREE_KEY：JSON 出现重复 key 时是否报错，默认不报错
         *    8）FAIL_ON_UNKNOWN_PROPERTIES：未知字段即不存在于目标类中时是否报错，默认报错
         *    9）FAIL_ON_TRAILING_TOKENS：解析完根对象后，JSON 末尾还有多余字符/垃圾数据时报错，默认不报错。若true则必须恰好一个完整 JSON，多余则报错
         *    10）FAIL_ON_NULL_CREATOR_PROPERTIES：构造方法 / @JsonCreator 工厂方法的参数，显式 null 注入到 creator 参数时是否报错，默认不报错
         *    11）FAIL_ON_UNRESOLVED_OBJECT_IDS：处理 @JsonIdentityInfo 对象 ID 引用时JSON 里出现一个对象 ID，但找不到对应对象（未定义、丢失、循环引用断链）是否报错，默认报错
         *
         * 4、序例化配置属性：
         *    1）FAIL_ON_EMPTY_BEANS：对象无任何可序列化字段时报错，默认报错。若为false则输出空对象 {}
         *    2）FAIL_ON_SELF_REFERENCES：对象存在循环自引用时报错，默认报错。若为false允许自引用（会无限递归，可能会导致堆栈溢出，慎用）
         *    3）FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS：多态类型 ID 与 @JsonUnwrapped 冲突时报错，默认报错
         *
         */

        this.jsonMapper = (JsonMapper) jsonMapperOptional
                .map(JsonMapper::copy)
                .orElseGet(JsonMapper::new)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL) // 自引用设置为null，但需要搭配@JsonIdentityInfo注解使用
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID) // 启用引用标识处理(利用对象相等性判断，但非内存地址)，但需要搭配@JsonIdentityInfo注解使用，自动用 "@id" 和 "@ref" 标记重复对象，
                //.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModules(blackbirdModule, simpleModule);

        this.xmlMapper = (XmlMapper) xmlMapperOptional
                .map(XmlMapper::copy)
                .orElseGet(XmlMapper::new)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL) // 自引用设置为null，但需要搭配@JsonIdentityInfo注解使用
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID) // 启用引用标识处理(利用对象相等性判断，但非内存地址)，但需要搭配@JsonIdentityInfo注解使用，自动用 "@id" 和 "@ref" 标记重复对象，
                //.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModules(blackbirdModule, simpleModule);

    }

    /**
     * 从Jackson中XmlMapper获取xml字符串根节点
     *
     * @param xmlParser XML解析器
     * @return 返回XML字符串
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
            } catch (Exception ignore) {
                // 获取不到根节点
            }
        }
        return null;
    }

    @Override
    public <T> T toBean(String str, Class<T> valueType) {
        try {
            if (StrUtil.isTypeJson(str)) {
                return this.jsonMapper.readValue(str, valueType);
            }
            if (StrUtil.isTypeXml(str)) {
                return this.readValueAs(str, valueType);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("Unstructured data other than JSON and XML is not supported for conversion to Java Bean at this time.");
    }


    @Override
    public String toString(Object obj, SemiStructType type, String rootName) {
        return this.toString(obj, type, rootName, false);
    }

    @Override
    public String toPrettyString(Object obj, SemiStructType type, String rootName) {
        return this.toString(obj, type, rootName, true);
    }

    @Override
    public <T> T getMapper(SemiStructType type) {
        return (T) (SemiStructType.JSON.equals(type) ? this.jsonMapper : SemiStructType.XML.equals(type)
                ? this.xmlMapper : null);
    }

    /**
     * 将XML字符串转Java Bean
     *
     * @param str       XML字符串
     * @param valueType Bean类型
     * @return 返回Bean实例
     */
    private <T> T readValueAs(String str, Class<T> valueType) throws IOException {
        String wrapXml = Map.class.isAssignableFrom(valueType) || JsonNode.class.isAssignableFrom(valueType) ||
                Object.class.equals(valueType) ? StrUtil.wrapXml(str) : str;
        try (FromXmlParser parser = (FromXmlParser) this.xmlMapper.createParser(wrapXml)) {
            String rootName = getXmlRoot(parser);
            // 包装XML片段
            if (StrUtil.isBlank(rootName)) {
                return this.readValueAs(StrUtil.wrapXml(wrapXml), valueType);
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
     * @param isPretty 是否美化
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    private String toString(Object obj, SemiStructType type, String rootName, boolean isPretty) {
        try {
            if (SemiStructType.JSON.equals(type) || SemiStructType.XML.equals(type)) {
                ObjectMapper objectMapper = SemiStructType.JSON.equals(type) ? this.jsonMapper : this.xmlMapper;
                String val = StrUtil.strip((isPretty ? objectMapper.writerWithDefaultPrettyPrinter() : objectMapper.writer())
                                .withRootName(rootName)
                                .writeValueAsString(obj instanceof String ? objectMapper.readValue((String) obj, Object.class) : obj),
                        System.lineSeparator());


                return isPretty ? System.lineSeparator() + StrUtil.strip(val, System.lineSeparator()) : val;
            }

            return StrUtil.toStringOrNull(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
