package com.ouo.mask.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.*;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.ouo.mask.core.Desensitizer;
import com.ouo.mask.core.enums.SceneEnum;
import lombok.Getter;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/***********************************************************
 * 自定义Kryo（用于脱敏）
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class DesensitizingKryo extends Kryo {
    @Getter
    private final Desensitizer desensitizer; // 脱敏处理器
    @Getter
    private SceneEnum sceneEnum; // 脱敏场景
    @Getter
    private String curFieldName; // 当前字段名称
    @Getter
    private Annotation curAnnotRule; // 当前脱敏注解规则

    public DesensitizingKryo(Desensitizer desensitizer) {
        this.desensitizer = desensitizer;

        this.setReferences(true); // 开启序列化时引用共享（避免死循环）
        this.setCopyReferences(true); // 启用拷贝时引用共享（深拷贝场景）
        this.setRegistrationRequired(false); // 关闭强制注册
        //this.setOptimizedGenerics(true);  // 启用泛型推导即自动推断泛型类型，避免重复写入类信息（需启用优化）（默认开启）
        // 使用 Objenesis 策略（支持非静态内部类和只依赖无参构造函数）
        this.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // kryo.register：自定义序列化器，可以替换DefaultSerializers中定义内置（基本类型+string类型）序列化器，但不能替换UnsafeField中定义内置序列化器
        //this.register(String[].class, new StringSerializer(new DefaultArraySerializers.StringArraySerializer()));
        //this.register(String.class, new StringSerializer(new DefaultSerializers.StringSerializer()));
        //this.register(StringBuilder.class, new StringSerializer(new DefaultSerializers.StringBuilderSerializer()));
        //this.register(StringBuffer.class, new StringSerializer(new DefaultSerializers.StringBufferSerializer()));
        // this.setDefaultSerializer：自定义序列化器，可以替换FieldSerializerFactory中定义内置序列化器，如：替换UnsafeField序列化器
        this.setDefaultSerializer(DesensitizingFieldSerializer.class);
        // kryo.addDefaultSerializer：自定义序列化器，可以替换非register和setDefaultSerializer注册的序列化器，如：StringBuilder、StringBuffer、Map、集合、数组等
        // Map（即映射/字典）类型
        this.addDefaultSerializer(Map.class, new DesensitizingMapSerializer<>(new MapSerializer<>()));
        this.addDefaultSerializer(ConcurrentSkipListMap.class,
                new DesensitizingMapSerializer<>(new DefaultSerializers.ConcurrentSkipListMapSerializer()));
        this.addDefaultSerializer(TreeMap.class,
                new DesensitizingMapSerializer<>(new DefaultSerializers.TreeMapSerializer()));
        this.addDefaultSerializer(Collections.singletonMap(null, null).getClass(),
                new DesensitizingMapSerializer<>(new DefaultSerializers.CollectionsSingletonMapSerializer()));

        // Collection（即集合/列表）类型
        this.addDefaultSerializer(Collection.class, new DesensitizingCollectionSerializer<>(new CollectionSerializer<>()));
        this.addDefaultSerializer(TreeSet.class,
                new DesensitizingCollectionSerializer<>(new DefaultSerializers.TreeSetSerializer()));
        this.addDefaultSerializer(Arrays.asList().getClass(),
                new DesensitizingCollectionSerializer<>(new DefaultSerializers.ArraysAsListSerializer()));
        this.addDefaultSerializer(Collections.singletonList(null).getClass(),
                new DesensitizingCollectionSerializer<>(new DefaultSerializers.CollectionsSingletonListSerializer()));
        this.addDefaultSerializer(Collections.singleton(null).getClass(),
                new DesensitizingCollectionSerializer<>(new DefaultSerializers.CollectionsSingletonSetSerializer()));

        // Array数组类型
        addDefaultSerializer(String[].class, new DesensitizingArraySerializer<>(new DefaultArraySerializers.StringArraySerializer()));
        addDefaultSerializer(CharSequence[].class, new DesensitizingArraySerializer<>(new DefaultArraySerializers.ObjectArraySerializer(this, CharSequence[].class)));
    }

    public DesensitizingKryo sceneEnum(SceneEnum sceneEnum) {
        this.sceneEnum = sceneEnum;
        return this;
    }

    public DesensitizingKryo curFieldName(String fieldName) {
        this.curFieldName = fieldName;
        return this;
    }

    public DesensitizingKryo curAnnotRule(Annotation curAnnotRule) {
        this.curAnnotRule = curAnnotRule;
        return this;
    }
}
