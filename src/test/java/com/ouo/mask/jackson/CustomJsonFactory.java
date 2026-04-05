package com.ouo.mask.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;

import java.io.IOException;
import java.io.Writer;

public class CustomJsonFactory extends JsonFactory {
    @Override
    protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        JsonGenerator delegate = super._createGenerator(out, ctxt);
        return new DesensitizingJsonGeneratorDelegate(delegate);
    }
}
