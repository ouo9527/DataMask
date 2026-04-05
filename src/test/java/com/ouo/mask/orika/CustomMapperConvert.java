package com.ouo.mask.orika;

import com.ouo.mask.util.StrUtil;
import ma.glasnost.orika.Converter;
import ma.glasnost.orika.impl.generator.SourceCodeContext;
import ma.glasnost.orika.impl.generator.VariableRef;
import ma.glasnost.orika.impl.generator.specification.Convert;
import ma.glasnost.orika.metadata.FieldMap;

import static java.lang.String.format;
import static ma.glasnost.orika.impl.generator.SourceCodeContext.statement;

public class CustomMapperConvert extends Convert {

    @Override
    public String generateEqualityTestCode(FieldMap fieldMap, VariableRef source, VariableRef destination, SourceCodeContext code) {

        if (source.getConverter() instanceof DesensitizationConverter) {
            String className = StrUtil.replace(code.usedConverter(source.getConverter())
                    , Converter.class.getCanonicalName(), DesensitizationConverter.class.getName());
            if (destination.type().isPrimitive()) {
                String wrapper = source.asWrapper();
                String wrapperType = destination.type().getWrapperType().getSimpleName();
                String primitive = destination.type().getName();
                return format("(%s == ((%s)%s.convert(\"%s\", %s, %s, mappingContext)).%sValue())", destination, wrapperType,
                        className, source.name(), wrapper, code.usedType(destination), primitive);
            } else if (source.type().isPrimitive()) {
                return format("(%s == %s.convert(\"%s\", %s, %s, mappingContext))", destination.asWrapper(),
                        className, source.name(), source.asWrapper(), code.usedType(destination));
            } else {
                return format("(%s != null && %s.equals(%s.convert(\"%s\", %s, %s, mappingContext)))", destination, destination,
                        className, source.name(), source.asWrapper(), code.usedType(destination));
            }
        } else {
            return super.generateEqualityTestCode(fieldMap, source, destination, code);
        }
    }

    @Override
    public String generateMappingCode(FieldMap fieldMap, VariableRef source, VariableRef destination, SourceCodeContext code) {

        if (source.getConverter() instanceof DesensitizationConverter) {
            String statement;
            boolean canHandleNulls;
            if (code.isDebugEnabled()) {
                code.debugField(fieldMap, "converting using " + source.getConverter());
            }

            statement = destination.assignIfPossible("%s.convert(\"%s\", %s, %s, mappingContext)", StrUtil.replace(code.usedConverter(source.getConverter())
                    , Converter.class.getCanonicalName(), DesensitizationConverter.class.getName()), source.name(), source.asWrapper(), code.usedType(destination));
            canHandleNulls = false;
            boolean shouldSetNull = shouldMapNulls(fieldMap, code) && !destination.isPrimitive();
            String destinationNotNull = destination.ifPathNotNull();

            if (!source.isNullPossible() || (canHandleNulls && shouldSetNull && "".equals(destinationNotNull))) {
                return statement(statement);
            } else {
                String elseSetNull = shouldSetNull ? (" else " + destinationNotNull + "{ \n" + destination.assignIfPossible("null")) + ";\n }"
                        : "";
                return statement(source.ifNotNull() + "{ \n" + statement) + "\n}" + elseSetNull;
            }
        } else {
            return super.generateMappingCode(fieldMap, source, destination, code);
        }
    }
}
