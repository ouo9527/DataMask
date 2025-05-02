package com.ouo.mask.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.serializers.DesensitizationFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.ouo.mask.enums.SceneEnum;

public class DesensitizationFieldSerializerFactory extends SerializerFactory.FieldSerializerFactory {
    private final SceneEnum sceneEnum;

    public DesensitizationFieldSerializerFactory(SceneEnum sceneEnum) {
        this.sceneEnum = sceneEnum;
    }

    @Override
    public FieldSerializer newSerializer(Kryo kryo, Class type) {
        return new DesensitizationFieldSerializer(sceneEnum, kryo, type, super.getConfig().clone());
    }
}
