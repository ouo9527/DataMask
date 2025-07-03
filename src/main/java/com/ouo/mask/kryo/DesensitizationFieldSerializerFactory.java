package com.ouo.mask.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.serializers.DesensitizationFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;

public class DesensitizationFieldSerializerFactory extends SerializerFactory.FieldSerializerFactory {
    private final SceneEnum sceneEnum;
    private final DesensitizationHandler desensitizationHandler;

    public DesensitizationFieldSerializerFactory(SceneEnum sceneEnum, DesensitizationHandler desensitizationHandler) {
        this.sceneEnum = sceneEnum;
        this.desensitizationHandler = desensitizationHandler;
    }

    @Override
    public FieldSerializer newSerializer(Kryo kryo, Class type) {
        return new DesensitizationFieldSerializer(sceneEnum, desensitizationHandler, kryo, type, super.getConfig().clone());
    }
}
