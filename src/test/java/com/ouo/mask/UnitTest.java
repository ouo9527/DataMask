package com.ouo.mask;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
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
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;*/

@Slf4j
//todo：Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
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
        //todo: 仅有单层的Map
        Properties properties = new Properties();
        properties.load(new ClassPathResource("application.properties").getInputStream());

        //todo：具体层级的Map
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

        User u = new User();
        u.setExtra("[\"北京市朝阳区发和小区1号楼2单元303室\"]");
        u.setMobile("17722657194");
        u.setIdCard("6879796065447");
        data.put("user", u);

        log.info("根据配置进行脱敏：{}", objectMapper.writeValueAsString(handler.desensitized(UnitTest.class.getName(), SceneEnum.ALL, null, data)));

        User user = new User();
        user.setName("张王四");
        user.setExtra("{\"phone\":17722657194}");
        user.setTel("0987-2322");
        user.setPhone("17722657194");
        user.setMobile("17722657194");
        user.setAddr("北京市朝阳区发和小区1号楼2单元303室");
        user.setAmount("10387.34");
        user.setCar("云A8848");
        user.setBankCard("636669809199510286631");
        user.setPassport("G99923456");
        user.setDate("2023年9月11日");
        User.Attach attach = new User.Attach();
        attach.setHobbies(new String[]{"打球"});
        attach.setEmail("lc123@qq.com");
        attach.setCard("532128199510286631");
        user.setAttach(attach);

        log.info("根据注解&配置进行脱敏：{}", objectMapper.writeValueAsString(handler.desensitized(UnitTest.class.getName(), SceneEnum.ALL, null, user)));

        String xml = "<Student> <Name> 李四 </Name> <Phones> <Phone> 13333333311 </Phone> <Phone> 13333333312 </Phone></Phones> <text><![CDATA[<name>张三</name>]]></text> </Student>";
        log.info("XML字符串脱敏后数据：{}", handler.desensitized(UnitTest.class.getName(), SceneEnum.ALL, null, xml));
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

        User user = new User();
        user.setName("张王四");
        user.setExtra("{\"phone\":17722657194}");
        user.setTel("0987-2322");
        user.setPhone("17722657194");
        user.setMobile("17722657194");
        user.setAddr("北京市朝阳区发和小区1号楼2单元303室");
        user.setAmount("10387.34");
        user.setCar("云A8848");
        user.setBankCard("636669809199510286631");
        user.setPassport("G99923456");
        user.setDate("2023年9月11日");
        User.Attach attach = new User.Attach();
        attach.setHobbies(new String[]{"打球"});
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

    @Setter
    @Getter
    static class User {
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
        @Mask(type = SensitiveTypeEnum.ADDRESS)
        String addr;
        @Mask(type = SensitiveTypeEnum.NUMBER)
        String amount;
        @Mask(type = SensitiveTypeEnum.CAR_LICENSE)
        String car;
        @Mask(type = SensitiveTypeEnum.PASSPORT)
        String passport;
        @Mask(type = SensitiveTypeEnum.BANK_CARD)
        String bankCard;
        @Regex(pattern = "(\\d{4})年(\\d{1,2})月(\\d{1,2})日.*", rv = "$1年**月**日")
        String date;

        String idCard;

        @Empty
        Attach attach;

        @Setter
        @Getter
        static class Attach {
            @Empty
            String[] hobbies;
            //@Regex(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
            @Mask(type = SensitiveTypeEnum.EMAIL)
            String email;
            @Hash(algorithm = Hash.AlgorithmEnum.MD5, salt = "ws@4q#")
            String card;
        }
    }
}
