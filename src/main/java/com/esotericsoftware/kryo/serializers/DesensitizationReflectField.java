package com.esotericsoftware.kryo.serializers;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Generics;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;

import java.lang.reflect.Field;

/***********************************************************
 * 采用反射处理字段脱敏
 * Author:   刘春
 * Date:     2025/12/7
 ***********************************************************/
public class DesensitizationReflectField extends ReflectField {

    private final SceneEnum sceneEnum;
    private final DesensitizationHandler desensitizationHandler;

    DesensitizationReflectField(SceneEnum sceneEnum, DesensitizationHandler desensitizationHandler, Field field, FieldSerializer serializer, Generics.GenericType genericType) {
        super(field, serializer, genericType);
        this.sceneEnum = sceneEnum;
        this.desensitizationHandler = desensitizationHandler; //SpringUtil.getBean(DesensitizationHandler.class);
    }

    @Override
    public void copy(Object original, Object copy) {
        try {
            set(copy, desensitizationHandler.desensitized(sceneEnum, field, get(original)));
        } catch (IllegalAccessException ex) {
            throw new KryoException("Error accessing field: " + name + " (" + fieldSerializer.type.getName() + ")", ex);
        } catch (KryoException ex) {
            ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
            throw ex;
        } catch (Throwable t) {
            KryoException ex = new KryoException(t);
            ex.addTrace(name + " (" + fieldSerializer.type.getName() + ")");
            throw ex;
        }
    }
}
