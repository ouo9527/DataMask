package com.ouo.mask.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/***********************************************************
 * 自定义序列化拦截器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class JacksonBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        return new JacksonSerializer<>(super.modifySerializer(config, beanDesc, serializer));
    }
}
