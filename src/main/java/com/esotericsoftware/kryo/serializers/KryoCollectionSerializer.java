package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.kryo.KryoDesensitizer;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static com.esotericsoftware.kryo.serializers.DefaultSerializers.*;

/***********************************************************
 * 自定义集合序列化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public final class KryoCollectionSerializer<T extends Collection> extends Serializer<T> {
    private final Serializer<T> delegate; // 当前委派序例化器
    private final DesensitizationContext context; // 脱敏上下文

    @Override
    public void write(Kryo kryo, Output output, T object) {
        this.delegate.write(kryo, output, object);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        return this.delegate.read(kryo, input, type);
    }

    @Override
    public T copy(Kryo kryo, T original) {
        if (kryo instanceof KryoDesensitizer) {
            if (this.delegate instanceof CollectionSerializer) {
                Collection copy = null;
                if (this.delegate instanceof ArraysAsListSerializer) {
                    copy = new ArrayList<>(original.size()); //Arrays.asList( new Object[original.size()]);
                } else if (this.delegate instanceof TreeSetSerializer) {
                    copy = ((TreeSetSerializer) this.delegate).createCopy(kryo, (TreeSet) original);
                } else {
                    copy = ((CollectionSerializer) this.delegate).createCopy(kryo, original);
                }

                kryo.reference(copy);

                for (Object element : original)
                    copy.add(((KryoDesensitizer) kryo).desensitize(element, this.context));

                return (T) copy;
            } else if (this.delegate instanceof CollectionsSingletonListSerializer) {
                return (T) Collections.singletonList(((KryoDesensitizer) kryo).desensitize(((List) original).get(0),
                        this.context));
            } else if (this.delegate instanceof CollectionsSingletonSetSerializer) {
                return (T) Collections.singleton(((KryoDesensitizer) kryo).desensitize(original.iterator().next(),
                        this.context));
            }
        }

        return this.delegate.copy(kryo, original);
    }
}
