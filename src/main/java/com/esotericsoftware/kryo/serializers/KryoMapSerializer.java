package com.esotericsoftware.kryo.serializers;

import cn.hutool.core.map.MapUtil;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.kryo.KryoDesensitizer;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static com.esotericsoftware.kryo.serializers.DefaultSerializers.*;

/***********************************************************
 * 自定义字典序列化器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@RequiredArgsConstructor
public final class KryoMapSerializer<T extends Map> extends Serializer<T> {
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
            if (delegate instanceof MapSerializer) {
                Map copy;
                if (delegate instanceof ConcurrentSkipListMapSerializer) {
                    copy = ((ConcurrentSkipListMapSerializer) delegate).createCopy(kryo, (ConcurrentSkipListMap) original);
                } else if (delegate instanceof TreeMapSerializer) {
                    copy = ((TreeMapSerializer) delegate).createCopy(kryo, (TreeMap) original);
                } else {
                    copy = ((MapSerializer<T>) delegate).createCopy(kryo, original);
                }

                //kryo.reference(copy);
                for (Object obj : original.entrySet()) {
                    Map.Entry<?, ?> entry = this.desensitize((KryoDesensitizer) kryo, (Map.Entry) obj);
                    copy.put(entry.getKey(), entry.getValue());
                }
                return (T) copy;
            } else if (delegate instanceof CollectionsSingletonMapSerializer) {
                Map.Entry<?, ?> entry = this.desensitize((KryoDesensitizer) kryo, (Map.Entry) original.entrySet()
                        .iterator().next());

                return (T) Collections.singletonMap(entry.getKey(), entry.getValue());
            }
        }

        return delegate.copy(kryo, original);
    }

    /**
     * 脱敏数据
     *
     * @param kryo  Kryo脱敏器
     * @param entry 原始数据
     * @return 脱敏后数据
     */
    private Map.Entry<?, ?> desensitize(KryoDesensitizer kryo, Map.Entry<?, ?> entry) {
        Object key = entry.getKey();

        if (key instanceof String) return MapUtil.builder().put(key, kryo.desensitize(entry.getValue(),
                DesensitizationContext.builder(context).fieldName((String) key).build()))
                .build().entrySet().iterator().next();

        return MapUtil.builder().put(kryo.desensitize(key, context), kryo.desensitize(entry.getValue(),
                context)).build().entrySet().iterator().next();
    }
}
