package com.ouo.mask.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/***********************************************************
 * 字符脱敏序列化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
final class CharSequenceDesensitizingSerializer extends JsonSerializer<CharSequence> {

    private final JacksonDesensitizer desensitizer;
    private final JsonSerializer delegate;

    @Override
    public void serialize(CharSequence value, JsonGenerator gen, SerializerProvider provider) throws IOException {

        this.delegate.serialize(this.desensitizer.desensitized(value, gen, provider), gen, provider);
    }
}
