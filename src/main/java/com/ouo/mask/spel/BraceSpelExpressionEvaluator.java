package com.ouo.mask.spel;

import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;

/***********************************************************
 * {}模板表达式执行器，例如：{name}
 *
 * Author:   ouo
 * Date:     2024/11/28
 ***********************************************************/
public class BraceSpelExpressionEvaluator extends SpelExpressionEvaluator {

    private final TemplateParserContext parserContext = new TemplateParserContext("{", "}");

    @Override
    public ParserContext getParserContext() {
        return this.parserContext;
    }
}
