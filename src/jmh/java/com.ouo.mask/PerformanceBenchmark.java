package com.ouo.mask;

import cn.hutool.core.text.StrBuilder;
import com.ouo.mask.config.DesensitizationAutoConfiguration;
import com.ouo.mask.core.Desensitizer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

//Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
//@RunWith(SpringRunner.class)
@SpringBootTest(classes = DesensitizationAutoConfiguration.class/*, rule = {"classpath*:application.yml"}*/)
public class PerformanceBenchmark {

    // ----------------------
    // 方式一：独立 JMH 基准测试（推荐）
    // ----------------------
    @State(Scope.Benchmark) // 测试状态共享（整个测试阶段仅初始化一次）
    @BenchmarkMode(Mode.Throughput) // 测试模式：吞吐量（ops/s）
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS) // 预热 5 次，每次 1 秒
    @Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS) // 测量 5 次，每次 2 秒
    @Fork(1) // 分叉数（测试进程数）
    @Threads(4) // 并发线程数
    @Slf4j
    public static class JMHSample {

        @Autowired
        private Desensitizer desensitizer;

        @Setup(Level.Trial) // 每个测试阶段初始化一次
        public void setup() {

        }

        @Benchmark // 标记为压测方法
        public void testThroughput(Blackhole blackhole) {
            UnitTest.User user = new UnitTest.User();
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
            UnitTest.User.Attach attach = user.new Attach();
            attach.setHobbies(Arrays.asList("打球"));
            attach.setEmail("lc123@qq.com");
            attach.setCard("532128199510286631");
            user.setAttach(attach);

            log.info("根据注解&配置进行脱敏：{}", desensitizer.desensitized(SceneEnum.ALL, user));

            String xml = "<Student> <Name> 李四 </Name> <Phones> <Phone> 13333333311 </Phone> <Phone> &lt;13333333312> </Phone></Phones> <text><![CDATA[<name>张曼玉</name>]]></text> </Student>";
            log.info("XML字符串脱敏后数据：{}", desensitizer.desensitized(SceneEnum.ALL, "", xml));

            //blackhole.consume(false); // 避免 JIT 优化忽略结果
        }
    }

    // ----------------------
    // 方式二：通过 JUnit 执行 JMH（简化版）
    // ----------------------
    @Test
    public void runJMHBenchmark() throws RunnerException {
        Options options = new OptionsBuilder()
                .include(JMHSample.class.getSimpleName()) // 指定要运行的基准测试类
                .output("jmh-result.log") // 结果输出文件
                .shouldFailOnError(true) // 出错时测试失败
                .build();
        new Runner(options).run(); // 执行 JMH 测试
    }
}
