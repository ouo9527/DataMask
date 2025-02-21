package com.ouo.mask.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ouo.mask.handler.DefaultDesensitizationHandler;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.spel.SpelTemplateExpressionResolver;
import com.ouo.mask.spel.TemplateExpressionResolver;
import com.ouo.mask.support.log.LogbackContextInitializer;
import com.ouo.mask.support.web.DesensitizationResponseBodyAdvice;
import com.ouo.mask.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/***********************************************************
 * TODO:     数据脱敏配置
 * Author:   刘春
 * Date:     2023/1/17
 ***********************************************************/
@Configuration
@ConditionalOnProperty(prefix = DesensitizationSource.PREFIX, name = "enabled", havingValue = "true")
@Import({SpringUtil.class, DesensitizationResponseBodyAdvice.class})
//@EnableConfigurationProperties({DesensitizationProperties.class})
//@RefreshScope //springcloud刷新@Value注解属性
@Slf4j
public class DesensitizationAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
    public void initialize() {
        new LogbackContextInitializer().initialize();
    }


    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }

    @Bean
    public DesensitizationSource desensitizationProperties() {
        return new DesensitizationSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizationHandler desensitizationHandler(DesensitizationSource source, ObjectMapper objectMapper, XmlMapper xmlMapper) {
        DefaultDesensitizationHandler handler = new DefaultDesensitizationHandler();
        handler.setDesensitizationSource(source);
        handler.setObjectMapper(objectMapper);
        handler.setXmlMapper(xmlMapper);

        return handler;
    }

    @Bean
    @ConditionalOnMissingBean
    public TemplateExpressionResolver templateExpressionResolver() {
        return new SpelTemplateExpressionResolver();
    }
}
