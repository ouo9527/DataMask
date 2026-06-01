package com.ouo.mask.jackson;

import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.core.DesensitizeException;
import com.ouo.mask.core.des.SemiStructuredDesensitizer;
import com.ouo.mask.semi.SemiStructType;
import com.ouo.mask.semi.SemiStructuredMapper;
import com.ouo.mask.util.StrUtil;
import org.w3c.dom.Node;

import java.io.IOException;

/***********************************************************
 * 基于Jackson半结构化脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class JacksonDesensitizer<T> extends SemiStructuredDesensitizer<T> {

    public JacksonDesensitizer(DesensitizationExecutor executor, SemiStructuredMapper mapper) {
        super(executor, mapper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T desensitize(T data, DesensitizationContext context) {
        try {
            if (data instanceof String) {
                if (StrUtil.isTypeJson((CharSequence) data)) {
                    JsonMapper jsonMapper = this.mapper.getMapper(SemiStructType.JSON);
                    return (T) this.toString(jsonMapper.readValue((String) data, Object.class), jsonMapper, context);
                } else if (StrUtil.isTypeXml((CharSequence) data)) {
                    return (T) this.transform((String) data, this.mapper.getMapper(SemiStructType.XML), context);
                } else throw new DesensitizeException("Only JSON and XML data are supported for desensitization; " +
                        "other structured data types are not supported.");
            }
            return (T) this.mapper.toBean(this.toString(data, data instanceof Node
                    ? this.mapper.getMapper(SemiStructType.XML) : this.mapper.getMapper(
                    SemiStructType.JSON), context), data.getClass());
        } catch (IOException e) {
            throw new DesensitizeException("JSON and XML string desensitization failed: ", e);
        }
    }

    /**
     * 转换XML字符串
     *
     * @param xml     XML字符串
     * @param context 脱敏上下文
     * @return 返回转换后XML字符串
     */
    private String transform(String xml, XmlMapper xmlMapper, DesensitizationContext context) throws IOException {
        String wrapXml = StrUtil.wrapXml(xml);
        try (FromXmlParser parser = (FromXmlParser) xmlMapper.createParser(wrapXml)) {
            // 获取xml字符串根节点xmlParser.getParsingContext().inObject()
            String rootName = JacksonObjectMapper.getXmlRoot(parser);
            // 包装XML片段
            if (StrUtil.isBlank(rootName)) {
                return this.transform(StrUtil.wrapXml(wrapXml), xmlMapper, context);
            }

            String val = this.toString(parser.readValueAsTree(), xmlMapper, DesensitizationContext
                    .builder(context)
                    .fieldName(rootName)
                    .build());

            // 移除XML片段包装(不换行)
            if (StrUtil.DEFAULT_ROOT_NAME.equals(rootName)) {
                return StrUtil.strip(val, StrUtil.DEFAULT_START_ROOT_NODE,
                        StrUtil.DEFAULT_END_ROOT_NODE);
            }

            return val;
        }
    }

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj          任意对象
     * @param objectMapper 半结构化映射器
     * @param context      脱敏上下文
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    private String toString(Object obj, ObjectMapper objectMapper, DesensitizationContext context)
            throws JsonProcessingException {
        if (null == objectMapper) return StrUtil.toStringOrNull(obj);

        return objectMapper
                .writer()
                .withAttributes(MapUtil.builder()
                        .put(DesensitizationExecutor.class, this.executor)
                        .put(DesensitizationContext.class, context)
                        .build())
                .withRootName(objectMapper instanceof XmlMapper ? context.getFieldName() : null)
                .writeValueAsString(obj);
    }
}
