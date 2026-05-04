package com.ouo.mask.core;

import cn.hutool.core.util.ClassUtil;
import com.ouo.mask.core.des.CharSequenceDesensitizer;
import com.ouo.mask.core.des.Desensitizer;
import com.ouo.mask.core.des.SemiStructuredDesensitizer;
import com.ouo.mask.core.des.StringDesensitizer;
import com.ouo.mask.core.rule.DesensitizationProperties;
import com.ouo.mask.jackson.JacksonDesensitizer;
import com.ouo.mask.kryo.KryoDesensitizer;
import com.ouo.mask.semi.SemiStructuredMapper;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***********************************************************
 * 默认脱敏执行器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
@Slf4j
public class DefaultDesensitizationExecutor implements DesensitizationExecutor {
    // 全局脱敏规则
    private final DesensitizationProperties properties;
    // 半结构化处理器
    private final SemiStructuredMapper mapper;
    // 默认脱敏器（基于Kryo）
    private final ThreadLocal<Desensitizer> kryoThreadLocal;
    // 脱敏器
    private Map<Class, Desensitizer> desensitizers = new ConcurrentHashMap<>();

    public DefaultDesensitizationExecutor(DesensitizationProperties properties, SemiStructuredMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;

        // 注册数据类型脱敏器（若找不到合适数据类型脱敏器时，由KryoDesensitizer脱敏器处理）
        SemiStructuredDesensitizer desensitizer = new JacksonDesensitizer(this, mapper);
        this.register(Node.class, desensitizer);
        this.register(Iterable.class, desensitizer); // Iterable可多次迭代，而Iterator一次性迭代器类型（游标/指针迭代）
        //this.register(Iterator.class, desensitizer);
        this.register(String.class, new StringDesensitizer(this, desensitizer));
        this.register(CharSequence.class, new CharSequenceDesensitizer(this));
        // 默认脱敏器（基于Kryo）
        this.kryoThreadLocal = ThreadLocal.withInitial(() -> new KryoDesensitizer(this));
    }

    /*@Override
    public <T> T desensitize(T data) {
        return this.desensitize(data, DesensitizationContext
                .builder()
                .properties(properties).build());
    }*/

    @Override
    public <T> T desensitize(T data, DesensitizationContext context) {
        //if (null == context) new DesensitizeException("Desensitization context cannot be null..");
        Desensitizer desensitizer = this.getDesensitizer(ClassUtil.getClass(data));
        if (null == desensitizer) return data;

        try {
            return (T) desensitizer.desensitize(data, DesensitizationContext
                    .builder(context)
                    .properties(properties).build());
        } catch (Throwable e) {
            log.warn("【{}】类脱敏异常：{}", ClassUtil.getClassName(data, false), e.getMessage());
            return data;
        } finally {
            if (desensitizer instanceof KryoDesensitizer) {
                kryoThreadLocal.remove();
            }
        }
    }

    /**
     * 根据数据类型获取脱敏处理器（若找不到合适数据类型脱敏器时，由KryoDesensitizer脱敏器处理）
     *
     * @param type 数据类型
     * @return 返回与之对应数据脱敏处理器
     */
    protected Desensitizer getDesensitizer(Class type) {
        Desensitizer desensitizer = null;
        if (null != type) {
            desensitizer = this.desensitizers.get(type);

            if (null == desensitizer) {
                for (Map.Entry<Class, Desensitizer> entry : this.desensitizers.entrySet()) {
                    // 判断type是否是entry.getKey()子类
                    if (entry.getKey().isAssignableFrom(type)) {
                        desensitizer = entry.getValue();
                        if (!(desensitizer instanceof SemiStructuredDesensitizer &&
                                Collection.class.isAssignableFrom(type))) return desensitizer;
                    }
                }
                return kryoThreadLocal.get();
            }
        }

        return desensitizer;
    }

    /**
     * 注册数据类型脱敏处理器
     *
     * @param type         数据类型
     * @param desensitizer 脱敏处理器
     * @return 返回注册成功后数据脱敏处理器
     */
    public Desensitizer register(Class type, Desensitizer desensitizer) {
        if (this.desensitizers.containsKey(type)) {
            return this.desensitizers.replace(type, desensitizer);
        }
        return this.desensitizers.put(type, desensitizer);
    }
}
