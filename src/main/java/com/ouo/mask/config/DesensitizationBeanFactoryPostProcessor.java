package com.ouo.mask.config;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.core.rule.DesensitizationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.lang.NonNull;

import java.util.Map;

/***********************************************************
 * 针对于Spring（非Spring Boot）脱敏配置加载并解析
 *
 * Author:   ouo
 * Date:     2025/12/7
 ***********************************************************/
@Slf4j
public class DesensitizationBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private Environment environment; // 环境配置

    @Override
    @SuppressWarnings("unchecked")
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 判断DesensitizationProperties bean是否存在，不存在则创建
        String[] beans = beanFactory.getBeanNamesForType(DesensitizationProperties.class);
        if (ArrayUtil.isEmpty(beans)) {
            log.info("【{}】bean未加入到Spring容器，开始读取解析脱敏配置后注入Spring容器！",
                    DesensitizationProperties.class.getName());
            // 解决Spring 中 <context:property-placeholder>、PropertySourcesPlaceholderConfigurer或PropertyPlaceholderConfigurer 未将localProperties或environmentProperties加入到Environment中
            PropertySourcesPlaceholderConfigurer placeholderConfigurer = beanFactory.getBean(
                    PropertySourcesPlaceholderConfigurer.class);
            Dict dict = this.processProperties(placeholderConfigurer.getAppliedPropertySources()); // 脱敏属性配置
            if (environment instanceof ConfigurableEnvironment) {
                dict.putAll(this.processProperties(((ConfigurableEnvironment) environment).getPropertySources()));
            }
            DesensitizationProperties desensitizationProperties = new DesensitizationProperties();
            Object rules = dict.getByPath(DesensitizationProperties.RULES);
            if (rules instanceof Map) desensitizationProperties.setRules((Map<String, ?>) rules);
            beanFactory.registerSingleton(StrUtil.lowerFirst(DesensitizationProperties.class.getSimpleName()),
                    desensitizationProperties);
        }
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        // 优先级必须在PropertySourcesPlaceholderConfigurer或PropertyPlaceholderConfigurer后加载
        return Ordered.LOWEST_PRECEDENCE; // 最低级别
    }

    /**
     * 解析脱敏规则配置
     *
     * @param propertySources 应用属性配属源
     * @return 脱敏规则
     */
    @SuppressWarnings("unchecked")
    private Dict processProperties(PropertySources propertySources) {
        Dict dict = Dict.create(); // 脱敏规则配置
        if (null != propertySources) {
            propertySources.stream()
                    // 1. 过滤：只保留 source 是 Map 类型的 PropertySource
                    .filter(ps -> ps.getSource() instanceof Map)
                    // 2. 映射：将 PropertySource 转换为 Map<String, Object>
                    .map(ps -> (Map<String, Object>) ps.getSource())
                    // 3. 扁平化：将多个 Map 的 entrySet 流合并为一个大的 Entry 流
                    .flatMap(map -> map.entrySet().stream())
                    // 4. 过滤：只保留 key 以指定前缀开头的 entry
                    .filter(entry -> StrUtil.startWith(entry.getKey(),
                            DesensitizationProperties.PREFIX))
                    .forEach(entry -> BeanPath.create(entry.getKey()).set(dict, entry.getValue()));
        }
        return dict;
    }
}
