# 敏感信息无感脱敏

## 脱敏规则

主要作用是哪些字段的数据按照何种规则方式进行脱敏。目前支持5种脱敏方式：置空、哈希、正则、替换、掩盖。针对于掩盖这种脱敏，其内置以下脱敏规则：

- 姓名（FULL_NAME）：默认自动根据字符长度显示，当长度小于等于2，则显示第1个字符，否则显示前2个字符；
- 手机号（MOBILE_PHONE）：大陆-11位、台湾-10位、香港澳门-8位。默认自动根据字符长度显示大陆-前3后4、台湾-前3后3、香港澳门-前2后2；
- 固话（FIXED_PHONE）：由3～4位区号+7～8位固定数字组成。默认自动根据字符长度显示，当区号小于等于3，则显示前3后2，否则前4后2；
- 身份证号（ID_CARD）：由6位地址码+8位出生日期+3位顺序码+1位校验码，有15位或18位。默认显示前3后4；
- 地址（ADDRESS）：默认自动根据字符长度显示，当长度大于6，则显示前6，则显示前7；
- 电子邮件（EMAIL）：默认自动根据@前字符长度显示，且@后字符显示，当@前字符长度小于3，则@前字符全显示，否则显示前三位及@后；
- 中国大陆车牌（CAR_LICENSE）：由1个汉字+1个字母+5～6字母和数字组成，默认显示前2后2；
- 银行卡（BANK_CARD）：默认显示前6后4；
- 护照（PASSPORT）：由1位字母（护照类型）+8位数字组成。默认显示前1后3；
- 数值（NUMBER）：默认显示第1位；

注：
- 对于日期字符串脱敏，则采用正则处理，如默认显示年份：(.+年)?(.+月)?(.+日)?.+；而若自定义显示，格式：前,后，如前3后4显示（3,4）、前3显示（3）、后4显示（,4），
    如：@Mask(show = @CustomShow(pre = 3, suf = 4))；
- 只支持脱敏key所对应字符类型，而key所对应枚举、基本类型等类型不支持，若需要脱敏数值类型，则key必须采用字符类型修饰，如：String amount = "3000.01";
- JSON或XML字符串，则同样会进行脱敏；

### 注解方式（侵入式）

精确脱敏（即局部），常用于相同字段需要不同脱敏规则的场景，其中注解如下：

- @Empty 置空（将敏感字段值取空，非等长）
    ```java
    @Empty
    private String bankCard;
    ```
- @Hash 哈希（将敏感字段值取Hash，非等长）
    ```java
    @Hash(algorithm = Hash.AlgorithmEnum.SM3, salt = "ws@4q#")
    private String card;
    ```
- @Regex 正则（将敏感字段值通过正则表达式进行替换，非等长）
    ```java
    @Regex(pattern = "(\\d{4})年(\\d{1,2})月(\\d{1,2})日.*", rv = "$1年**月**日")
    private String date;
    ``` 
- @Repl 替换（将敏感字段值根据所指定位置进行替换，等长）
    ```java
    @Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#?12$%34")})
    private String tel;
    ```
- @Mask 掩盖（将敏感字段值进行*替换，且可采用内置脱敏规则或自定义位置，等长）
    ```java
    @Mask(type = SensitiveTypeEnum.MOBILE_PHONE/*, custom = Mask.CommonMaskOptions.PRE_3_SUF_3*/)
    private String phone;

    @Mask(type = SensitiveTypeEnum.MOBILE_PHONE, show = @Mask.CustomShow(pre = 2, suf = 3))
    privateString mobile;
    ```

### 配置方式（非侵入式）

按脱敏规则配置的字段匹配属脱敏（即脱敏规则全局共享），字段名必须符合（驼峰、蛇形、烤肉、帕斯卡等命名规范），常用于与注解侵入式相反的场景，可同时用且局部优先于全局，除此之外一般推荐配置全局规则。

## 非结构化日志脱敏

对于日志脱敏，只支持Logback、Log4j/Log4j2，且需满足如下日志输出格式：（遵循SpEL表达式规范）

- 只含一个参数(多个占位符或占位符spel表达式)

    ```java
    log.info("无表达式={}、spel模板表达式1(仅一个参数时)={name}、spel表达式2={#p0.phone}、符合规范spel表达式2={#a0.bankCard}", user);
    ```

- 多个参数(多个占位符或占位符spel表达式)

    ```java
    log.info("用户名={name}、{acctName}、电话号码={#p0.phone}，{}", user, "<acctName>张三丰</acctName>", "{\"phone\":\"12345678901\"}");
    ```

- 多个参数(多个占位符或占位符spel表达式且含转义符)

    ```java
    log.info("转义占位符(此时占位符属于无效)=\\{}、{}、用户名={name}、电话号码={Phone}、{}", user.getAddr(), user.getName(), user.getPhone());
    ```
```text
注意：
    1）若占位符含表达式（即会被视为SpEL表达式）时，始终都会从左往右从参数中取值，若还是取不到，则按占位符的索引位置取相应的参数；
    2）若占位符无表达式，则直接按占位符的索引位置取相应的参数；
    3）若占位符被使用\进行转义，则不会参与取参数，反而视为无需占位符，如：\{}；
    4）默认会对参数进行脱敏，如：JSOM/XML字符串、Java Bean等类型
    
日志框架有以下组合：
    1）slf4j + logback： slf4j-api.jar + logback-classic.jar + logback-core.jar
    2）slf4j + log4j： slf4j-api.jar + slf4j-log412.jar + log4j.jar
    3）slf4j + jul： slf4j-api.jar + slf4j-jdk14.jar
    4）也可以只用slf4j无日志实现：slf4j-api.jar + slf4j-nop.jar
    注log4j2配合需要导入log4j2的log4j-api.jar、log4j-core.jar和桥接包log4j-slf4j-impl.jar。
    所谓的桥接包，就是实现StaticLoggerBinder类，用来连接slf4j和日志框架。因为log4j和log4j2刚开始没有StaticLoggerBinder这个类，
    为了不改变程序结构，只能重新写一个新的jar来实现StaticLoggerBinder。而logback出现slf4j之后，于是在logback本身的jar中实现了StaticLoggerBinder，所以就不需要桥接包    
``` 

## Web页面脱敏

对于网页脱敏（基于Spring Web），当采用@ResponseBody修饰或返回类型为ResponseEntity的Restful接口时（即JSON或XML），默认会脱敏。若其他特殊场景如下：

- 对外交互接口：若不需要脱敏则使用**@Desensitization(enabled=false)禁用脱敏**，例如与第三方进行接口交互；
- 按权限/角色：若需要根据用户权限或角色脱敏页面即WEB场景，则可以实现UserMaskPermission接口，并加入Spring容器管理；

## 注解侵入式案列

```text
1、电话号码177*****144：@Mask(type = SensitiveTypeEnum.MOBILE_PHONE)
2、电子邮箱133***@qq.com：@Regex(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
3、身份证：@Hash(algorithm = Hash.AlgorithmEnum.MD5, salt = "ws@4q#")
4、地址：@Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#?12$%34")})
```

## 配置非侵入式案列

```yaml
ouo:
  desens:
    #是否开启脱敏，默认true即开启
    enabled: true
    #字段敏规则
    rules:
      #字段名
      field1: #需脱敏字段1
        #脱敏模式：置空empty、哈希hash、正则regex、替换replace、掩盖mask
        mode: mask
        #敏感类型，采用内置脱敏配置
        type: full_name
      field2: #需脱敏字段2
        mode: replace
        #按位置（从左往右）进行替换
        posns:
          #i表示位置索引
          - i: 3 #表示前三位保持原样
          - i: 8 #3～8采用固定值替换
            #是否固定值，默认true，false为随机值
            fixed: true
            #替换值
            rv: "#"
        #剩余位置
        surplus:
          fixed: false  #表示剩余位置，采用随机值替换  
      field3: #需脱敏字段3
        mode: hash
        #算法
        algorithm: sm3
        #盐
        salt: grvyw$2
      field4: #需脱敏字段4
        mode: empty
      # 如ip  
      field5: #需脱敏字段5
        mode: regex
        #正则表达式，注：yml配置中无需使用\转义符，只需符合正则语法即可，但properties配置文件中正则特殊符号最好使用1个反斜杠进行转义
        pattern: (\d{1,}.).*(.\d{1,})
```

## 相关依赖

- spring-webmvc：基于AOP对Spring/SpringBoot Web应用自动拦截处理响应（需由外部环境提供）；
- spring-boot-autoconfigure：SpringBoot应用自动装配（需由外部环境提供）；
- jackson-dataformat-xml/jackson-module-afterburner：Json/Xml字符串脱敏处理（可由外部环境提供）；
- kryo：高性能拷贝，用于对类中字段脱敏（可由外部环境提供）；
- hutool-crypto：用于Hash哈希方式脱敏，若其使用SM3等算法时，还依赖如bcprov-jdk15to18加密算法（可由外部环境提供）；
- logback-classic：用于Logback框架日志脱敏（需由外部环境提供）；
- log4j-core：用于Log4j/Log4j2框架日志脱敏（需由外部环境提供）；

若外部项目需要使用自己的Jackson和Hutool依赖，此时只需要在项目中使用<dependencyManagement>标签添加如下bom，便可覆盖：
```xml
<!-- Jackson BOM依赖 -->
<dependency>
    <groupId>com.fasterxml.jackson</groupId>
    <artifactId>jackson-bom</artifactId>
    <version>${jackson.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<!-- Hutool BOM依赖 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-bom</artifactId>
    <version>${hutool.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```
