package com.ouo.mask.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

public class DesensitizingJsonGeneratorDelegate extends JsonGeneratorDelegate {
    public DesensitizingJsonGeneratorDelegate(JsonGenerator delegate) {
        super(delegate);
    }
}
