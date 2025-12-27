package com.ouo.mask.spel;

import org.springframework.expression.common.TemplateParserContext;

/***********************************************************
 * {}模板表达式解析器，例如：{name}
 * Author:   刘春
 * Date:     2024/11/28
 ***********************************************************/
public class BraceSpelExpressionResolver implements ExpressionResolver {
    private TemplateParserContext parserContext = new TemplateParserContext("{", "}");

    @Override
    public TemplateParserContext getTemplateParserContext() {
        return this.parserContext;
    }
}
