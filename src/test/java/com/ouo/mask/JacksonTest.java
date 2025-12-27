package com.ouo.mask;

import com.ouo.mask.util.JacksonUtil;
import com.ouo.mask.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

@Slf4j
public class JacksonTest {
    @BeforeEach
    public void init() {

    }

    @Test
    public void jackson() throws Exception {
        // 若开启SerializationFeature.FAIL_ON_EMPTY_BEANS，则无属性的空对象时，会报No serializer found for class java.lang.Object and no rule discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
        log.info("无属性的空对象转JSON字符串：{}", JacksonUtil.toJsonString(new Object()));
        log.info("无属性的空对象转XML字符串：{}", JacksonUtil.toXmlString(new Object()));

        log.info("对象转JSON字符串时自动忽略null值字段：{}", JacksonUtil.toJsonStringWithDefaultPrettyPrinter(new User()));
        log.info("对象转XML字符串时自动忽略null值字段：{}", JacksonUtil.toXmlStringWithDefaultPrettyPrinter(new User(), "student"));

        // 若开启DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES，则不存在于目标类中的字段，会报Unrecognized field "adrs" (class com.ouo.mask.vo.User), not marked as ignorable
        log.info("JSON字符串转对象时自动忽略未知字段：{}", JacksonUtil.toBean("{\"name\":\"李四\", \"adrs\":\"北京\"}", User.class));
        log.info("XML字符串转对象时自动忽略未知字段：{}", JacksonUtil.toBean("<User><name>李四</name><adrs>北京</adrs><Class><text><![CDATA[<name>张三丰</name>]]></text></Class></User>",
                Map.class));

        log.info("时间对象转JSON字符串：{}", JacksonUtil.toJsonStringWithDefaultPrettyPrinter(new Date()));
        log.info("时间对象转XML字符串：{}", JacksonUtil.toXmlStringWithDefaultPrettyPrinter(new Date()));
        log.info("JSON字符串转时间对象：{}", JacksonUtil.toBean("1756738384741", Date.class));
        log.info("XML字符串转时间对象：{}", JacksonUtil.toBean("<Date>1756738384746</Date>", Date.class));
    }
}
