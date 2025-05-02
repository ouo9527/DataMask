package com.ouo.mask;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrBuilder;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ouo.mask.annotation.*;
import com.ouo.mask.config.DesensitizationAutoConfiguration;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.enums.SensitiveTypeEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.support.log.LogDesensitizationParser;
import com.ouo.mask.util.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/*import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;*/

@Slf4j
//Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
//@RunWith(SpringRunner.class)
@SpringBootTest(classes = DesensitizationAutoConfiguration.class/*, properties = {"classpath*:application.yml"}*/)
public class UnitTest {

    @Autowired
    private DesensitizationHandler handler;

    //@MockBean
    @Autowired
    private ObjectMapper objectMapper;

    //@MockBean
    @Autowired
    private XmlMapper xmlMapper;

    /**
     * 转驼峰命名测试
     */
    @Test
    public void toCamelCase() {
        log.info("蛇形命名（Snake Case）-> 驼峰命名案列1：{}", StringUtil.toCamelCase2("user_login_count"));
        log.info("蛇形命名（Snake Case）-> 驼峰命名案列2：{}", StringUtil.toCamelCase2("User_login_count"));
        log.info("蛇形命名（Snake Case）-> 驼峰命名案列3：{}", StringUtil.toCamelCase2("user_Login_count"));
        log.info("蛇形命名（Snake Case）-> 驼峰命名案列4：{}", StringUtil.toCamelCase2("user_login-count"));
        log.info("烤肉命名（Kebab Case）-> 驼峰命名案列1：{}", StringUtil.toCamelCase2("first-name"));
        log.info("烤肉命名（Kebab Case）-> 驼峰命名案列2：{}", StringUtil.toCamelCase2("First-name"));
        log.info("烤肉命名（Kebab Case）-> 驼峰命名案列3：{}", StringUtil.toCamelCase2("first-Name"));
        log.info("烤肉命名（Kebab Case）-> 驼峰命名案列4：{}", StringUtil.toCamelCase2("first-name_count"));
        log.info("帕斯卡命名（Pascal Case）-> 驼峰命名案列1：{}", StringUtil.toCamelCase2("UserLoginCount"));
        log.info("帕斯卡命名（Pascal Case）-> 驼峰命名案列2：{}", StringUtil.toCamelCase2("userLoginCount"));
        log.info("帕斯卡命名（Pascal Case）-> 驼峰命名案列3：{}", StringUtil.toCamelCase2("UserloginCount"));
        log.info("帕斯卡命名（Pascal Case）-> 驼峰命名案列4：{}", StringUtil.toCamelCase2("UserLogin-Count"));
    }

    /**
     * 配置加载测试
     *
     * @throws IOException
     */
    @Test
    public void properties() throws IOException {
        //仅有单层的Map
        Properties properties = new Properties();
        properties.load(new ClassPathResource("application.properties").getInputStream());

        //具体层级的Map
        Dict dict = Dict.create();
        properties.entrySet().stream()
                .sorted((e1, e2) ->
                        CompareUtil.compare(Convert.toStr(e1.getKey()).length(), Convert.toStr(e2.getKey()).length()))
                .forEach(entry -> {
                    BeanPath.create(entry.getKey().toString()).set(dict, entry.getValue());
                });
        log.info("Properties->Dict：{}", objectMapper.writeValueAsString(dict));
    }

    /**
     * 脱敏测试
     *
     * @throws JsonProcessingException
     */
    @Test
    public void desensitized() throws JsonProcessingException {

        Map<String, Object> data = new HashMap<>();
        data.put("PhonE", "17722657194");
        data.put("name", "[\"张三丰\",2]");
        data.put("ip", "14.23.0.1");

        User u = new User("");
        u.setExtra("[\"北京市朝阳区发和小区1号楼2单元303室\"]");
        u.setMobile("17722657194");
        u.setIdCard(new StrBuilder("6879796065447"));
        data.put("user", u);

        System.out.printf("根据配置进行脱敏：%s\n", objectMapper.writeValueAsString(handler.desensitized(SceneEnum.ALL, data)));

        User user = new User("");
        user.setName("张王四");
        //user.setExtra("{\"phone\":17722657194}");
        user.setExtra("<text><![CDATA[<name>张二</name>]]></text>");
        user.setTel("0987-2322");
        user.setPhone("17722657194");
        user.setMobile("17722657194");
        user.setAddr(new String[]{"北京市朝阳区发和小区1号楼2单元303室", "广东省深圳市福田区1单元"});
        user.setAmount("10387.34");
        user.setCar("云A8848");
        user.setBankCard(new StringBuilder("636669809199510286631"));
        user.setPassport("G99923456");
        user.setDate("2023年9月11日");
        user.setIdCard(new StrBuilder("6879796065447"));
        user.setAges(new int[]{23, 300});
        User.Attach attach = user.new Attach();
        attach.setHobbies(Arrays.asList("打球"));
        attach.setEmail("lc123@qq.com");
        attach.setCard("532128199510286631");
        user.setAttach(attach);

        log.info("根据注解&配置进行脱敏：{}", objectMapper.writeValueAsString(handler.desensitized(SceneEnum.ALL, user)));

        String xml = "<Student> <Name> 李四 </Name> <Phones> <Phone> 13333333311 </Phone> <Phone> &lt;13333333312> </Phone></Phones> <text><![CDATA[<name>张曼玉</name>]]></text> </Student>";
        log.info("XML字符串脱敏后数据：{}", handler.desensitized(SceneEnum.ALL, "", xml));
    }

    /**
     * 日志脱敏
     */
    @Test
    public void logDesensitized() {
        String template = "转义占位符：\\{}，占位符1：{}，占位符2：{name}，占位符3：{ }，占位符4：{}，占位符5：\\\\{}、占位符7：\\{、占位符8：}\"，占位符9：{}、占位符10：{}"; //占位符6：{、
        Object[] args = new Object[]{"hello", "world", "张思", null};
        log.info(template, args);
        System.out.println("MessageFormatter: " + MessageFormatter.arrayFormat(template, args).getMessage());
        /*EscapeSpelExpressionParser expressionParser = new EscapeSpelExpressionParser();
        Expression expression = expressionParser.parseExpression("占位符1:\\{name}、占位符2：{name}、占位符3：{ }、占位符4：{#root}、占位符5：\\\\{}、占位符7：\\{、占位符8：}\"",
                new TemplateParserContext("{", "}"));
        if (expression instanceof ExpressionWrap) {
            log.info("解析spel模板表达式：{}", ((ExpressionWrap) expression).getValue(args, (e, i, c) -> {
                log.info("所无法解析表达式{}：{}", i, e);
                return "";
            }));
        }*/
        LogDesensitizationParser desensitizedLog = new LogDesensitizationParser() {
        };

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
        log.info("日志模板解析：{}", desensitizedLog.resolvePlaceholder(this.getClass().getSimpleName(), template, args));
        log.info("日志脱敏格式1(只含一个参数，多个占位符或占位符spel表达式)：{}", desensitizedLog.resolvePlaceholder("com.ouo.mask",
                "无表达式={}、不规范spel模板表达式={name}、符合规范spel表达式1={#p0.phone}、符合规范spel表达式2={args[0].bankCard}", new Object[]{user}));

        log.info("日志脱敏表达式2(多个参数，多个占位符或占位符spel表达式)：{}", desensitizedLog.resolvePlaceholder("com.ouo.mask",
                "用户名={name}、电话号码={#p0.phone}，{}", new Object[]{user, "{\"phone\":\"1772285144\"}", "<?xml version=\"1.0\" ?><name>张三丰</name>"}));

        log.info("日志脱敏格式3(只含占位符即不含占位符spel表达式，参数个数多余有效占位符)：{}", desensitizedLog.resolvePlaceholder("com.ouo.mask",
                "转义占位符(此时占位符属于无效)=\\{}、用户名={}、电话号码={}", new Object[]{user.getAddr(), user.getName(), user.getPhone()}));
        log.info("日志脱敏格式4(只含占位符即不含占位符spel表达式，参数个数小于有效占位符)：{}", desensitizedLog.resolvePlaceholder("com.ouo.mask",
                "{}、用户名={name}、电话号码={Phone}、{}、{}", new Object[]{user.getAddr(), user.getName(), user.getPhone(), null}));
    }

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

        // 序列化
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new ByteBufferOutput(baos);
        kryo.writeObject(output, user); // 实际写入 "CUSTOM_hello"
        output.close();

        // 反序列化
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Input input = new ByteBufferInput(bais);
        User deserialized = kryo.readObject(input, User.class);

    }

    @Setter
    @Getter
    public static class User extends BasicUser {

        private static final String ip = "192.168.55.13";
        private final String FNAME = "常量可序列化";
        public StrBuilder idCard;
        @Mask(type = SensitiveTypeEnum.ADDRESS)
        String[] addr;

        @Mask(type = SensitiveTypeEnum.FULL_NAME)
        String name;
        @Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#")})
        String extra;
        //@Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#?12$%34")})
        @Mask(type = SensitiveTypeEnum.FIXED_PHONE)
        String tel;
        @Mask(type = SensitiveTypeEnum.MOBILE_PHONE/*, custom = Mask.CommonMaskOptions.PRE_3_SUF_3*/)
        String phone;
        @Mask(type = SensitiveTypeEnum.MOBILE_PHONE, show = @Mask.CustomShow(pre = 2, suf = 3))
        String mobile;
        @Mask(type = SensitiveTypeEnum.BANK_CARD)
        StringBuilder bankCard; //StrBuilder
        @Mask(type = SensitiveTypeEnum.NUMBER)
        String amount;
        @Mask(type = SensitiveTypeEnum.CAR_LICENSE)
        String car;
        @Mask(type = SensitiveTypeEnum.PASSPORT)
        String passport;
        private transient String tname = "transient变量不可序列化";
        @Regex(pattern = "(\\d{4})年(\\d{1,2})月(\\d{1,2})日.*", rv = "$1年**月**日")
        String date;
        private int[] ages;

        private User(String str) {
        }

        @Empty
        Attach attach;

        @Setter
        @Getter
        class Attach {
            //@Empty
            @Mask(type = SensitiveTypeEnum.ADDRESS, show = @Mask.CustomShow(pre = 1, suf = 0))
            List<String> hobbies;
            //@Regex(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
            @Mask(type = SensitiveTypeEnum.EMAIL)
            String email;
            @Hash(algorithm = Hash.AlgorithmEnum.MD5, salt = "ws@4q#")
            String card;
        }
    }

    @Setter
    @Getter
    static abstract class BasicUser {
        private String passowrd;
    }
}
