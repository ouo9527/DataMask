package com.ouo.mask.config;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.enums.ModeEnum;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.rule.*;
import com.ouo.mask.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.*;

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
 */
@Slf4j
public class DesensitizationSource {
    public static final String PREFIX = "ouo.desensitization";
    public static final String RULES = PREFIX + ".rules";
    public static final String STRATEGY = PREFIX + ".strategy";

    @Autowired
    private Environment env;

    /**
     * 获取脱敏策略
     *
     * @return
     */
    public DesensitizationStrategy getStrategy() {
        return this.getDict(DesensitizationSource.STRATEGY).getByPath(DesensitizationSource.STRATEGY, DesensitizationStrategy.class);
    }

    /**
     * 获取脱敏规则
     *
     * @return
     */
    public Map<String, List<DesensitizationRule>> getRules() {
        //todo: 脱敏规则，key：字段，value：规则集
        Map<String, List<DesensitizationRule>> rules = new HashMap<>();
        CollUtil.forEach(this.getDict(DesensitizationSource.RULES).getByPath(DesensitizationSource.RULES, Map.class), (k, v, i) -> {
            final String field = StrUtil.toStringOrNull(k);
            if (v instanceof List) {
                rules.put(StringUtil.toCamelCase2(field), convert(field, (List) v));
            } else if (v instanceof Map) {
                rules.put(StringUtil.toCamelCase2(field), convert(field, CollUtil.newArrayList(((Map) v).values())));
            } else log.debug("{}.{}: This does not comply with the desensitization rules.",
                    DesensitizationSource.RULES, field);
        });

        return rules;
    }

    /**
     * 从Environment对象中获取相应脱敏配置
     *
     * @param prefix
     * @return
     */
    private Dict getDict(String prefix) {
        // 对于Springboot可以采用Binder
        /*if (null != env) {
            this.setRules(Binder.get(env).bind(PREFIX, Bindable.mapOf(Object.class, Object.class))
                    .orElse(new Properties()));
        }*/
        Properties properties = new Properties();
        if (env instanceof ConfigurableEnvironment) {
            for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) { //获取所有配置文件的属性
                /*if (null == propertySource || !LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME.equals(propertySource.getName()))
                    continue;*/
                if (propertySource instanceof EnumerablePropertySource) {
                    for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
                        if (key.startsWith(prefix)) {
                            properties.put(key, propertySource.getProperty(key));
                        }
                    }
                }
            }
        }
        //todo：具体层级的Map
        Dict dict = Dict.create();
        CollUtil.forEach(properties, (k, v, i) -> {
            BeanPath.create(StrUtil.toStringOrNull(k)).set(dict, v);
        });

        return dict;
    }

    private List<DesensitizationRule> convert(String field, List<?> rules) {
        final List<DesensitizationRule> drs = new ArrayList<>();
        CollUtil.forEach(rules, ((r, i) -> {
            if (r instanceof Map) {
                String mode = MapUtil.getStr((Map) r, "mode", "");
                String scene = MapUtil.getStr((Map) r, "scene", "");

                if (StrUtil.isNotBlank(mode)) ((Map) r).put("mode", mode.toUpperCase());
                if (StrUtil.isNotBlank(scene)) ((Map) r).put("scene", scene.toUpperCase());
                DesensitizationRule dr = null;
                if (StrUtil.equalsIgnoreCase(mode, ModeEnum.EMPTY.name())) {//todo：置空
                    dr = BeanUtil.toBean(r, EmptyDesensitizationRule.class);
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.HASH.name())) {//todo：哈希
                    String algorithm = MapUtil.getStr((Map) r, "algorithm", "");
                    if (StrUtil.isNotBlank(algorithm)) ((Map) r).put("algorithm", algorithm.toUpperCase());
                    dr = BeanUtil.toBean(r, HashDesensitizationRule.class);
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REGEX.name())) {//todo：正则
                    dr = BeanUtil.toBean(r, RegexDesensitizationRule.class);
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REPL.name())) {//todo：替换
                    Object posns = ((Map) r).get("posns");
                    //todo：对于springboot yml转properties时，若多层数组嵌套时，会被转成LinkedHashMap
                    if (posns instanceof Map) ((Map) r).put("posns", CollUtil.newArrayList(((Map) posns).values()));
                    dr = BeanUtil.toBean(r, ReplDesensitizationRule.class);
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.MASK.name())) {//todo：掩盖
                    String type = MapUtil.getStr((Map) r, "type", "");
                    if (StrUtil.isNotBlank(type)) ((Map) r).put("type", type.toUpperCase());
                    dr = BeanUtil.toBean(r, MaskDesensitizationRule.class);
                } else {
                    log.debug("{}.{}[{}]: mode={} is not within the range of [empty,hash,regex,replace,mask]",
                            DesensitizationSource.RULES, field, i, mode);
                }

                if (null != dr) {
                    dr.setScene(null == dr.getScene() ? SceneEnum.ALL : dr.getScene());
                    dr.setField(field);
                    drs.add(dr);
                }
            }
        }));

        return drs;
    }
}
