package com.ouo.mask.jackson;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.core.DesensitizationExecutor;

import java.io.IOException;

/***********************************************************
 * 自定义JSON/XML生成器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class JacksonGenerator extends JsonGeneratorDelegate {
    private final SerializerProvider provider;

    public JacksonGenerator(JsonGenerator gen, SerializerProvider provider) {
        super(gen);
        this.provider = provider;
    }

    @Override
    public void writeString(String text) throws IOException {
        final DesensitizationExecutor executor = (DesensitizationExecutor) provider.getAttribute(
                DesensitizationExecutor.class); // 字符串脱敏器
        if (null != executor) {
            final DesensitizationContext context = (DesensitizationContext) provider.getAttribute(
                    DesensitizationContext.class); // 脱敏上下文

            // gen.getCurrentValue()：获取当前序列化对象

            text = executor.desensitize(text, this.builder(this.delegate().getOutputContext(), context));
        }

        super.writeString(text);
    }

    /**
     * 通过当前上下文构建DesensitizationContext
     *
     * @param context Json流上下文
     * @param parent  脱敏父上下文
     * @return
     */
    private DesensitizationContext builder(JsonStreamContext context, DesensitizationContext parent) {
        if (null == context || context.inRoot()) return DesensitizationContext
                .builder(parent).build();

        if (context.inObject() && StrUtil.isNotBlank(context.getCurrentName())) {
            DesensitizationContext.Builder builder = DesensitizationContext
                    .builder(parent)
                    .fieldName(context.getCurrentName());
            try {
                builder.field(null == context.getCurrentValue() ? null : ReflectUtil.getField(
                        context.getCurrentValue().getClass(), context.getCurrentName()));
            } catch (RuntimeException ignore) {
                // 无法获取到字段
            }
            return builder.build();
        }

        return this.builder(context.getParent(), parent);
    }
}
