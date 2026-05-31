package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ouo.mask.core.DesensitizationContext;
import com.ouo.mask.kryo.KryoDesensitizer;
import lombok.RequiredArgsConstructor;

/**
 * 自定义对象字段序列化器
 *
 * 利用Kryo实现对象序列化/反序列化和深拷贝
 * <p>
 * 序列化/反序列化：通过将对象转换为字节流（序列化）存储或传输，再从字节流恢复为对象（反序列化），本质是跨存储/传输的对象重建。
 * “跨世界的重生”，适用用于将对象状态持久化到外部介质并重建，性能中低（涉及IO操作），如对象的持久化存储（如文件、数据库）、网络传输（如 RPC）、跨进程通信等场景。
 * 反序列化生成的对象与原对象完全独立（新内存地址）。
 * Copy（拷贝）：直接在内存中复制对象数据，不涉及字节流转换，分为浅拷贝（复制引用，共享对象内容）和深拷贝（递归复制所有属性，新对象独立），仅作用于同一 JVM 内，不涉及存储或传输。
 * 内存里的“克隆”，适用于快速创建独立副本，性能高；浅拷贝的对象与原对象共享属性引用，修改会相互影响；深拷贝则完全独立（取决于实现是否彻底）。
 * <p>
 * 1、反射（Reflection）：通过Class对象在运行时动态获取类的信息（如字段、方法、注解等），并执行调用。反射会绕过编译器优化，每次调用都涉及【安全检查、动态解析】等CPU高开销操作步骤，因此性能低。
 * 不适合高频调用场景；无法操作private final字段（需配合setAccessible(true)，且可能受JVM限制）。
 * 典型场景：框架开发（如Spring依赖注入）、动态代理、JSON序列化/反序列化（如Jackson、Gson）。
 * 2、Unsafe（sun.misc.Unsafe）：通过Unsafe类直接操作底层内存和JVM内部功能（如对象FieldOffset、内存分配、CAS操作等），绕过Java的访问控制（如私有字段访问），属于JVM内部API（非标准，可能随版本变化）。
 * 性能优于反射，但非标准API，依赖JVM实现（如OpenJDK的sun.misc.Unsafe，可能在其他JVM中不可用），其次存在内存管理风险高（如allocateMemory/freeMemory），易导致JVM崩溃（如野指针、内存泄漏）。
 * 典型场景：高性能框架（如Netty、Kafka）、原子操作（如AtomicInteger）、无锁编程、内存管理。
 * <p>
 * 3、字节码操作（如：ASM）：直接操作字节码（.class文件），在类加载前动态生成或修改类的字节码，生成的类在运行时与手写代码性能接近。性能最优，因为字节码直接生成并加载，运行时无额外开销。
 * ASM 支持操作非public和final修饰的成员，但强行绕过限制可能导致运行时异常或违反 Java 规范。因此对于Kryo框架，默认此时会采用反射进行操作。
 * 典型场景：动态代理（如CGLIB、Byte Buddy）、字节码增强（如Hibernate字节码增强）、代码生成（如MyBatis的Mapper动态生成）。
 */
@RequiredArgsConstructor
public final class KryoFieldSerializer<T> extends Serializer<T> {
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
    public T copy(Kryo kryo, T original) {
        if (kryo instanceof KryoDesensitizer && this.delegate instanceof FieldSerializer) {
            T copy = ((FieldSerializer<T>) this.delegate).createCopy(kryo, original);
            kryo.reference(copy);

            FieldSerializer.CachedField[] cachedFields = ((FieldSerializer<T>) this.delegate).getCopyFields();
            for (FieldSerializer.CachedField cachedField : cachedFields) {
                if (cachedField instanceof UnsafeField) {
                    this.desensitize((UnsafeField) cachedField, original, copy);
                } else if (cachedField instanceof AsmField) {
                    this.desensitize((AsmField) cachedField, original, copy);
                } else if (cachedField instanceof ReflectField) {
                    this.desensitize((ReflectField) cachedField, original, copy);
                } else {
                    cachedField.copy(original, copy);
                }
            }

            return copy;
        }

        return this.delegate.copy(kryo, original);
    }

    /**
     * 脱敏数据
     *
     * @param cachedField 对象字段操作（UnsafeField｜AsmField｜ReflectField）
     * @param src         原始数据
     * @param dest        脱敏后数据
     */
    @SuppressWarnings("unchecked")
    private <F extends ReflectField> void desensitize(F cachedField, T src, T dest) {
        FieldSerializer<T> serializer = ((FieldSerializer<T>) this.delegate);
        KryoDesensitizer kryo = (KryoDesensitizer<?>) serializer.getKryo();
        try {
            cachedField.set(dest, kryo.desensitize(cachedField.get(src), DesensitizationContext
                    .builder(this.context).field(cachedField.getField()).build()));
        } catch (IllegalAccessException ex) {
            throw new KryoException("Error accessing field: " + cachedField.getName() + " (" +
                    serializer.getType().getName() + ")", ex);
        } catch (KryoException ex) {
            ex.addTrace(cachedField.getName() + " (" + serializer.getType().getName() + ")");
            throw ex;
        } catch (Throwable t) {
            KryoException ex = new KryoException(t);
            ex.addTrace(cachedField.getName() + " (" + serializer.getType().getName() + ")");
            throw ex;
        }
    }
}
