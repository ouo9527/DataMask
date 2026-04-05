package com.ouo.mask.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/***********************************************************
 * 字符列化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
public class CharSequenceSerializer extends JsonSerializer<CharSequence> {

    @Override
    public void serialize(CharSequence value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        log.info("接收到：{}", value);
        gen.writeString(value.toString());
    }
}
