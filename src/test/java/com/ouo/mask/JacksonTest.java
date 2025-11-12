package com.ouo.mask;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.ouo.mask.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

@Slf4j
public class JacksonTest {

    // json处理器
    private ObjectMapper objectMapper;
    // xml处理器
    private XmlMapper xmlMapper;

    @BeforeEach
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        AfterburnerModule afterburnerModule = new AfterburnerModule();
        this.objectMapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                //.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModule(afterburnerModule);

        this.xmlMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false) // 序列化时，是否对无属性的空对象抛异常
                .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModule(afterburnerModule);
    }

    @Test
    public void jackson() throws JsonProcessingException {
        // 若开启SerializationFeature.FAIL_ON_EMPTY_BEANS，则无属性的空对象时，会报No serializer found for class java.lang.Object and no rule discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
        log.info("无属性的空对象转JSON字符串：{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Object()));
        log.info("无属性的空对象转XML字符串：{}", xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Object()));

        log.info("对象转JSON字符串时自动忽略null值字段：{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new User()));
        log.info("对象转XML字符串时自动忽略null值字段：{}", xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new User()));

        // 若开启DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES，则不存在于目标类中的字段，会报Unrecognized field "adrs" (class com.ouo.mask.vo.User), not marked as ignorable
        log.info("JSON字符串转对象时自动忽略未知字段：{}", objectMapper.readValue("{\"name\":\"李四\", \"adrs\":\"北京\"}", User.class));
        log.info("XML字符串转对象时自动忽略未知字段：{}", xmlMapper.readValue("<User><name>李四</name><adrs>北京</adrs></User>", User.class));

        log.info("时间对象转JSON字符串：{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Date()));
        log.info("时间对象转XML字符串：{}", xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Date()));
        log.info("JSON字符串转时间对象：{}", objectMapper.readValue("1756738384741", Date.class));
        log.info("XML字符串转时间对象：{}", xmlMapper.readValue("<Date>1756738384746</Date>", Date.class));
    }
}
