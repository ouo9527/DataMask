package com.ouo.mask;

import cn.hutool.core.text.StrBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ouo.mask.core.DesensitizationExecutor;
import com.ouo.mask.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * JMH（Java Microbenchmark Harness）是Java语言的微基准测试框架，用于准确、可靠地测量和评估Java代码的性能。
 * • JMH 独立于 Spring 上下文运行：JMH 基准测试在单独的 JVM 进程中执行，默认不加载 Spring 容器。
 * • Bean 作用域冲突：Spring 的 Bean 默认是单例的，而 JMH 可能多次实例化测试类，导致依赖注入失败。
 * <p>
 * 注：
 * • 不支持debug模式，否则会报org.openjdk.jmh.runner.BenchmarkException: Benchmark error
 * • 若获取Spring容器中Bean，则需要如：
 * private static ApplicationContext applicationContext; // 使用静态进行优化性能（避免重复初始化）
 *
 * @Setup(Level.Trial) // 每个测试阶段初始化一次
 * public void setup() {
 * // 加载 Spring 配置（根据实际配置调整）
 * if (applicationContext == null) applicationContext = new SpringApplicationBuilder(DesensitizationAutoConfiguration.class)
 * .web(WebApplicationType.NONE)
 * .run();
 * this.desensitizer = this.applicationContext.getBean(Desensitizer.class);
 * }
 * 若Spring则采用if (applicationContext == null) applicationContext = new AnnotationConfigApplicationContext(DesensitizationAutoConfiguration.class);
 * <p>
 * 若基准测试使用多线程（@Threads），需确保 Bean 是线程安全的：
 * • 使用原型作用域修饰Bean：@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
 * • 同步访问：在 Bean 的方法中加锁（影响性能）
 * <p>
 * junit4：@RunWith(SpringJUnit4ClassRunner.class)或@RunWith(SpringRunner.class) + @SpringBootTest
 * junit5：@SpringJUnitConfig 或 @SpringBootTest
 * <p>
 * <p>
 * JMH注解详解：
 * 1、@Warmup：预热用于让 JVM 完成编译优化、类加载等操作，避免冷启动对测试结果的干扰，可用在类级别和方法级别
 * • iterations：表示预热的次数；默认为-1；
 * • time：表示每次预热的时间；默认为-1
 * • timeUnit：表示每次预热的时间的单位，默认为TimeUnit.SECONDS；
 * 注：根据实际测试需求调整 iterations 和 time，复杂场景建议通过多次实验确定最佳预热配置（如观察吞吐量/延迟曲线是否趋于稳定）。
 * <p>
 * 2、@Measurement：配置测量阶段的参数（收集基准测试数据），可用在类级别和方法级别
 * • iterations：表示测试的次数；默认为5；
 * • time：表示每次测试的时间；默认为10
 * • timeUnit：表示每次测试的时间的单位，默认为TimeUnit.SECONDS；
 * <p>
 * 3、@Benchmark：基准测试方法，只可用在方法上，表示该方法是需要测试的方法；
 * <p>
 * 4、@BenchmarkMode：基准测试的模式（测量指标）; 可用在类和方法上；一共有四种Mode, 分别是:
 * • Mode.Throughput: 吞吐量（ops）测试, 即指定时间单位该方法会执行多少次;
 * • Mode.AverageTime: 平均耗时测试, 即输出每次操作平均耗时
 * • Mode.SampleTime: 抽样测试, 会在执行过程中采样, 输出最快的, 50% 快, 90%, 95%, 99%, 99.9%, 99.99%, 100%
 * • Mode.SingleShotTime: 冷启动测试, 设置后, 该方法一轮只会运行一次; 该模式主要为了测试冷启动的性能;
 * 也可以使用 Mode.All 对以上四种模式都进行测试; 或者通过 @BenchmarkMode({Mode.SampleTime, Mode.SingleShotTime}) 方式选择需要进行测试的多种模式;
 * <p>
 * 5、@OutputTimeUnit：表示输出测试报告的时间单位, 可用在类和方法上;
 * <p>
 * 6、@State：类注解, 表示类对象的作用域, 用于在测试方法间共享数据; 一般标记在静态内部类里；作用域主要有三种取值:
 * • Scope.Thread: 默认，表示类对象会在 Benchmark 各个线程执行之前初始化, 作为入参, 且各个线程的实参是相互独立的; 且参数类型为当前 @State 注解的类名;
 * • Scope.Benchmark: 表示 Benchmark 的各个线程共用一个入参;
 * • Scope.Group: 表示一个线程组共用一个对象
 * <p>
 * 7、@Setup/@TearDown：方法级别注解, 表示在启动测试方法前做的准备工作/表示在测试方法执行结束之后做的操作; 必须在 @State 的类中才能使用; 实际上就是 @State 管理的对象的生命周期的一部分;
 * 可以选择操作的执行方式,有以下三种情况:
 * • Level.Iteration: 每个测试阶段（预热/测量）前/后执行一次;
 * • Level.Trial: 每个迭代前/后执行一次;
 * • Level.Invocation: 每个方法调用前/后执行一次（细粒度）;
 * <p>
 * 8、@OperationsPerInvocation：类、方法级别注解，指定每次方法调用的操作数（影响吞吐量计算）。 假设为n, 则用于告诉 JMH 标记的方法执行了一次相当于执行了 n 次, 最后统计结果时会除n取平均值;
 * <p>
 * 9、@Fork：可用在方法级别和类级别上, 配置测试的分叉（Fork）行为; 默认不指定value时, 相当于 5;
 * • value取0时, 测试默认只执行一次, 且测试方法会在 main 启动进程中执行;
 * • value取1时, 会fork出一个线程来专门执行测试方法; 与 main 方法不在同一个进程中执行;
 * • value取n时, 会fork出 n 个线程来执行测试方法;
 * 可以通过使用 @Fork 注解对测试方法多进行几轮测试, 提高测试结果的精确性;
 * <p>
 * 10、@Group：方法级别注解; 指定方法所属的测试组; 相同测试组的方法会在一个 Benchmark 中执行; 可以模拟生产环境的资源竞争;
 * <p>
 * 11、@GroupThreads：方法级别注解，为分组测试指定线程数;
 * <p>
 * 12、@Param：字段级别注解，为基准测试方法提供参数化输入，支持数组、枚举等类型; 必须在 @State 的类中才能使用;
 * <p>
 * 13、@CompilerControl：控制JIT编译器的优化行为
 * • Mode.DONT_INLINE：禁止方法内联。
 * • Mode.EXCLUDE：排除方法，不进行编译优化。
 * <p>
 * <p>
 * Blackhole：JMH 提供的黑洞对象, 可以对执行结果进行消费, 调用其 consume() 方法可对执行结果进行消费;
 * OptionsBuilder：是指如何启动测试并进行配置构造的（即程序化构建基准测试配置）;
 * • include：表示包含的测试类;
 * • threads：在执行每一个测试方法时, 即执行每一个 Benchmark 时,会创造多少个线程去执行;
 * • warmupIterations：表示预热的次数;
 * • measurementTime：表示执行测试的时间;
 * • measurementIterations：表示执行测试的次数;
 * • resultFormat：指定输出结果文件的格式, 可以指定为 JSON 文件
 * • result：指定结果的输出位置; 如平均值及误差：平均值及误差、最小值、平均值、最大值、方差(stdev)、置信区间(CI)等
 * 注意：若同时使用注解和 OptionsBuilder，OptionsBuilder 中的配置会覆盖注解的设置
 * <p>
 * ops/s：每秒完成的操作数，值越大越好（表示高吞吐量）
 * s/op：每次操作的平均耗时（秒），值越小越好（表示低延迟）
 */

// ----------------------
// 方式一：独立 JMH 基准测试（推荐）
// ----------------------
@State(Scope.Benchmark) // 测试状态共享（整个测试阶段仅初始化一次）
@BenchmarkMode(Mode.Throughput) // 测试模式：吞吐量（ops/s）
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS) // 预热 1 次，每次 1 秒
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS) // 测量 1 次，每次 2 秒
@Fork(2) // 分叉数（测试进程数）
@Threads(4) // 并发线程数
@Slf4j
public class SpringBootJmhTest {

    private static ApplicationContext applicationContext; // 使用静态进行优化性能（避免重复初始化）

    //@Autowired
    private DesensitizationExecutor executor;

    private ObjectMapper objectMapper;

    // ----------------------
    // 方式一：通过 main函数
    // ----------------------
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(SpringBootJmhTest.class.getSimpleName()) // 指定要运行的基准测试类
                .output("jmh-result.log") // 结果输出文件
                .shouldFailOnError(true) // 出错时测试失败
                .build();
        new Runner(options).run(); // 执行 JMH 测试
    }

    @Setup(Level.Trial) // 每个测试阶段初始化一次
    public void setup() {
        // 加载 Spring 配置（根据实际配置调整）
        if (applicationContext == null)
            applicationContext = new SpringApplicationBuilder(DesensitizationAutoConfiguration.class)
                    .web(WebApplicationType.NONE)
                    .run();
        // AnnotationConfigApplicationContext：不会自动扫描类路径，需显式注册 @Configuration 类。
        //if (applicationContext == null) applicationContext = new AnnotationConfigApplicationContext(DesensitizationAutoConfiguration.class);
        this.executor = applicationContext.getBean(DesensitizationExecutor.class);
        this.objectMapper = applicationContext.getBean("objectMapper", ObjectMapper.class);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        ((AnnotationConfigApplicationContext) applicationContext).close();
    }

    /**
     * 脱敏压测
     */
    @Benchmark // 标记为压测方法
    public void desensitized() {
        User user = new User();
        user.setName("张王四");
        //user.setExtra("{\"phone\":17722657194}");
        user.setExtra("<text><![CDATA[<name>张二狗蛋儿</name>]]></text>");
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
        attach.setHobbies(Arrays.asList("打球", "2"));
        attach.setEmail("lc123@qq.com");
        attach.setCard("532128199510286631");
        user.setAttach(attach);

        try {
            System.out.printf("根据注解&配置进行脱敏：%s\n", objectMapper.writeValueAsString(
                    executor.desensitize(user)));
        } catch (JsonProcessingException e) {
        }

        String xml = "<Student> <Name> 李四 </Name> <Phones> <Phone> 13333333311 </Phone> <Phone> &lt;13333333312> </Phone></Phones> <text><![CDATA[<name>张曼玉</name>]]></text> </Student>";
        log.info("XML字符串脱敏后数据：{}", executor.desensitize(xml));

        //blackhole.consume(false); // 避免 JIT 优化忽略结果
    }

    // ----------------------
    // 方式二：通过 JUnit 执行 JMH（简化版）
    // ----------------------
    @Test
    void runJMHBenchmark() throws RunnerException {
        Options options = new OptionsBuilder()
                .verbosity(VerboseMode.EXTRA) // 打印额外日志
                .include(SpringBootJmhTest.class.getSimpleName()) // 指定要运行的基准测试类
                .output("jmh-result.log") // 结果输出文件
                .shouldFailOnError(true) // 出错时测试失败
                .build();
        new Runner(options).run(); // 执行 JMH 测试
    }
}
