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
            if (this.delegate instanceof MapSerializer) {
                Map copy = null;
                if (this.delegate instanceof ConcurrentSkipListMapSerializer) {
                    copy = ((ConcurrentSkipListMapSerializer) this.delegate).createCopy(kryo,
                            (ConcurrentSkipListMap) original);
                } else if (this.delegate instanceof TreeMapSerializer) {
                    copy = ((TreeMapSerializer) this.delegate).createCopy(kryo, (TreeMap) original);
                } else {
                    copy = ((MapSerializer<T>) this.delegate).createCopy(kryo, original);
                }

                //kryo.reference(copy);
                for (Object obj : original.entrySet()) {
                    Map.Entry<?, ?> entry = this.desensitize((KryoDesensitizer<Object>) kryo, (Map.Entry<?, ?>) obj);
                    copy.put(entry.getKey(), entry.getValue());
                }
                return (T) copy;
            } else if (this.delegate instanceof CollectionsSingletonMapSerializer) {
                Map.Entry<?, ?> entry = this.desensitize((KryoDesensitizer<Object>) kryo,
                        (Map.Entry<?, ?>) original.entrySet().iterator().next());

                return (T) Collections.singletonMap(entry.getKey(), entry.getValue());
            }
        }

        return this.delegate.copy(kryo, original);
    }

    /**
     * 脱敏数据
     *
     * @param kryo  Kryo脱敏器
     * @param entry 原始数据
     * @return 脱敏后数据
     */
    private Map.Entry<?, ?> desensitize(KryoDesensitizer<Object> kryo, Map.Entry<?, ?> entry) {
        Object key = entry.getKey();

        if (key instanceof String) return MapUtil.builder().put(key, kryo.desensitize(entry.getValue(),
                DesensitizationContext.builder(this.context).fieldName((String) key).build()))
                .build().entrySet().iterator().next();

        return MapUtil.builder().put(kryo.desensitize(key, this.context), kryo.desensitize(entry.getValue(),
                this.context)).build().entrySet().iterator().next();
    }
}
