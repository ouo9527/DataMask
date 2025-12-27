package com.ouo.mask;

import com.ouo.mask.config.DesensitizationProperties;
import com.ouo.mask.handler.DefaultDesensitizationHandler;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.spel.BraceSpelExpressionResolver;
import com.ouo.mask.spel.DollarSpelExpressionResolver;
import com.ouo.mask.spel.PoundSpelExpressionResolver;
import com.ouo.mask.support.web.DesensitizationResponseBodyAdvice;
import com.ouo.mask.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/***********************************************************
 * 数据脱敏配置
 * Author:   刘春
 * Date:     2023/1/17
 ***********************************************************/
//@EnableAutoConfiguration // 若@EnableAutoConfiguration + spring.factories同时使用会造成 DesensitizationAutoConfiguration 循环依赖
@AutoConfiguration
@ConditionalOnProperty(prefix = DesensitizationProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({SpringUtil.class, DesensitizationResponseBodyAdvice.class})
//@EnableConfigurationProperties({DesensitizationProperties.class})
//@RefreshScope //springcloud刷新@Value注解属性
@Slf4j
public class DesensitizationAutoConfiguration {

    /*@Bean
    @ConditionalOnMissingBean
    public DesensitizationBeanFactoryPostProcessor desensitizationBeanFactoryPostProcessor() {
        return new DesensitizationBeanFactoryPostProcessor();
    }*/

    @Bean
    @ConfigurationProperties(DesensitizationProperties.PREFIX)
    @ConditionalOnMissingBean
    public DesensitizationProperties desensitizationProperties() {
        return new DesensitizationProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizationHandler desensitizationHandler() {
        return new DefaultDesensitizationHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public BraceSpelExpressionResolver braceSpelExpressionResolver() {
        return new BraceSpelExpressionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public DollarSpelExpressionResolver dollarSpelExpressionResolver() {
        return new DollarSpelExpressionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public PoundSpelExpressionResolver poundSpelExpressionResolver() {
        return new PoundSpelExpressionResolver();
    }
}
