package com.ouo.mask.spel;

import com.ouo.mask.util.StrUtil;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/***********************************************************
 * 含转义spel表达式解析器
 *  1、当模板左占位符出现时，右占位符必须出现即要成对，否必须使用\转义字符进行转义，
 *  不然会报"org.springframework.expression.ParseException: Found non terminating string literal starting"
 *
 * Author:   ouo
 * Date:     2024/12/29
 ***********************************************************/
class EscapeSpelExpressionParser extends SpelExpressionParser {
    // escape character
    private static final char ESCAPE_CHAR = '\\';

    /**
     * Return true if the specified suffix can be found at the supplied position in the
     * supplied expression string.
     *
     * @param expression the expression string which may contain the suffix
     * @param pos        the start position at which to check for the suffix
     * @param suffix     the suffix string
     */
    private static boolean isSuffixHere(String expression, int pos, String suffix) {
        int suffixPosition = 0;
        for (int i = 0; i < suffix.length() && pos < expression.length(); i++) {
            if (expression.charAt(pos++) != suffix.charAt(suffixPosition++)) {
                return false;
            }
        }
        // the expression ran out before the suffix could entirely be found
        return suffixPosition == suffix.length();
    }

    /**
     * Copes with nesting, for example '${...${...}}' where the correct end for the first
     * ${ is the final }.
     *
     * @param suffix           the suffix
     * @param expression       the expression string
     * @param afterPrefixIndex the most recently found prefix location for which the
     *                         matching end suffix is being sought
     * @return the position of the correct matching nextSuffix or -1 if none can be found
     */
    private static int skipToCorrectEndSuffix(String suffix, String expression, int afterPrefixIndex)
            throws ParseException {

        // Chew on the expression text - relying on the rules:
        // brackets must be in pairs: () [] {}
        // string literals are "..." or '...' and these may contain unmatched brackets
        int pos = afterPrefixIndex;
        int maxlen = expression.length();
        int nextSuffix = expression.indexOf(suffix, afterPrefixIndex);
        if (nextSuffix == -1) {
            return -1; // the suffix is missing
        }
        Deque<Bracket> stack = new ArrayDeque<>();
        while (pos < maxlen) {
            if (isSuffixHere(expression, pos, suffix) && stack.isEmpty()) {
                break;
            }
            char ch = expression.charAt(pos);
            switch (ch) {
                case '{':
                case '[':
                case '(':
                    stack.push(new Bracket(ch, pos));
                    break;
                case '}':
                case ']':
                case ')':
                    if (stack.isEmpty()) {
                        throw new ParseException(expression, pos, "Found closing '" + ch +
                                "' at position " + pos + " without an opening '" +
                                Bracket.theOpenBracketFor(ch) + "'");
                    }
                    Bracket p = stack.pop();
                    if (!p.compatibleWithCloseBracket(ch)) {
                        throw new ParseException(expression, pos, "Found closing '" + ch +
                                "' at position " + pos + " but most recent opening is '" + p.bracket +
                                "' at position " + p.pos);
                    }
                    break;
                case '\'':
                case '"':
                    // jump to the end of the literal
                    int endLiteral = expression.indexOf(ch, pos + 1);
                    if (endLiteral == -1) {
                        throw new ParseException(expression, pos,
                                "Found non terminating string literal starting at position " + pos);
                    }
                    pos = endLiteral;
                    break;
            }
            pos++;
        }
        if (!stack.isEmpty()) {
            Bracket p = stack.pop();
            throw new ParseException(expression, p.pos, "Missing closing '" +
                    Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
        }
        if (!isSuffixHere(expression, pos, suffix)) {
            return -1;
        }
        return pos;
    }

    /**
     * Determine if escaping is necessary. {@link org.slf4j.helpers.MessageFormatter}
     *
     * @param expression
     * @param delimeterStartIndex
     * @return
     */
    private static boolean isEscapedDelimeter(String expression, int delimeterStartIndex) {
        if (delimeterStartIndex == 0) {
            return false;
        }
        char potentialEscape = expression.charAt(delimeterStartIndex - 1);
        return potentialEscape == ESCAPE_CHAR;
    }

    /**
     * Whether it is a double escape character. {@link org.slf4j.helpers.MessageFormatter}
     *
     * @param expression
     * @param delimeterStartIndex
     * @return
     */
    private static boolean isDoubleEscaped(String expression, int delimeterStartIndex) {
        return delimeterStartIndex >= 2 && expression.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR;
    }

    @Override
    public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
        if (context != null && context.isTemplate()) {
            return this.parseTemplate(expressionString, context);
        } else {
            return super.doParseExpression(expressionString, context);
        }
    }

    private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
        if (StrUtil.isBlank(expressionString)) {
            return new LiteralExpression("");
        }

        Expression[] expressions = this.parseExpressions(expressionString, context);
        if (expressions.length == 1) {
            return expressions[0];
        } else {
            return new TemplateStringExpression(expressionString, expressions);
        }
    }

    /**
     * Helper that parses given expression string using the configured parser. The
     * expression string can contain any number of expressions all contained in "${...}"
     * markers. For instance: "foo${expr0}bar${expr1}". The static pieces of text will
     * also be returned as Expressions that just return that static piece of text. As a
     * result, evaluating all returned expressions and concatenating the results produces
     * the complete evaluated string. Unwrapping is only done of the outermost delimiters
     * found, so the string 'hello ${foo${abc}}' would break into the pieces 'hello ' and
     * 'foo${abc}'. This means that expression languages that used ${..} as part of their
     * functionality are supported without any problem. The parsing is aware of the
     * structure of an embedded expression. It assumes that parentheses '(', square
     * brackets '[' and curly brackets '}' must be in pairs within the expression unless
     * they are within a string literal and a string literal starts and terminates with a
     * single quote '.
     *
     * @param expressionString the expression string
     * @return the parsed expressions
     * @throws ParseException when the expressions cannot be parsed
     */
    private Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {

        String prefix = context.getExpressionPrefix();
        String suffix = context.getExpressionSuffix();
        if (StrUtil.startWith(prefix, ESCAPE_CHAR) || StrUtil.endWith(suffix, ESCAPE_CHAR))
            throw new ExpressionException("Template expressions cannot start or end with an escape character such as \\\\, \\{}, \\{}\\, etc.");

        List<Expression> expressions = new ArrayList<>();

        int startIdx = 0;
        int placeholderIndex = 0;

        while (startIdx < expressionString.length()) {
            int prefixIndex = expressionString.indexOf(prefix, startIdx);

            if (prefixIndex >= startIdx) {
                // an inner expression was found - this is a composite
                if (prefixIndex > startIdx) {
                    boolean isEscaped = isEscapedDelimeter(expressionString, prefixIndex) && !isDoubleEscaped(expressionString, prefixIndex);
                    expressions.add(new LiteralExpression(StrUtil.replaceLast(expressionString.substring(startIdx, prefixIndex + (isEscaped ? prefix.length() : 0))
                            , "\\{", "{")));
                    if (isEscaped) {
                        startIdx = prefixIndex + prefix.length();
                        continue;
                    }
                }
                int afterPrefixIndex = prefixIndex + prefix.length();
                int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
                if (suffixIndex == -1) {
                    throw new ParseException(expressionString, prefixIndex,
                            "No ending suffix '" + suffix + "' for expression starting at character " +
                                    prefixIndex + ": " + expressionString.substring(prefixIndex));
                }
                if (suffixIndex == afterPrefixIndex) {
                    // No expression
                    expressions.add(new PlaceholderExpression(context, new LiteralExpression(""), placeholderIndex++)); //prefix + suffix
                } else {
                    String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
                    // expr.isEmpty() No expression
                    expressions.add(new PlaceholderExpression(context, StrUtil.isBlank(expr) ? new LiteralExpression(expr) :
                            this.doParseExpression(expr, context), placeholderIndex++));
                }
                startIdx = suffixIndex + suffix.length();
            } else {
                // no more ${expressions},#{expressions},{expressions} found in string, add rest as static text
                String expr = expressionString.substring(startIdx);
                expressions.add(StrUtil.isBlank(expr) || !expressions.isEmpty()
                        ? new LiteralExpression(expr) : super.doParseExpression(expr, context));
                startIdx = expressionString.length();
            }
        }
        return expressions.toArray(new Expression[0]);
    }

    /**
     * This captures a type of bracket and the position in which it occurs in the
     * expression. The positional information is used if an error has to be reported
     * because the related end bracket cannot be found. Bracket is used to describe:
     * square brackets [] round brackets () and curly brackets {}
     */
    private static class Bracket {

        char bracket;

        int pos;

        Bracket(char bracket, int pos) {
            this.bracket = bracket;
            this.pos = pos;
        }

        static char theOpenBracketFor(char closeBracket) {
            if (closeBracket == '}') {
                return '{';
            } else if (closeBracket == ']') {
                return '[';
            }
            return '(';
        }

        static char theCloseBracketFor(char openBracket) {
            if (openBracket == '{') {
                return '}';
            } else if (openBracket == '[') {
                return ']';
            }
            return ')';
        }

        boolean compatibleWithCloseBracket(char closeBracket) {
            if (this.bracket == '{') {
                return closeBracket == '}';
            } else if (this.bracket == '[') {
                return closeBracket == ']';
            }
            return closeBracket == ')';
        }
    }
}
