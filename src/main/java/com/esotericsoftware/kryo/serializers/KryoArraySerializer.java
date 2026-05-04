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
    private final Serializer<T> delegate; // 当前委派序例化器
    private final DesensitizationContext context; // 脱敏上下文

    @Override
    public void write(Kryo kryo, Output output, T object) {
        delegate.write(kryo, output, object);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        return delegate.read(kryo, input, type);
    }

    @Override
    public T copy(Kryo kryo, T original) {
        if (kryo instanceof KryoDesensitizer) {
            if (delegate instanceof StringArraySerializer) {
                int n = ((String[]) original).length;
                String[] copy = new String[n];
                //System.arraycopy(original, 0, copy, 0, copy.length);
                for (int i = 0; i < n; i++)
                    copy[i] = (String) ((KryoDesensitizer) kryo).desensitize(((String[]) original)[i], context);

                return (T) copy;
            } else if (delegate instanceof ObjectArraySerializer) {
                if (original instanceof CharSequence[]) {
                    int n = ((CharSequence[]) original).length;
                    Object[] copy = (CharSequence[]) Array.newInstance(original.getClass().getComponentType(), n);
                    kryo.reference(copy);

                    for (int i = 0; i < n; i++) {
                        CharSequence val = ((CharSequence[]) original)[i];
                        copy[i] = ((KryoDesensitizer) kryo).desensitize(val, context);
                    }

                    return (T) copy;
                }
            }
        }

        return delegate.copy(kryo, original);
    }
}
