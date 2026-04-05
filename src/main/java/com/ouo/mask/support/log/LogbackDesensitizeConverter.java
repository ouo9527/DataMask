package com.ouo.mask.support.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/***********************************************************
 * 基于logback提供MessageConverter，在message打印之前，
 *  允许对“参数格式化之后的message”（formattedMessage）进行转换，
 *  最终logger打印的实际内容是converter返回的整形后的结果。
 *  需要在logback.xml配置文件，增加如下配置：
 *  <configuration>
 *      ……
 *      <conversionRule conversionWord="m" converterClass="com.ouo.mask.support.log.LogbackDesensitizeConverter"/>
 *      ……
 *  </configuration>
 *  注：1）conversionRule标签中conversionWord即定义日志输出格式中的信息参数，默认%msg，若如上自定义为m，则%m
 *  2）当输出一个日志logEvent时，其处理流程如下：Appender ->Encoder->Layout-> Converter
 *  Logger：日志对象的抽象，负责日志等级，Mark的设置等
 *  Appender：负责将日志输出到不同的目的地，控制台、文件、数据库、网络...
 *  Encoder：将日志事件转换为字节数组，同时将字节数组写入到一个 OutputStream
 *  Layouts：负责将日志事件转化为格式化的字符串
 *
 * Author:   ouo
 * Date:     2022/11/18
 ***********************************************************/
public class LogbackDesensitizeConverter extends MessageConverter implements LogDesensitizationParser {

    @Override
    public String convert(ILoggingEvent event) {
        try {
            return this.resolvePlaceholder(event.getLoggerName(), event.getMessage(), event.getArgumentArray());
        } catch (Throwable e) {
            //会引发死循环，从而造成栈溢出
            //log.error("从Sping容器中加载DesensitizationRule脱敏规则异常：", e);
            return super.convert(event);
        }
    }
}
