package com.ouo.mask.orika;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import ma.glasnost.orika.metadata.TypeBuilder;

import java.util.Collection;
import java.util.Map;

@Slf4j
public class DesensitizationConverter extends CustomConverter<Object, Object> {
    /*@Override
    public boolean canConvert(Type<?> sourceType, Type<?> destinationType) {
            *//*return (this.sourceType.isAssignableFrom(sourceType.getRawType())
                    && this.destinationType.isAssignableFrom(destinationType.getRawType()))*//*
        return (CharSequence.class.isAssignableFrom(sourceType.getRawType())
                *//*&& CharSequence.class.isAssignableFrom(destinationType.getRawType())*//*)
                || this.isArrayOfType(sourceType, destinationType); //super.canConvert(sourceType, destinationType);
    }*/

    @Override
    public Object convert(Object source, Type<?> destinationType, MappingContext mappingContext) {
        log.info("MappingContext:Property：{}", ReflectUtil.getFieldValue(mappingContext, "properties"));

        if ((CharSequence.class.isAssignableFrom(source.getClass())
                /*&& CharSequence.class.isAssignableFrom(destinationType.getRawType())*/)
                || this.isArrayOfType(sourceType, destinationType)) {
            Object currentPopertyName = mappingContext.getProperty(Thread.currentThread().getId() + ":" + mappingContext.toString());
            if (currentPopertyName instanceof CharSequence) {
                log.info("【{}】字段需要脱敏", currentPopertyName);
            }

            return source;
        } else if (source instanceof Map) {
            return this.mapperFacade.mapAsMap((Map) source, new TypeBuilder<Map<Object, Object>>() {
                    }.build()
                    , new TypeBuilder<Map<Object, Object>>() {
                    }.build());
        } else if (source instanceof Collection) {
            Collection results = CollUtil.create(source.getClass());
            this.mapperFacade.mapAsCollection((Collection) source, results, Object.class);
            return results;
        } else {
            return this.mapperFacade.map(source, source.getClass());
        }
    }

    public Object convert(String propertyName, Object source, Type<? extends Object> destinationType, MappingContext mappingContext) {
        /*IntrospectorPropertyResolver propertyResolver = (IntrospectorPropertyResolver) mappingContext.getProperty(Properties.PROPERTY_RESOLVER_STRATEGY);
        Map<String, Property> properties = CollUtil.getFirst(((Map<java.lang.reflect.Type, Map<String, Property>>) ReflectUtil
                .getFieldValue(propertyResolver, "propertiesCache")).values());//propertyResolver.getProperties(mappingContext.getResolvedSourceType());
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            if (destinationType.getUniqueIndex() == entry.getValue().getType().getUniqueIndex()) {
                log.info("【{}】需要脱敏字段", entry.getKey());
                break;
            }
        }*/
        log.info("【{}】需要脱敏字段", propertyName);

        return source;
    }

    /**
     * 通过反射判断一个对象是否为指定类型的数组
     *
     * @return 是否为该类型的数组
     */
    private boolean isArrayOfType(Type<?> sourceType, Type<?> destinationType) {
        if (!sourceType.isArray() && !destinationType.isArray()) {
            return false;
        }
        // 获取数组的组件类型（即元素类型）
        // 判断元素类型是否是目标类型的子类或实现类
        return this.sourceType.isAssignableFrom(sourceType.getRawType().getComponentType())
                /*&& this.destinationType.isAssignableFrom(destinationType.getRawType().getComponentType())*/;
    }
}
