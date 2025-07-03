package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.unsafe.UnsafeUtil;
import com.esotericsoftware.kryo.util.Generics;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;

import java.lang.reflect.Field;

public class DesensitizationUnsafeField extends UnsafeField {

    private final SceneEnum sceneEnum;
    private final DesensitizationHandler desensitizationHandler;

    public DesensitizationUnsafeField(SceneEnum sceneEnum, DesensitizationHandler desensitizationHandler, Field field, FieldSerializer serializer, Generics.GenericType genericType) {
        super(field, serializer, genericType);
        this.sceneEnum = sceneEnum;
        this.desensitizationHandler = desensitizationHandler; //SpringUtil.getBean(DesensitizationHandler.class);
    }

    @Override
    public void copy(Object original, Object copy) {
        try {
            UnsafeUtil.unsafe.putObject(copy, offset, desensitizationHandler.desensitized(sceneEnum, field, UnsafeUtil.unsafe.getObject(original, offset)));
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
