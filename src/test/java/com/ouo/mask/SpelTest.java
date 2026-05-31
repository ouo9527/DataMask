package com.ouo.mask;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.ouo.mask.spel.BraceSpelExpressionEvaluator;
import com.ouo.mask.spel.SpelEvaluationContext;
import com.ouo.mask.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;

import java.util.HashMap;
import java.util.Map;

/***********************************************************
 * SpEL表达式单元测试
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
class SpelTest {
    // json处理器
    private ObjectMapper objectMapper;
    // xml处理器
    private XmlMapper xmlMapper;

    @BeforeEach
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
        //AfterburnerModule afterburnerModule = new AfterburnerModule();
        BlackbirdModule blackbirdModule = new BlackbirdModule();
        this.objectMapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 序列化时，是否对无属性的空对象抛异常
                //.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModules(blackbirdModule);

        this.xmlMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 反序列化时，自动忽略未知字段即不存在于目标类中的字段
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false) // 序列化时，是否对无属性的空对象抛异常
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL) // 序列化时，自动忽略 null 值字段
                .registerModules(blackbirdModule);
    }

    @Test
    void spel() throws JsonProcessingException {
        User user = new User();
        user.setName("李四");
        user.setAddr(new String[]{"深圳"});
        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("addr", "英雄联盟");

        BraceSpelExpressionEvaluator evaluator = new BraceSpelExpressionEvaluator();
        EvaluationContext evalContext = SpelEvaluationContext.builder().args(user).build();
        EvaluationContext evalContext2 = SpelEvaluationContext.builder().args(map).build();

        // Java Bean类型使用.引用
        log.info("Java Bean类型SpEL普通表达式1(通过非元数据)：{}", evaluator.exe("#a0.name", evalContext));
        log.info("Java Bean类型SpEL普通表达式2(通过元数据即根对象)：{}", evaluator.exe("args[0].addr", evalContext)); // 等价于#root.args[0]
        log.info("Java Bean类型SpEL占位符表达式1(通过元数据即根对象)：{}", evaluator.exe("{args[0].addr}", evalContext)); // 等价于#root.args[0]
        log.info("Java Bean类型SpEL占位符表达式2(通过元数据即根对象)：{}", evaluator.exe("{#root.args[0].name}", evalContext));
        log.info("Java Bean类型SpEL占位符表达式3(通过非元数据)：{}", evaluator.exe("{#a0.name}", evalContext));
        log.info("Java Bean类型SpEL占位符表达式4(通过非元数据)：{}", evaluator.exe("{#a0}", evalContext));
        log.info("Java Bean类型SpEL占位符表达式5(通过非元数据)：{}", evaluator.exe("{#p0.name}", evalContext));

        // Collection集合/Array数组/Map字典类型使用[]引用
        log.info("Collection集合/Array数组/Map字典类型SpEL普通表达式1(通过非元数据)：{}", evaluator.exe("#a0[name]", evalContext2));
        log.info("Collection集合/Array数组/Map字典类型SpEL普通表达式2(通过元数据即根对象)：{}", evaluator.exe("args[0][addr]", evalContext2)); // 等价于#root.args[0]
        log.info("Collection集合/Array数组/Map字典类型SpEL占位符表达式1(通过元数据即根对象)：{}", evaluator.exe("{args[0][addr]}", evalContext2));
        log.info("Collection集合/Array数组/Map字典类型SpEL占位符表达式2(通过非元数据)：{}", evaluator.exe("{#a0[name]}", evalContext2));
        // Jackson对象无法简单使用普通或占位符方式（如：[name]）获取，可通过回调函数使用Hutool#BeanUtil（支持可使用$）处理（如：args[0]._children[name]）
        log.info("SpEL占位符表达式1：{}", evaluator.exe("{args[0][_children].name}来到【{$args[0]._children[addr]}】!", String.class,
                SpelEvaluationContext.builder().args(objectMapper.readTree("{\"name\": \"张三\",\"addr\":\"深圳\"}")).build(),
                ((expression, context, result, e) -> StrUtil.toStringOrNull(BeanUtil.getProperty(context.getRootObject().getValue(),
                        expression.getExpressionString())))));

    }
}
