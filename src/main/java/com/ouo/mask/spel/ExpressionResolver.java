package com.ouo.mask.spel;

import org.springframework.expression.common.TemplateParserContext;

import java.util.Map;

/***********************************************************
 * 模板表达式解析器
 * Author:   刘春
 * Date:     2024/11/28
 ***********************************************************/
public interface ExpressionResolver {

    /**
     * 获取表达式值
     *
     * @param expression SpEL表达式
     * @param metadata   元数据，属于root object，可以使用#root.属性或直接使用属性
     * @return
     */
    default Object getValue(String expression, SpelExpressionMetaData metadata) {
        return this.getValue(expression, metadata, Object.class);
    }

    /**
     * 获取表达式值
     *
     * @param expression SpEL表达式
     * @param metadata   元数据，属于root object，可以使用#root.属性或直接使用属性
     * @param resultType 结果类型
     * @return
     */
    default <T> T getValue(String expression, SpelExpressionMetaData metadata, Class<T> resultType) {
        return this.getValue(expression, metadata, null, null, resultType);
    }

    /**
     * 获取表达式值
     *
     * @param expression       SpEL表达式
     * @param metadata         元数据，属于root object，可以使用#root.属性或直接使用属性
     * @param contextVariables 表达式的上下文值，属于evaluation context，需要使用#属性
     * @return
     */
    default Object getValue(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables) {
        return this.getValue(expression, metadata, contextVariables, Object.class);
    }

    /**
     * 获取表达式值
     *
     * @param expression       SpEL表达式
     * @param metadata         元数据，属于root object，可以使用#root.属性或直接使用属性
     * @param contextVariables 表达式的上下文值，属于evaluation context，需要使用#属性
     * @return
     */
    default <T> T getValue(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables, Class<T> resultType) {
        return this.getValue(expression, metadata, contextVariables, null, resultType);
    }

    /**
     * 通过日志模板占位符获取表达式值
     *
     * @param expression       SpEL表达式，也可以采用模板表达式，默认模板占位符{}，如：{name}
     * @param metadata         元数据，属于root object，可以使用{#root.属性}或{属性}
     * @param expressionParser 处理不符合spel规范的模板表达式
     * @return
     */
    default String getValueByLogTemplate(String expression, SpelExpressionMetaData metadata, PlaceholderExpressionParser<String> expressionParser) {
        return this.getValue(expression, metadata, null, new TemplateParserContext("{", "}"), String.class, expressionParser);
    }

    /**
     * 通过默认模板占位符获取表达式值
     *
     * @param expression SpEL表达式，也可以采用模板表达式，默认模板占位符#{}，如：#{name}
     * @param metadata   元数据，属于root object，可以使用#{#root.属性}或#{属性}
     * @return
     */
    default String getValueByDefaultTemplate(String expression, SpelExpressionMetaData metadata) {
        return this.getValueByDefaultTemplate(expression, metadata, null, null);
    }

    /**
     * 通过默认模板占位符获取表达式值
     *
     * @param expression       SpEL表达式，也可以采用模板表达式，默认模板占位符#{}，如：#{name}
     * @param metadata         元数据，属于root object，可以使用#{#root.属性}或#{属性}
     * @param expressionParser 处理不符合spel规范的模板表达式
     * @return
     */
    default String getValueByDefaultTemplate(String expression, SpelExpressionMetaData metadata, PlaceholderExpressionParser<String> expressionParser) {
        return this.getValueByDefaultTemplate(expression, metadata, null, expressionParser);
    }

    /**
     * 通过默认模板占位符获取表达式值
     *
     * @param expression       SpEL表达式，也可以采用模板表达式，默认模板占位符#{}，如：#{name}
     * @param metadata         元数据，属于root object，可以使用#{#root.属性}或#{属性}
     * @param contextVariables 表达式的上下文值，属于evaluation context，需要使用#{#属性}
     * @return
     */
    default String getValueByDefaultTemplate(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables) {
        return this.getValueByDefaultTemplate(expression, metadata, contextVariables, null);
    }

    /**
     * 通过默认模板占位符获取表达式值
     *
     * @param expression       SpEL表达式，也可以采用模板表达式，默认模板占位符#{}，如：#{name}
     * @param metadata         元数据，属于root object，可以使用#{#root.属性}或#{属性}
     * @param contextVariables 表达式的上下文值，属于evaluation context，需要使用#{#属性}
     * @param expressionParser 处理不符合spel规范的模板表达式
     * @return
     */
    default String getValueByDefaultTemplate(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables
            , PlaceholderExpressionParser<String> expressionParser) {
        return this.getValue(expression, metadata, contextVariables, new TemplateParserContext(), String.class, expressionParser);
    }

    /**
     * 获取模板表达式值
     *
     * @param expression  SpEL模板表达式
     * @param metadata    元数据，属于root object
     * @param placeholder 模板占位符
     * @return
     */
    default String getValue(String expression, SpelExpressionMetaData metadata, TemplateParserContext placeholder) {
        return this.getValue(expression, metadata, null, placeholder);
    }

    /**
     * 获取模板表达式值
     *
     * @param expression       SpEL模板表达式
     * @param metadata         元数据，属于root object
     * @param contextVariables 表达式的上下文值，属于evaluation context
     * @param placeholder      模板占位符
     * @return
     */
    default String getValue(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables
            , TemplateParserContext placeholder) {
        return this.getValue(expression, metadata, contextVariables, placeholder, String.class);
    }

    /**
     * 获取模板表达式值
     *
     * @param expression       SpEL模板表达式
     * @param metadata         元数据，属于root object
     * @param contextVariables 表达式的上下文值，属于evaluation context
     * @param placeholder      模板占位符
     * @param resultType       结果类型
     * @return
     */
    default <T> T getValue(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables
            , TemplateParserContext placeholder, Class<T> resultType) {
        return this.getValue(expression, metadata, contextVariables, placeholder, resultType, null);
    }

    /**
     * 获取模板表达式值
     *
     * @param expression       SpEL模板表达式
     * @param metadata         元数据，属于root object
     * @param contextVariables 表达式的上下文值，属于evaluation context
     * @param placeholder      模板分隔符
     * @param resultType       结果类型
     * @param expressionParser 处理不符合spel规范的模板表达式
     * @return
     */
    <T> T getValue(String expression, SpelExpressionMetaData metadata, Map<String, Object> contextVariables
            , TemplateParserContext placeholder, Class<T> resultType, PlaceholderExpressionParser<T> expressionParser);
}
