package com.ouo.mask.support.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.List;

/***********************************************************
 *  基于log4j RewritePolicy实现自定义日志格式（msg模板（{}）替换之前，即只是此时日志总进口，有利于脱敏）
 *  注：
 *  1）在类路径（如：resources目录）下创建log4j2.component.properties文件，
 *  并在改文件中加入Log4jLogEventFactory=xx.xx.Log4jDesensitizedLogEventFactory,会被log4j框架自动加载
 *  2）log4j.xml文件【无需配置】
 *  注意Marker的使用：
 *  private static final Marker SENSITIVE_DATA_MARKER = MarkerFactory.getMarker("SENSITIVE_DATA_MARKER");
 *  logger.warn(SENSITIVE_DATA_MARKER, cardNo);
 *
 * Author:   ouo
 * Date:     2022/12/3
 ***********************************************************/
public class Log4jDesensitizedLogEventFactory implements LogEventFactory, LogDesensitizationParser {

    @Override
    public LogEvent createEvent(String loggerName, Marker marker, String fqcn, Level level, Message message,
                                List<Property> properties, Throwable t) {
        Message newMessage = message;
        try {
            newMessage = new SimpleMessage(this.resolvePlaceholder(loggerName, message.getFormat(),
                    message.getParameters()));
        } catch (Throwable ignore) {
            //会引发死循环，从而造成栈溢出
            //log.error("从Sping容器中加载DesensitizationRule脱敏规则异常：", e);
        }
        // message.getFormattedMessage()即格式化后message，此时为null
        return new Log4jLogEvent(loggerName, marker, fqcn, level, newMessage, properties, t);
    }
}
