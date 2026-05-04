package com.ouo.mask.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/***********************************************************
 * 自定义Json序列化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
/**/
@RequiredArgsConstructor
public class JacksonSerializer<T> extends JsonSerializer<T> {
    protected final JsonSerializer<T> delegate; // 当前委派序例化器

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        this.delegate.serialize(value, new JacksonGenerator(gen, provider), provider);
    }
}
