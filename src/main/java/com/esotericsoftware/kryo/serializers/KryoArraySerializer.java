package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.kryo.KryoDesensitizer;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Array;

import static com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ObjectArraySerializer;
import static com.esotericsoftware.kryo.serializers.DefaultArraySerializers.StringArraySerializer;

/***********************************************************
 * 自定义数组序列化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public final class KryoArraySerializer<T> extends Serializer<T> {
    // 当前委派序例化器
    private final Serializer<T> delegate;
    // 脱敏上下文
    private final DesensitizationContext context;

    @Override
    public void write(Kryo kryo, Output output, T object) {
        this.delegate.write(kryo, output, object);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        return this.delegate.read(kryo, input, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T copy(Kryo kryo, T original) {
        if (kryo instanceof KryoDesensitizer) {
            if (this.delegate instanceof StringArraySerializer) {
                int n = ((String[]) original).length;
                String[] copy = new String[n];
                //System.arraycopy(original, 0, copy, 0, copy.length);
                for (int i = 0; i < n; i++)
                    copy[i] =((KryoDesensitizer<String>) kryo).desensitize(((String[]) original)[i], this.context);

                return (T) copy;
            } else if (this.delegate instanceof ObjectArraySerializer && original instanceof CharSequence[]) {
                int n = ((CharSequence[]) original).length;
                Object[] copy = (CharSequence[]) Array.newInstance(original.getClass().getComponentType(), n);
                kryo.reference(copy);

                for (int i = 0; i < n; i++) {
                    CharSequence val = ((CharSequence[]) original)[i];
                    copy[i] = ((KryoDesensitizer<CharSequence>) kryo).desensitize(val, this.context);
                }

                return (T) copy;
            }
        }

        return this.delegate.copy(kryo, original);
    }
}
