package com.ouo.mask.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.annotation.Hash;
import com.ouo.mask.enums.ModeEnum;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.enums.SensitiveTypeEnum;
import com.ouo.mask.rule.*;
import com.ouo.mask.util.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Value失效场景： 1、PropertySourcesPlaceholderConfigurer类及其子类使用，由于配置还未加载并解析
 * 2、使用static或者final修饰
 * 3、用该注解的类没有被spring管理
 * 4、在Bean初始化时构造方法中引用被@Value修饰的变量
 * @ConfigurationProperties失效场景： 1、所修饰的类，其类中属性不存在非静态set方法或者不存在无参构造方法
 * 2、若使用@Bean+@ConfigurationProperties自定义bean或（@Component 或者 @Configuration等）bean注解+@ConfigurationProperties，
 * 则需要使用EnableAutoConfiguration修饰在入口类，如配置类、启动类上等
 * 3、若只使用@ConfigurationProperties，则需要使用@EnableConfigurationProperties修饰在入口类，如启配置类、动类上等
 * <p>
 * 注意：1）在spring-boot 2.2 之前版本,必须使用 @Component 或者 @Configuration 声明成Spring Bean；
 * 而2.2.0 新增一个 @ConfigurationPropertiesScan 的注解，默认是开启的扫描 main 启动类所在的包路径的所有ConfigurationProperties
 * 或使用@EnableConfigurationProperties注解加载相应配置类，所以可以不用再加 @Component 或者 @Configuration ；
 * Spring boot 2.2.1 默认关闭此功能，需要显式指定此注解，实际在使用过程中会发现 @Profile 和这个注解的兼容问题,
 * @ConfigurationPropertiesScan not compatible with @Profile @ConfigurationProperties，所以 Spring Boot 2.2.1 默认关闭了这个功能；
 * <p>
 * 2）@ConfigurationProperties虽可以处理复杂的数据类型，且会将复杂数据类型（无论数组或列表）转成LinkedHashMap，然后再做实际映射处理，但是对于properties和yml文件
 * 中复杂类型做映射处理时，前者无法映射成数组，而后者可以
 * 3）在spring-boot2.0以下，@ConfigurationProperties映射对象原理由PropertiesConfigurationFactory；2.0后由ConfigurationPropertiesBindingPostProcessor
 * 4）脱敏规则或策略不影响局部的注解脱敏
 */
@Slf4j
@Setter
@Getter
public class DesensitizationProperties {
    public static final String PREFIX = "ouo.desens";
    public static final String RULES = PREFIX + ".rules";
    public static final String STRATEGY = PREFIX + ".strategy";

    // 是否启用脱敏
    private boolean enabled;
    // 脱敏策略
    private DesensitizationStrategy strategy;
    // 脱敏规则
    private Map<String, DesensitizationRule> rules;

    public void setRules(Map<String, ?> rules) {
        if (CollUtil.isEmpty(rules)) return;
        this.rules = new HashMap<>();
        for (Map.Entry<String, ?> entry : rules.entrySet()) {
            if (entry.getValue() instanceof Map) {
                this.rules.put(StringUtil.toCamelCase2(entry.getKey()), this.convert(entry.getKey(), (Map) entry.getValue()));
            }
        }
    }

    /*@Autowired
    private Environment env;*/

    /**
     * 获取脱敏策略
     *
     * @return
     */
    /*public DesensitizationStrategy getStrategy() {
        return this.getDict(DesensitizationProperties.STRATEGY).getByPath(DesensitizationProperties.STRATEGY, DesensitizationStrategy.class);
    }*/

    /**
     * 获取脱敏规则
     *
     * @return
     */
    /*public Map<String, List<DesensitizationRule>> getRules() {
        //脱敏规则，key：字段，value：规则集
        Map<String, List<DesensitizationRule>> rules = new HashMap<>();
        CollUtil.forEach(this.getDict(DesensitizationProperties.RULES).getByPath(DesensitizationProperties.RULES, Map.class), (k, v, i) -> {
            final String field = StrUtil.toStringOrNull(k);
            if (v instanceof List) {
                rules.put(StringUtil.toCamelCase2(field), convert(field, (List) v));
            } else if (v instanceof Map) {
                rules.put(StringUtil.toCamelCase2(field), convert(field, CollUtil.newArrayList(((Map) v).values())));
            } else log.debug("{}.{}: This does not comply with the desensitization rules.",
                    DesensitizationProperties.RULES, field);
        });

        return rules;
    }*/

    /**
     * 从Environment对象中获取相应脱敏配置
     *
     * @param prefix
     * @return
     */
    /*private Dict getDict(String prefix) {
        // 对于Springboot可以采用Binder
        *//*if (null != env) {
            this.setRules(Binder.get(env).bind(PREFIX, Bindable.mapOf(Object.class, Object.class))
                    .orElse(new Properties()));
        }*//*
        Properties properties = new Properties();
        if (env instanceof ConfigurableEnvironment) {
            for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) { //获取所有配置文件的属性
                *//*if (null == propertySource || !LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME.equals(propertySource.getName()))
                    continue;*//*
                if (propertySource instanceof EnumerablePropertySource) {
                    for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
                        if (key.startsWith(prefix)) {
                            properties.put(key, propertySource.getProperty(key));
                        }
                    }
                }
            }
        }
        //具体层级的Map
        Dict dict = Dict.create();
        CollUtil.forEach(properties, (k, v, i) -> {
            BeanPath.create(StrUtil.toStringOrNull(k)).set(dict, v);
        });

        return dict;
    }*/

    /**
     * 将字段脱敏规则转具体脱敏规则类
     *
     * @param field 待脱敏字段
     * @param rule  字段脱敏规则
     * @return
     */
    private DesensitizationRule convert(String field, Map rule) {
        DesensitizationRule dr = null;
        String mode = MapUtil.getStr(rule, "mode", "");
        String scene = MapUtil.getStr(rule, "scene", "");

        if (StrUtil.equalsIgnoreCase(mode, ModeEnum.EMPTY.name())) { // 置空
            dr = new EmptyDesensitizationRule();
        } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.HASH.name())) { // 哈希
            dr = new HashDesensitizationRule();
            ((HashDesensitizationRule) dr).setAlgorithm(Convert.convert(Hash.AlgorithmEnum.class,
                    StrUtil.toUpperCase(MapUtil.getStr(rule, "algorithm", "")), Hash.AlgorithmEnum.SM3));
            ((HashDesensitizationRule) dr).setSalt(MapUtil.getStr(rule, "salt", ""));
        } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REGEX.name())) { // 正则
            dr = new RegexDesensitizationRule();
            ((RegexDesensitizationRule) dr).setPattern(MapUtil.getStr(rule, "pattern", ""));
            ((RegexDesensitizationRule) dr).setRv(MapUtil.getStr(rule, "rv", ""));

        } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REPL.name())) { // 替换
            dr = new ReplDesensitizationRule();
            ((ReplDesensitizationRule) dr).setSurplus(MapUtil.get(rule, "surplus", ReplDesensitizationRule.Posn.class, null));
            //对于springboot yml转properties时，若多层数组嵌套时，会被转成LinkedHashMap
            List<?> posns = MapUtil.get(rule, "posns", List.class, null);
            if (CollUtil.isNotEmpty(posns)) {
                List<ReplDesensitizationRule.Posn> posnList = new ArrayList<>(posns.size());
                for (Object posn : posns) {
                    posnList.add(BeanUtil.toBean(posn, ReplDesensitizationRule.Posn.class));
                }
                ((ReplDesensitizationRule) dr).setPosns(posnList);
            }
        } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.MASK.name())) { // 掩盖
            dr = new MaskDesensitizationRule();
            ((MaskDesensitizationRule) dr).setType(Convert.convert(SensitiveTypeEnum.class,
                    StrUtil.toUpperCase(MapUtil.getStr(rule, "type", "")), null));
            ((MaskDesensitizationRule) dr).setShow(MapUtil.get(rule, "show", MaskDesensitizationRule.CustomShow.class, null));
        } else {
            log.debug("{}.{}.mode: mode={} is not within the range of [empty,hash,regex,replace,mask]",
                    DesensitizationProperties.RULES, field, mode);
        }

        if (null != dr) {
            if (StrUtil.isNotBlank(scene)) {
                dr.setScene(Convert.convert(SceneEnum.class, StrUtil.toUpperCase(scene), SceneEnum.ALL));
            }
            dr.setField(field);
        }

        return dr;
    }
}
