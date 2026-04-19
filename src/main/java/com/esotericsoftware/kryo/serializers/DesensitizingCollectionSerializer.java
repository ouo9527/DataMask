package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.kryo.DesensitizingKryo;
import lombok.RequiredArgsConstructor;

import java.util.*;

/***********************************************************
 * 脱敏集合序例化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public final class DesensitizingCollectionSerializer<T extends Collection> extends Serializer<T> {

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
            if (serializer instanceof CollectionSerializer) {
                Collection copy = null;
                if (serializer instanceof DefaultSerializers.ArraysAsListSerializer) {
                    copy = new ArrayList(original.size()); //Arrays.asList( new Object[original.size()]);
                } else if (serializer instanceof DefaultSerializers.TreeSetSerializer) {
                    copy = ((DefaultSerializers.TreeSetSerializer) serializer).createCopy(kryo, (TreeSet) original);
                } else copy = ((CollectionSerializer) serializer).createCopy(kryo, original);

                kryo.reference(copy);
                for (Object element : original)
                    copy.add(this.desensitized(kryo, kryo.copy(element)));

                return (T) copy;
            } else if (serializer instanceof DefaultSerializers.CollectionsSingletonListSerializer) {
                return (T) Collections.singletonList(this.desensitized(kryo, kryo.copy(((List) original).get(0))));
            } else if (serializer instanceof DefaultSerializers.CollectionsSingletonSetSerializer) {
                return (T) Collections.singleton(this.desensitized(kryo, kryo.copy(original.iterator().next())));
            }
        }

        return serializer.copy(kryo, original);
    }

    private Object desensitized(Kryo kryo, Object val) {
        return ((DesensitizingKryo) kryo).getDesensitizer().desensitized(((DesensitizingKryo) kryo).getSceneEnum(),
                ((DesensitizingKryo) kryo).getCurFieldName(), val, ((DesensitizingKryo) kryo).getCurAnnotRule());
    }
}
