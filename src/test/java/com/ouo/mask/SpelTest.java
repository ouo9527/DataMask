package com.ouo.mask;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.ouo.mask.spel.BraceSpelExpressionResolver;
import com.ouo.mask.spel.ExpressionResolver;
import com.ouo.mask.spel.SpelExpressionMetaData;
import com.ouo.mask.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * SpEL表达式解析测试
 */
@Slf4j
public class SpelTest {

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
    public void spel() throws JsonProcessingException {
        User user = new User();
        user.setName("李四");
        user.setAddr(new String[]{"深圳"});
        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("addr", "英雄联盟");
        SpelExpressionMetaData metaData = new SpelExpressionMetaData(user);
        ExpressionResolver expressionResolver = new BraceSpelExpressionResolver();
        log.info("SpEL普通表达式1(通过元数据)：{}", expressionResolver.exe("{#p0.name}", metaData));
        log.info("SpEL普通表达式2(通过元数据)：{}", expressionResolver.exe("{args[0].addr}", metaData));
        log.info("SpEL普通表达式3(通过元数据)：{}", expressionResolver.exe("{name}", user));
        log.info("SpEL普通表达式4(通过元数据)：{}", expressionResolver.exe("{#a0.name}", new User[]{user}));
        log.info("SpEL普通表达式5(通过非元数据)：{}", expressionResolver.exe("{#addr}", map));
        log.info("解析SpEL花括号模板表达式1：{}", expressionResolver.exe("亲爱的【{#root.name}】您好，欢迎来到【{addr}】!", user, String.class));
        log.info("解析SpEL花括号模板表达式2：{}", expressionResolver.exe("亲爱的【{[name]}】您好，欢迎来到【{[addr]}】!", map, String.class));
        log.info("解析SpEL花括号模板表达式3：{}", expressionResolver.exe("亲爱的【{#root}】您好，欢迎来到【{[0]}】!",
                objectMapper.readTree("{\"name\":\"张三\",\"addr\":\"深圳\"}"),
                String.class/*, ((expression, result, e) -> BeanUtil.getProperty(context.getRootObject(), expression.getExpressionString()))*/));
        /*SpelExpressionParser parser = new SpelExpressionParser();

        EvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .withRootObject(metaData)
                .build();*/

    }
}
