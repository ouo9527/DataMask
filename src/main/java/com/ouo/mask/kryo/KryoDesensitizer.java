package com.ouo.mask.kryo;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.util.ClassUtil;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.*;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.core.des.Desensitizer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.util.Iterator;

/***********************************************************
 * 基于Kryo脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class KryoDesensitizer<T> extends Kryo implements Desensitizer<T> {
    // 脱敏执行器
    private final DesensitizationExecutor executor;

    public KryoDesensitizer(DesensitizationExecutor executor) {
        this.executor = executor;
        // FST（线程安全）
        //FSTConfiguration fstConfiguration = FSTConfiguration.createDefaultConfiguration();
        //fstConfiguration.setShareReferences(false); // 关闭对象引用共享，减少元数据开销
        //fstConfiguration.setForceSerializable(true); // 允许未实现Serializable or externalizable
        //fstConfiguration.setInstantiator(new FSTDefaultClassInstantiator()); // 避免反射创建实例
        //fstConfiguration.registerSerializer(Object.class, new FSTBasicObjectSerializer());

        // Kryo（线程不安全）：copy方法支持transient修饰属性拷贝（但序列化不支持），不支持非静态内部类和只依赖无参构造函数，可以使用objenesis框架的StdInstantiatorStrategy策略解决
        this.setReferences(true); // 开启序列化时引用共享（避免死循环）
        this.setCopyReferences(true); // 启用拷贝时引用共享（深拷贝场景）
        this.setRegistrationRequired(false); // 关闭强制注册
        //this.setOptimizedGenerics(true);  // 启用泛型推导即自动推断泛型类型，避免重复写入类信息（需启用优化）（默认开启）
        // 使用 Objenesis 策略（支持非静态内部类和只依赖无参构造函数）
        this.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        // this.register：自定义序列化器，可以替换DefaultSerializers中定义内置（基本类型+string类型）序列化器，但不能替换UnsafeField中定义内置序列化器
        //this.register(String[].class, new StringSerializer(new DefaultArraySerializers.StringArraySerializer()));

        // this.setDefaultSerializer：自定义序列化器，可以替换FieldSerializerFactory中定义内置序列化器，如：替换UnsafeField序列化器
        //this.setDefaultSerializer(DesensitizingFieldSerializer.class);

        // this.addDefaultSerializer：自定义序列化器，可以替换非register和setDefaultSerializer注册的序列化器，如：StringBuilder、StringBuffer、Map、集合、数组等
        // Map（即映射/字典）类型
        //this.addDefaultSerializer(Map.class, new MapSerializer<>());
        // Collection（即集合/列表）类型
        //this.addDefaultSerializer(Collection.class, new CollectionSerializer<>());
        // Array数组类型
        //this.addDefaultSerializer(CharSequence[].class, new DefaultArraySerializers.ObjectArraySerializer(kryo, CharSequence[].class));
    }

    @Override
    public DesensitizationExecutor getDesensitizationExecutor() {
        return this.executor;
    }

    @Override
    public T desensitize(T data, DesensitizationContext context) {
        // Iterator一次性迭代器类型（游标/指针迭代）
        boolean isIterator = false;
        Object obj = null;
        if (data instanceof CharSequence) {
            obj = this.executor.desensitize(data, context);
        } else if (data instanceof Iterator) {
            isIterator = true;
            obj = IterUtil.toList((Iterator<?>) data);
        } else obj = data;

        Serializer serializer = this.getSerializer(ClassUtil.getClass(obj), context); //  this.getSerializer(ClassUtil.getClass(obj))
        Object cp = this.copy(obj, serializer);

        return (T) (isIterator ? IterUtil.getIter(cp) : cp);
    }

    /**
     * Returns the serializer for the registration for the specified class.
     *
     * @see #getRegistration(Class)
     * @see Registration#getSerializer()
     */
    private Serializer getSerializer(Class type, DesensitizationContext context) {
        Serializer delegate = null;
        if (null != type) {
            delegate = getRegistration(type).getSerializer();
            if (delegate instanceof FieldSerializer) {
                return new KryoFieldSerializer<T>(delegate, context);
            } else if (delegate instanceof DefaultArraySerializers.StringArraySerializer
                    || delegate instanceof DefaultArraySerializers.ObjectArraySerializer) {
                return new KryoArraySerializer<T>(delegate, context);
            } else if (delegate instanceof CollectionSerializer
                    || delegate instanceof DefaultSerializers.CollectionsSingletonListSerializer
                    || delegate instanceof DefaultSerializers.CollectionsSingletonSetSerializer) {
                return new KryoCollectionSerializer<>(delegate, context);
            } else if (delegate instanceof MapSerializer
                    || delegate instanceof DefaultSerializers.CollectionsSingletonMapSerializer) {
                return new KryoMapSerializer<>(delegate, context);
            }

        }
        return delegate;
    }
}
