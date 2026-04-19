package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.kryo.DesensitizingKryo;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Array;

/***********************************************************
 * 脱敏字符数组序例化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public final class DesensitizingArraySerializer<T> extends Serializer<T> {

    private final Serializer<T> serializer;

    @Override
    public void write(Kryo kryo, Output output, T object) {
        serializer.write(kryo, output, object);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        return serializer.read(kryo, input, type);
    }

    public T copy(Kryo kryo, T original) {
        if (kryo instanceof DesensitizingKryo) {
            if (serializer instanceof DefaultArraySerializers.StringArraySerializer) {
                int n = ((String[]) original).length;
                String[] copy = new String[n];
                //System.arraycopy(original, 0, copy, 0, copy.length);
                for (int i = 0; i < n; i++)
                    copy[i] = this.desensitized(kryo, kryo.copy(((String[]) original)[i]));

                return (T) copy;
            } else if (serializer instanceof DefaultArraySerializers.ObjectArraySerializer) {
                if (original instanceof CharSequence[]) {
                    int n = ((CharSequence[]) original).length;
                    Object[] copy = (CharSequence[]) Array.newInstance(original.getClass().getComponentType(), n);
                    kryo.reference(copy);
                    for (int i = 0; i < n; i++) {
                        CharSequence val = ((CharSequence[]) original)[i];
                        /*copy[i] = ( val instanceof StringBuffer || val instanceof StringBuilder )
                                ? this.desensitized(kryo, val) : kryo.copy(val);*/
                        copy[i] = this.desensitized(kryo, val);
                        if (copy[i] == val) copy[i] = kryo.copy(val);
                    }

                    return (T) copy;
                }
            }
        }

        return serializer.copy(kryo, original);
    }

    private <V> V desensitized(Kryo kryo, V val) {

        return ((DesensitizingKryo) kryo).getDesensitizer().desensitized(((DesensitizingKryo) kryo).getSceneEnum(),
                ((DesensitizingKryo) kryo).getCurFieldName(), val, ((DesensitizingKryo) kryo).getCurAnnotRule());
    }
}
