package com.ouo.mask.util;

import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlTokenStream;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JacksonUtil {
    // json处理器
    private static ObjectMapper objectMapper;
    // xml处理器
    private static XmlMapper xmlMapper;

    static {
        // 当Jackson启用Afterburner时，性能差不多接近Kryo
        objectMapper = new ObjectMapper();
        xmlMapper = new XmlMapper();
        /*SimpleModule simpleModule = new SimpleModule();
        // 将&lt;xx>&lt;/xx>转<![CDATA[]]>处理
        simpleModule.addSerializer(String.class, new StdSerializer<String>(String.class) {
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
        });
        this.xmlMapper.registerModule(simpleModule);*/
        AfterburnerModule afterburnerModule = new AfterburnerModule();
        objectMapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModule(afterburnerModule);

        xmlMapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModule(afterburnerModule);
    }

    /**
     * 将对象转格式化JSON字符串
     *
     * @param obj 任意对象
     * @return
     * @throws JsonProcessingException
     */
    public static String toJsonStringWithDefaultPrettyPrinter(Object obj) throws Exception {
        String val = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        return StrUtil.removeSuffix(StrUtil.startWith(val, System.lineSeparator())
                ? val : System.lineSeparator() + val, System.lineSeparator());
    }

    /**
     * 将对象转JSON字符串
     *
     * @param obj 任意对象
     * @return
     * @throws JsonProcessingException
     */
    public static String toJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 将对象转格式化XML字符串
     *
     * @param obj 任意对象
     * @return
     * @throws JsonProcessingException
     */
    public static String toXmlStringWithDefaultPrettyPrinter(Object obj) throws Exception {
        return toXmlStringWithDefaultPrettyPrinter(obj, null);
    }

    /**
     * 将对象转格式化XML字符串
     *
     * @param obj      任意对象
     * @param rootName 自定义XML字符串根节点
     * @return
     * @throws JsonProcessingException
     */
    public static String toXmlStringWithDefaultPrettyPrinter(Object obj, String rootName) throws Exception {
        String val = xmlMapper.writer()
                .withDefaultPrettyPrinter()
                .withRootName(rootName)
                .writeValueAsString(obj);
        return StrUtil.removeSuffix(StrUtil.startWith(val, System.lineSeparator())
                ? val : System.lineSeparator() + val, System.lineSeparator());
    }

    /**
     * 将对象转XML字符串
     *
     * @param obj 任意对象
     * @return
     * @throws JsonProcessingException
     */
    public static String toXmlString(Object obj) throws Exception {
        return xmlMapper.writer()
                .writeValueAsString(obj);
    }

    /**
     * 将对象转XML字符串
     *
     * @param obj      任意对象
     * @param rootName 自定义XML字符串根节点
     * @return
     * @throws JsonProcessingException
     */
    public static String toXmlString(Object obj, String rootName) throws Exception {
        return xmlMapper.writer()
                .withRootName(rootName)
                .writeValueAsString(obj);
    }

    /**
     * 将json或xml字符串转JsonNode
     *
     * @param str json或xml字符串
     * @return JsonNode
     */
    public static JsonNode readTree(String str) throws Exception {
        try (FromXmlParser parser = (FromXmlParser) xmlMapper.createParser(str)) {
            String rootName = getXmlRoot(parser);
            // 包装XML片段
            if (StrUtil.isBlank(rootName)) {
                try {
                    parser.close();
                } catch (IOException e) {
                }

                String v = StrUtil.wrapXml(str);
                return readTree(v);
            }

            return parser.readValueAsTree();
        } catch (Exception e) {
        }
        return objectMapper.readTree(str);
    }

    /**
     * 将json或xml字符串转Java Bean
     *
     * @param content json或xml字符串
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T toBean(String content, Class<T> clazz) throws Exception {
        JsonNode jsonNode = xmlToJsonNode(readTree(content));
        // Jackson Map类型循环递归处理
        try {
            return xmlMapper.convertValue(jsonNode, clazz);
        } catch (Exception e) {
        }
        return objectMapper.convertValue(jsonNode, clazz);
    }

    /**
     * 创建JOSN解析器
     *
     * @param content json或xml字符串
     * @return
     */
    public static JsonParser createParser(String content) throws Exception {
        try {
            return xmlMapper.createParser(content);
        } catch (IOException e) {

        }
        return objectMapper.createParser(content);
    }

    /**
     * 获取JSON JsonNodeFactory
     *
     * @return
     */
    public static JsonNodeFactory getJsonNodeFactory() {
        return objectMapper.getNodeFactory();
    }

    /**
     * 获取XML JsonNodeFactory
     *
     * @return
     */
    public static JsonNodeFactory getXmlNodeFactory() {
        return xmlMapper.getNodeFactory();
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

    /**
     * 将XML字符串转JsonNode
     *
     * @param jsonNode
     * @return
     */
    private static JsonNode xmlToJsonNode(JsonNode jsonNode) {
        if (jsonNode instanceof ObjectNode) {
            ObjectNode result = new ObjectNode(JacksonUtil.getJsonNodeFactory());
            Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                if (null != entry) {
                    JsonNode node = entry.getValue();
                    if (node instanceof TextNode) {
                        try {
                            node = readTree(node.asText());
                        } catch (Exception e) {
                        }
                    } else node = xmlToJsonNode(node);
                    result.set(entry.getKey(), node);
                }
            }
            return result;
        } else if (jsonNode instanceof ArrayNode) {
            ArrayNode results = new ArrayNode(JacksonUtil.getJsonNodeFactory(), jsonNode.size());
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode node = jsonNode.get(i);
                if (node instanceof TextNode) {
                    try {
                        node = readTree(node.asText());
                    } catch (Exception e) {
                    }
                } else node = xmlToJsonNode(node);
                results.add(node);
            }
            return results;
        }
        return jsonNode;
    }
}
