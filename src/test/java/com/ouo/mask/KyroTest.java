package com.ouo.mask;

import cn.hutool.core.text.StrBuilder;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.ouo.mask.vo.User;
import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KyroTest {

    /**
     * 高性能深拷贝
     */
    @Test
    public void deeCopy() throws IOException {
        User user = new User("");
        user.setName("张王四");
        user.setExtra("{\"phone\":17722657194}");
        user.setTel("0987-2322");
        user.setPhone("17722657194");
        user.setMobile("17722657194");
        user.setAddr(new String[]{"北京市朝阳区发和小区1号楼2单元303室", "广东省深圳市福田区1单元"});
        user.setAmount("10387.34");
        user.setCar("云A8848");
        user.setBankCard(new StringBuilder("636669809199510286631"));
        user.setPassport("G99923456");
        user.setDate("2023年9月11日");
        User.Attach attach = user.new Attach();
        attach.setHobbies(Arrays.asList("打球"));
        attach.setEmail("lc123@qq.com");
        attach.setCard("532128199510286631");
        user.setAttach(attach);
        user.setPassowrd("helloword");
        user.setIdCard(new StrBuilder("2368uhg"));

        // FST（线程安全）
        //FSTConfiguration fstConfiguration = FSTConfiguration.createDefaultConfiguration();
        //fstConfiguration.setShareReferences(false); // 关闭对象引用共享，减少元数据开销
        //fstConfiguration.setForceSerializable(true); // 允许未实现Serializable or externalizable

        //fstConfiguration.setInstantiator(new FSTDefaultClassInstantiator()); // 避免反射创建实例

        //fstConfiguration.registerSerializer(Object.class, new FSTBasicObjectSerializer());

        // Kryo（线程不安全）：copy方法支持transient修饰属性拷贝（但序列化不支持），不支持非静态内部类和只依赖无参构造函数，可以使用objenesis框架的StdInstantiatorStrategy策略解决
        Kryo kryo = new Kryo();
        kryo.setReferences(true); // 开启序列化时引用共享（避免死循环）
        kryo.setCopyReferences(true); // 启用拷贝时引用共享（深拷贝场景）
        kryo.setRegistrationRequired(false); // 关闭强制注册
        //kryo.setOptimizedGenerics(true);  // 启用泛型推导即自动推断泛型类型，避免重复写入类信息（需启用优化）（默认开启）
        // 使用 Objenesis 策略（支持非静态内部类和只依赖无参构造函数）
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        // kryo.register：自定义序列化器，可以替换DefaultSerializers中定义内置序列化器，但不能替换UnsafeField中定义内置序列化器
//        kryo.register(StringBuilder.class, new DesensitizationCharSequenceSerializer(null));
//        kryo.register(StringBuffer.class, new DesensitizationCharSequenceSerializer(null));
//        kryo.register(String.class, new DesensitizationCharSequenceSerializer(null));
//        kryo.register(String[].class, new DesensitizationCharSequenceSerializer(null));
//        kryo.register(char[].class, new DesensitizationCharSequenceSerializer(null));


        // 自定义序列化器，可以替换DefaultSerializers中定义内置序列化器，但不能替换UnsafeField中定义内置序列化器
        //kryo.addDefaultSerializer(StrBuilder.class, stringSerializer);
        //kryo.setDefaultSerializer(new DesensitizationFieldSerializerFactory(null));


        //System.out.println("Hutool浅拷贝：" + BeanUtil.toBean(user, User.class));
        //System.out.println("FST深拷贝：" + fstConfiguration.deepCopy(user).getName());
        System.out.println("Kryo深拷贝：" + kryo.copy(user));

        Map<String, Object> map = new HashMap<>();
        map.put("name", "李四");
        map.put("user", user);
        System.out.println("Kryo深拷贝：" + kryo.copy(map));

        // 序列化
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new ByteBufferOutput(baos);
        kryo.writeObject(output, user); // 实际写入 "CUSTOM_hello"
        output.close();

        // 反序列化
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Input input = new ByteBufferInput(bais);
        User user1 = kryo.readObject(input, User.class);
    }
}
