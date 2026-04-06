package com.ouo.mask.jackson;

import cn.hutool.core.util.ReflectUtil;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.ouo.mask.util.SpringUtil;
import lombok.RequiredArgsConstructor;
import org.w3c.dom.Node;

import java.util.List;

/***********************************************************
 * 字符序列化元数据修改器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public class CharSequenceSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc
            , List<BeanPropertyWriter> beanProperties) {

        JacksonDesensitizer desensitizer = SpringUtil.getBean(JacksonDesensitizer.class, true);

        if (null != desensitizer) {
            for (BeanPropertyWriter writer : beanProperties) {
                // 判断字段是否有 @JsonSerialize
                JsonSerialize jsonSerialize = writer.getAnnotation(JsonSerialize.class);
                if (null != jsonSerialize && writer.getType().isTypeOrSubTypeOf(CharSequence.class)) {
                    ReflectUtil.setFieldValue(writer, "_serializer",
                            new CharSequenceDesensitizingSerializer(desensitizer, writer.getSerializer()));
                    //writer.assignSerializer(); // 此方法会校验是否于原序列化器相等
                }
            }
        }
        return super.changeProperties(config, beanDesc, beanProperties);
    }

    // 注解@JsonSerialize优先于modifySerializer
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc
            , JsonSerializer<?> serializer) {

        JacksonDesensitizer desensitizer = null;
        try {
            desensitizer = SpringUtil.getBean(JacksonDesensitizer.class);
        } catch (RuntimeException ignore) {
        }

        if (null != desensitizer) {
            // changeProperties：只能修改非集合、数组、列表、枚举外的Bean对象的属性
            if (beanDesc.getType().isTypeOrSubTypeOf(CharSequence.class)) {
                return new CharSequenceDesensitizingSerializer(desensitizer, serializer);
            } else if (beanDesc.getType().isTypeOrSubTypeOf(Node.class)) {
                return new DOMDesensitizingSerializer(desensitizer);
            }
        }

        return super.modifySerializer(config, beanDesc, serializer);
    }
}
