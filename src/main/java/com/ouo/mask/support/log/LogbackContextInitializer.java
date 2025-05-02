package com.ouo.mask.support.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/***********************************************************
 * 提供LogbackContextInitializer进行初始化，代替修改logback配置
 *
 * Author:   刘春
 * Date:     2024/11/18
 ***********************************************************/
public class LogbackContextInitializer /*implements ApplicationContextInitializer<ConfigurableApplicationContext>*/ {

    public void initialize() {
        // 获取LoggerContext
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        Map<String, String> ruleRegistry = (Map) lc.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (ruleRegistry == null) {
            ruleRegistry = new HashMap<String, String>();
        }
        ruleRegistry.put("m", LogbackDesensitizeConverter.class.getName());
        ruleRegistry.put("msg", LogbackDesensitizeConverter.class.getName());
        ruleRegistry.put("message", LogbackDesensitizeConverter.class.getName());
        // 注册自定义的MessageConverter
        lc.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);

        // 获取根Logger
        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);

        // 遍历根Logger的Appender
        Iterator<Appender<ILoggingEvent>> it = rootLogger.iteratorForAppenders();
        while (it.hasNext()) {
            Appender<ILoggingEvent> appender = it.next();
            if (appender instanceof OutputStreamAppender) {
                // 获取Encoder
                Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
                // 获取Layout
                Layout<?> layout = ((LayoutWrappingEncoder) encoder).getLayout();
                // 找到PatternLayout的Appender
                if (layout instanceof PatternLayoutBase) {
                    // 设置encoder上下文
                    layout.setContext(lc);
                    // 启动encoder以应用配置
                    layout.start();
                }
            }
        }
    }
}
