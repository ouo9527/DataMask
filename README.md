# 敏感信息无感脱敏

```text
1、注解侵入式：属于局部即精确脱敏，常用于相同字段需要不同脱敏规则的场景，其中注解如下：
    - @Empty：    置空（将敏感字段值取空，非等长）
    - @Hash：     哈希（将敏感字段值取Hash，非等长）
    - @Regex：    正则（将敏感字段值通过正则表达式进行替换，非等长）
    - @Repl：     替换（将敏感字段值根据所指定位置进行替换，等长）
    - @Mask：     掩盖（将敏感字段值进行*替换，等长，且可采用内置脱敏规则或自定义位置）
    
2、配置非侵入式：属于全局，常用于与注解侵入式相反的场景，可同时用且局部优先于全局，若局部匹配不到，则不会采用全局匹配，因此最好配置全局规则，且全局规则配置，字段名不区分大小写

3、对于日志脱敏，只支持Logback、Log4j/Log4j2；且需满足如下日志输出格式：（遵循SpEL表达式规范）
    1、日志脱敏格式1(只含一个参数，多个占位符或占位符spel表达式)：log.info("无表达式={}、spel模板表达式1(仅一个参数时)={name}、spel表达式2={#p0.phone}、符合规范spel表达式2={#a0.bankCard}", user);
    2、日志脱敏格式2(多个参数，多个占位符或占位符spel表达式)：log.info("用户名={name}、{acctName}、电话号码={#p0.phone}，{}", user, "<acctName>张三丰</acctName>", "{\"phone\":\"1772285144\"}");
    3、日志脱敏格式3(多个参数，多个占位符或占位符spel表达式)：log.info("转义占位符(此时占位符属于无效)=\\{}、{}、用户名={name}、电话号码={Phone}、{}", user.getAddr(), user.getName(), user.getPhone());
    
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

4、对于网页脱敏，当采用@ResponseBody修饰或返回类型为ResponseEntity的Restful接口时（即JSON或XML），默认会脱敏，若不需要脱敏则使用@Desensitization(enabled=false)禁用脱敏，例如与第三方进行接口交互   

5、若需要根据用户权限脱敏页面即WEB场景，则可以实现UserMaskPermission接口，并加入Spring容器管理

6、针对于@Mask内置脱敏规则有：
     1）姓名（FULL_NAME）：默认自动根据字符长度显示，当长度小于等于2，则显示第1个字符，否则显示前2个字符
     2）手机号（MOBILE_PHONE）：大陆-11位、台湾-10位、香港澳门-8位。默认自动根据字符长度显示大陆-前3后4、台湾-前3后3、香港澳门-前2后2
     3）固话（FIXED_PHONE）：由3～4位区号+7～8位固定数字组成。默认自动根据字符长度显示，当区号小于等于3，则显示前3后2，否则前4后2
     4）身份证号（ID_CARD）：由6位地址码+8位出生日期+3位顺序码+1位校验码，有15位或18位。默认显示前3后4
     5）地址（ADDRESS）：默认自动根据字符长度显示，当长度大于6，则显示前6，则显示前7
     6）电子邮件（EMAIL）：默认自动根据@前字符长度显示，且@后字符显示，当@前字符长度小于3，则@前字符全显示，否则显示前三位及@后
     7）中国大陆车牌（CAR_LICENSE）：由1个汉字+1个字母+5～6字母和数字组成。默认显示前2后2
     8）银行卡（BANK_CARD）：默认显示前6后4
     9）护照（PASSPORT）：由1位字母（护照类型）+8位数字组成。默认显示前1后3
     10）数值（NUMBER）：默认显示第1位
     对于日期，则采用正则处理，如默认显示年份：(.+年)?(.+月)?(.+日)?.+
     ；而若自定义显示，格式：前,后，如前3后4显示（3,4）、前3显示（3）、后4显示（,4），如：@Mask(show = @CustomShow(pre = 3, suf = 4))
7、脱敏策略：
    1）packages：脱敏范围即配置包路径，多个值时以英文逗号隔开；为了减少不必要的数据脱敏，否则会影响系统性能，因此推荐设置。默认：空，当为空时，会扫描全部类
    2）effectDate：脱敏生效期
    3）expiryDate：脱敏失效期
    
8、只支持脱敏key所对应字符类型，而key所对应枚举、基本类型等类型不支持，若需要脱敏数值类型，则key必须采用字符类型修饰，如：String amount = "3000.01";
    注：若是JSON或XML字符串，则同样会进行脱敏
```

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
    #脱敏策略
    strategy:
      #脱敏范围即配置包路径，多个值时以英文逗号隔开；为了减少不必要的数据脱敏，否则会影响系统性能，因此推荐设置。默认：空，当为空时，会扫描全部类
      packages: xx.xx,yy.yy
      #effect-date和expiry-date分别表示脱敏生效期和失效期，使用场景，如：只是某个时间段需要脱敏，而过了这段时间就无需脱敏，此时可以通过这两个属性设置
      #effect-date:
      #expiry-date:
    #字段敏规则
    rules:
      #字段名
      field1: #需脱敏字段1
        #脱敏模式：置空empty、哈希hash、正则regex、替换replace、掩盖mask
        mode: mask
        #脱敏场景：日志log、网页web、全部all，默认all
        scene: log
        #敏感类型，采用内置脱敏配置
        type: full_name
      field2: #需脱敏字段2
        mode: replace
        scene: all
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
        scene: web
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


