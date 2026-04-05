package com.ouo.mask;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.BeanContext;
import com.alibaba.fastjson2.filter.ContextValueFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ouo.mask.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***********************************************************
 * JSON处理单元测试
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
public class JsonTest {
    private ObjectMapper objectMapper = null;
    private XmlMapper xmlMapper = null;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        xmlMapper = new XmlMapper();
    }

    @Test
    public void jackson() throws Exception {
        // 若开启SerializationFeature.FAIL_ON_EMPTY_BEANS，则无属性的空对象时，会报No serializer found for class java.lang.Object and no rule discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
        log.info("无属性的空对象转JSON字符串：{}", objectMapper.writeValueAsString(new Object()));
        log.info("无属性的空对象转XML字符串：{}", xmlMapper.writeValueAsString(new Object()));

        User user = new User();
        user.setUid(123);
        user.setName("李四");
        user.setAddr(new String[]{"深圳"});
        user.setIdCard(new StrBuilder("2399500"));
        user.setPassowrd("hellp");
        user.setAges(new int[]{12});
        /*mapperFactory.classMap(User.class, User.class)
                .customize(new CustomMapper<User, User>() {
                    @Override
                    public void mapAtoB(User user, User user2, MappingContext context) {
                        super.mapAtoB(user, user2, context);
                    }
                })
                .byDefault()
                .register();*/
        List<Object> list = new ArrayList<>();
        Map map = new HashMap<>();
        map.put("name", "李四");
        map.put("age", 18);

        user.setMap(map);
        user.setUser(user);
        //map.put("user", user);

        list.add(new String[]{"123"});
        list.add(map);
        list.add(user);

        log.info("Map转JSON字符串：{}", objectMapper.writeValueAsString(map));

        log.info("对象转JSON字符串时自动忽略null值字段：{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new User()));
        log.info("对象转XML字符串时自动忽略null值字段：{}", xmlMapper.writerWithDefaultPrettyPrinter().withRootName("student").writeValueAsString(new User()));

        // 若开启DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES，则不存在于目标类中的字段，会报Unrecognized field "adrs" (class com.ouo.mask.vo.User), not marked as ignorable
        log.info("JSON字符串转对象时自动忽略未知字段：{}", objectMapper.readValue("{\"name\":\"李四\", \"adrs\":\"北京\"}", User.class));
        log.info("XML字符串转对象时自动忽略未知字段：{}", xmlMapper.readValue("<User><name>李四</name><adrs>北京</adrs><Class><text><![CDATA[<name>张三丰</name>]]></text></Class></User>",
                Map.class));
    }

    @Test
    public void fastjson() throws Exception {
        User user = new User();
        user.setUid(123);
        user.setName("李四");
        user.setAddr(new String[]{"深圳"});
        user.setIdCard(new StrBuilder("2399500"));
        user.setPassowrd("hellp");
        user.setAges(new int[]{12});
        /*mapperFactory.classMap(User.class, User.class)
                .customize(new CustomMapper<User, User>() {
                    @Override
                    public void mapAtoB(User user, User user2, MappingContext context) {
                        super.mapAtoB(user, user2, context);
                    }
                })
                .byDefault()
                .register();*/
        List<Object> list = new ArrayList<>();
        Map map = new HashMap<>();
        map.put("name", "李四");
        map.put("age", 18);

        //user.setMap(map);
        user.setUser(user);
        //map.put("user", user);
        list.add(user);

        ValueFilter valueFilter = (object, name, value) -> {
            if (null != value && (value instanceof CharSequence || value.getClass().isArray() && CharSequence.class.isAssignableFrom(value.getClass().getComponentType()))) {
                log.info("【{}】字段脱敏", name);
            }
            return value;
        };
        /*JSON.register(String.class, (JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) -> {
            log.info("【{}】字段脱敏", fieldName);
            if (null == object) jsonWriter.writeNull();
            else jsonWriter.writeString(object.toString());
        });*/

        log.info("字符串：{}", JSON.toJSONString(CollUtil.toList("hello", "<root>"),
                (ContextValueFilter) (BeanContext context, Object object, String name, Object value) -> {
                    if (null != value && (value instanceof CharSequence || value.getClass().isArray() && CharSequence.class.isAssignableFrom(value.getClass().getComponentType()))) {
                        log.info("【{}】字段脱敏", name);
                    }
                    return object;

                }));
        // fastjson默认关闭循环引用/自引用，可使用JSONWriter.Feature.ReferenceDetection进行开启，此时会输出"$ref":"$"表示循环引用，而"$ref":".."表示自引用
        log.info("Map转JSON字符串：{}", JSON.toJSONString(user, valueFilter, JSONWriter.Feature.ReferenceDetection, JSONWriter.Feature.PrettyFormat));
    }
}
