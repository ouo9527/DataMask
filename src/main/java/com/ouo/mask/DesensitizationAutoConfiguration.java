package com.ouo.mask;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ouo.mask.core.DefaultDesensitizationExecutor;
import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.core.rule.DesensitizationProperties;
import com.ouo.mask.jackson.JacksonObjectMapper;
import com.ouo.mask.semi.SemiStructuredMapper;
import com.ouo.mask.spel.BraceSpelExpressionEvaluator;
import com.ouo.mask.spel.DollarSpelExpressionEvaluator;
import com.ouo.mask.spel.PoundSpelExpressionEvaluator;
import com.ouo.mask.support.web.DesensitizationResponseBodyAdvice;
import com.ouo.mask.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Optional;

/***********************************************************
 * 数据脱敏配置
 *
 * Author:   ouo
 * Date:     2023/1/17
 ***********************************************************/
//@EnableAutoConfiguration // 若@EnableAutoConfiguration + spring.factories同时使用会造成 DesensitizationAutoConfiguration 循环依赖
@AutoConfiguration
@ConditionalOnProperty(prefix = DesensitizationProperties.PREFIX, name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Import({SpringUtil.class, DesensitizationResponseBodyAdvice.class})
//@EnableConfigurationProperties//({DesensitizationProperties.class})
//@RefreshScope //springcloud刷新@Value注解属性
@Slf4j
public class DesensitizationAutoConfiguration {

    @Bean
    @ConfigurationProperties(DesensitizationProperties.PREFIX)
    @ConditionalOnMissingBean
    public DesensitizationProperties desensitizationProperties() {
        return new DesensitizationProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public JacksonObjectMapper semiStructMapper(Optional<JsonMapper> jsonMapperOptional
            , Optional<XmlMapper> xmlMapperOptional) {
        return new JacksonObjectMapper(jsonMapperOptional, xmlMapperOptional);
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizationExecutor desensitizationExecutor(DesensitizationProperties properties
            , SemiStructuredMapper mapper) {
        return new DefaultDesensitizationExecutor(properties, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public BraceSpelExpressionEvaluator braceSpelExpressionEvaluator() {
        return new BraceSpelExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DollarSpelExpressionEvaluator dollarSpelExpressionEvaluator() {
        return new DollarSpelExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public PoundSpelExpressionEvaluator poundSpelExpressionEvaluator() {
        return new PoundSpelExpressionEvaluator();
    }
}
