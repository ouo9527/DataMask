package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.kryo.DesensitizingKryo;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

/***********************************************************
 * 脱敏字典序例化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public final class DesensitizingMapSerializer<T extends Map> extends Serializer<T> {

    private final Serializer<T> serializer;

    @Override
    public void write(Kryo kryo, Output output, T object) {
        serializer.write(kryo, output, object);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        return serializer.read(kryo, input, type);
    }

    @Override
    public T copy(Kryo kryo, T original) {
        if (kryo instanceof DesensitizingKryo) {
            if (serializer instanceof MapSerializer) {
                Map copy = null;
                if (serializer instanceof DefaultSerializers.ConcurrentSkipListMapSerializer) {
                    copy = ((DefaultSerializers.ConcurrentSkipListMapSerializer) serializer).createCopy(kryo, (ConcurrentSkipListMap) original);
                } else if (serializer instanceof DefaultSerializers.TreeMapSerializer) {
                    copy = ((DefaultSerializers.TreeMapSerializer) serializer).createCopy(kryo, (TreeMap) original);
                } else copy = ((MapSerializer<T>) serializer).createCopy(kryo, original);

                for (Iterator iter = original.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    copy.put(entry.getKey(), this.desensitized(kryo, entry));
                }
                return (T) copy;
            } else if (serializer instanceof DefaultSerializers.CollectionsSingletonMapSerializer) {
                Map.Entry entry = (Map.Entry) original.entrySet().iterator().next();
                return (T) Collections.singletonMap(entry.getKey(), this.desensitized(kryo, entry));
            }
        }

        return serializer.copy(kryo, original);
    }

    private Object desensitized(Kryo kryo, Map.Entry entry) {
        Object key = kryo.copy(entry.getKey());
        Object val = kryo.copy(entry.getValue());

        if (key instanceof String) {
            return ((DesensitizingKryo) kryo).getDesensitizer().desensitized(((DesensitizingKryo) kryo).getSceneEnum(),
                    (String) key, val);
        } else
            return ((DesensitizingKryo) kryo).getDesensitizer().desensitized(((DesensitizingKryo) kryo).getSceneEnum(), val);
    }
}
