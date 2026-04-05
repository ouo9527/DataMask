package com.ouo.mask;

import cn.hutool.core.text.StrBuilder;
import com.ouo.mask.orika.CustomMapperConvert;
import com.ouo.mask.orika.DesensitizationConverter;
import com.ouo.mask.vo.User;
import javassist.*;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultCodeGenerationStrategy;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.impl.generator.CodeGenerationStrategy;
import ma.glasnost.orika.impl.generator.specification.Convert;
import ma.glasnost.orika.metadata.TypeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***********************************************************
 * Orika高性能映射单元测试
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
public class OrikaTest {
    private MapperFactory mapperFactory;
    private Class<?> clzz;

    @BeforeEach
    public void init() throws CannotCompileException, NotFoundException {
        System.setProperty("orika.bytecode.providers.default", "no-cache");
        ClassPool pool = ClassPool.getDefault();
        pool.clearImportedPackages();

        CtClass typeCtClass = pool.get("ma.glasnost.orika.metadata.Type");
        // 检查并移除 final 修饰符
        if (Modifier.isFinal(typeCtClass.getModifiers())) {
            // 清除 ACC_FINAL 标志位（0x0010）
            typeCtClass.setModifiers(typeCtClass.getModifiers() & ~Modifier.FINAL);
        }
        // 修改构造器（如：包级私有(package-private)）访问权限
        for (CtConstructor ctor : typeCtClass.getDeclaredConstructors()) {
            if (!Modifier.isPublic(ctor.getModifiers())) {
                // 清除 ACC_PRIVATE、ACC_PROTECTED‌、ACC_TRANSIENT 标志
                ctor.setModifiers(Modifier.PUBLIC); // 将 0x00000002、0x00000004、0x00000080 → 0x00000001
            }
        }

        DefaultCodeGenerationStrategy codeGenerationStrategy = new DefaultCodeGenerationStrategy();
        codeGenerationStrategy.addSpecification(new CustomMapperConvert(), CodeGenerationStrategy.Position.IN_PLACE_OF, Convert.class);

        DefaultMapperFactory.Builder builder = new DefaultMapperFactory.Builder();
        mapperFactory = builder
                .mapNulls(false) // 忽略null值
                .codeGenerationStrategy(codeGenerationStrategy)
                //.unenhanceStrategy()
                //.propertyResolverStrategy(new DesensitizationPropertyResolver())
                //.mappingContextFactory(new CustomMappingContext.Factory())
                //.converterFactory(converterFactory)
                .useAutoMapping(true)
                .build();

        // 定制映射行为‌，允许开发者在不修改源代码的情况下，为映射过程添加额外的规则或提示
        /*mapperFactory.registerMappingHint((fromProperty, fromPropertyType) -> {
            return fromProperty;
        });*/

        // 注册自定义转换器(只能对值操作)，当设置customize方法时，优先于转换器
        mapperFactory.getConverterFactory().registerConverter(new DesensitizationConverter());

        // 注册自定义映射（可选）
        /*mapperFactory.classMap(User.class, Map.class)
                // 字段映射
                .fieldMap("email", "emailAddress").add()
                // 排除无需映射字段（如敏感字段）
                //.exclude()
                // 其余字段按默认方式映射
                .byDefault()
                .register();*/
    }

    @Test
    public void mapping() {
        User user = new User();
        user.setName("李四");
        user.setAddr(new String[]{"深圳"});
        user.setIdCard(new StrBuilder("2399500"));
        user.setPassowrd("hellp");
        user.setAges(new int[]{12});
        /*mapperFactory.classMap(User.class, User.class)
                .customize(new CustomMapper<User, User>() {
                    @Override
                    public void mapAtoB(User user, User user2, MappingContext context) {
                        super.mapAtoB(user, user2, context);
                    }
                })
                .byDefault()
                .register();*/
        List<Object> list = new ArrayList<>();
        Map map = new HashMap<>();
        map.put("name", "李四");
        map.put("age", 18);

        user.setUser(user);
        user.setMap(map);
        map.put("user", user);
        map.put("map", map);

        list.add(map);
        list.add(user);

        //mapperFactory.classMap(Map.class, Map.class).byDefault().register();
        Map toUser = mapperFactory.getMapperFacade()
                .mapAsMap(map, new TypeBuilder<Map<Object, Object>>() {
                        }.build()
                        , new TypeBuilder<Map<Object, Object>>() {
                        }.build());
        /*User toUser = mapperFactory.getMapperFacade()
                .map(user, User.class);*/
        log.info("列表映射结果：{}", toUser);

        List toList = mapperFactory.getMapperFacade().mapAsList(list, Object.class);
        log.info("集合映射结果：{}", toList);
    }
}
