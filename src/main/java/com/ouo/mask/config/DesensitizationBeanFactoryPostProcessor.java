package com.ouo.mask.config;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.rule.DesensitizationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Map;

@Slf4j
public class DesensitizationBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 判断DesensitizationProperties bean是否存在，不存在则创建
        String[] beans = beanFactory.getBeanNamesForType(DesensitizationProperties.class);
        if (ArrayUtil.isEmpty(beans)) {
            log.info("【{}】bean未加入到Spring容器，开始读取解析脱敏配置后注入Spring容器！", DesensitizationProperties.class.getName());
            Environment environment = beanFactory.getBean(Environment.class);
            if (environment instanceof ConfigurableEnvironment) {
                Dict dict = Dict.create();  // 脱敏属性配置
                ((ConfigurableEnvironment) environment).getPropertySources()
                        .stream()
                        .forEach(propertySource -> {
                            Object source = propertySource.getSource();
                            if (source instanceof Map) {
                                ((Map<String, Object>) source).entrySet()
                                        .stream()
                                        .forEach(entry -> {
                                            if (StrUtil.startWith(entry.getKey(), DesensitizationProperties.PREFIX)) {
                                                BeanPath.create(entry.getKey()).set(dict, entry.getValue());
                                            }
                                        });
                            }
                        });
                DesensitizationProperties desensitizationProperties = new DesensitizationProperties();
                desensitizationProperties.setStrategy(dict.getByPath(DesensitizationProperties.STRATEGY, DesensitizationStrategy.class));
                Object rules = dict.getByPath(DesensitizationProperties.RULES);
                if (rules instanceof Map) desensitizationProperties.setRules((Map<String, ?>) rules);
                beanFactory.registerSingleton(StrUtil.toCamelCase(DesensitizationProperties.class.getSimpleName()), desensitizationProperties);
            }
        }
    }
}
