package com.ouo.mask.config;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/***********************************************************
 * 脱敏规则加载（用于Spring环境，而Springboot环境采用{@link DesensitizationAutoConfiguration}自动加载）
 * Author:   刘春
 * Date:     2023/1/18
 ***********************************************************/
@Slf4j
public class SpringPropertySourcesPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {
    @Setter
    private Environment environment;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);
        // 把localProperties合并到 Environment
        if (this.environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment env = (ConfigurableEnvironment) this.environment;
            env.getPropertySources().addLast(super.getAppliedPropertySources().get(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME));
        }
    }
}
