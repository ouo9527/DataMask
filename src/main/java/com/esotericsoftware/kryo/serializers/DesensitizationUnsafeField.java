package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Generics;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.util.SpringUtil;

import java.lang.reflect.Field;

import static com.esotericsoftware.kryo.unsafe.UnsafeUtil.unsafe;

public class DesensitizationUnsafeField extends UnsafeField {

    private final SceneEnum sceneEnum;
    private final DesensitizationHandler desensitizationHandler;

    public DesensitizationUnsafeField(SceneEnum sceneEnum, Field field, FieldSerializer serializer, Generics.GenericType genericType) {
        super(field, serializer, genericType);
        this.sceneEnum = sceneEnum;
        this.desensitizationHandler = SpringUtil.getBean(DesensitizationHandler.class);
    }

    @Override
    public void copy(Object original, Object copy) {
        try {
            unsafe.putObject(copy, offset, desensitizationHandler.desensitized(sceneEnum, field, unsafe.getObject(original, offset)));
        } catch (KryoException ex) {
            ex.addTrace(this + " (" + fieldSerializer.type.getName() + ")");
            throw ex;
        } catch (Throwable t) {
            KryoException ex = new KryoException(t);
            ex.addTrace(this + " (" + fieldSerializer.type.getName() + ")");
            throw ex;
        }
    }
}
