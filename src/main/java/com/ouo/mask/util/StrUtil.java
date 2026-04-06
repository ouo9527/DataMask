package com.ouo.mask.util;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.ReUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***********************************************************
 * 字符串工具
 *
 * Author:   ouo
 * Date:     2024/4/15
 ***********************************************************/
public class StrUtil extends cn.hutool.core.util.StrUtil {
    public final static String DEFAULT_ROOT_NAME = "_"; // 默认根节点
    public final static String DEFAULT_START_ROOT_NODE = "<" + DEFAULT_ROOT_NAME + ">"; // 默认开始根节点
    public final static String DEFAULT_END_ROOT_NODE = "</" + DEFAULT_ROOT_NAME + ">"; // 默认结束根节点

    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        } else {
            for (int i = 0; i < substring.length(); ++i) {
                if (str.charAt(index + i) != substring.charAt(i)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * 简单从SpEL表达式获取对象名称
     *
     * @param spelStr SpEL表达式
     * @return 返回对象名称
     */
    public static String toObjName(String spelStr) {
        if (!contains(spelStr, CharPool.DOT)) return null;

        String objName = subBefore(spelStr, CharPool.DOT, false);

        return blankToDefault(subAfter(objName, '#', true)
                , blankToDefault(subAfter(objName, '$', true), objName));
    }

    /**
     * 简单从SpEL表达式获取属性
     *
     * @param spelStr   SpEL表达式
     * @return 返回属性
     */
    public static String toPropName(String spelStr) {
        String propName = blankToDefault(subAfter(spelStr, CharPool.DOT, true), spelStr);

        return blankToDefault(subBetween(propName, CharPool.BRACKET_START + "",
                CharPool.BRACKET_END + ""), propName);
    }

    /**
     * 简单判断字符串是否是XML字符串
     *
     * @param str 字符串
     * @return 是否为XML字符串
     */
    public static boolean isTypeXml(CharSequence str) {
        if (isEmpty(str)) return false;
        // 采用正则表达式
        Pattern pattern = Pattern.compile("^\\s*<(\\?xml|([a-zA-Z_][\\w\\-\\.]*)(\\s+[^>]*)?>.*</\\2>|\\w+[^>]*/?>)"
                , Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(str);
        if ((matcher.matches()) || (matcher.find() && 0 == matcher.start())) return true;

        String trimStr = strip(strip(trim(str), CharUtil.CR + ""), CharUtil.LF + ""); //去空格、去换行符
        return (startWith(trimStr, "<?xml") && endWith(trimStr, "?>")) ||
                (startWith(trimStr, '<') && endWith(trimStr, '>'));
    }

    /**
     * 简单判断字符串是否是JSON字符串（包含字典或列表类型）
     *
     * @param str 字符串
     * @return 是否为字典或列表类型的字符串
     */
    public static boolean isTypeJson(CharSequence str) {
        return isTypeJSONObject(str) || isTypeJSONArray(str);
    }

    /**
     * 简单判断是否为列表类型的字符串，首尾都为中括号判定为列表类型JSON字符串
     *
     * @param str 字符串
     * @return 是否为列表类型JSON字符串
     */
    public static boolean isTypeJSONArray(CharSequence str) {
        if (isBlank(str)) {
            return false;
        }
        return isWrap(trim(str), '[', ']');
    }


    /**
     * 简单判断是否为字典类型字符串，首尾都为大括号判定为字典类型JSON字符串
     *
     * @param str 字符串
     * @return 是否为字典类型JSON字符串
     */
    public static boolean isTypeJSONObject(CharSequence str) {
        if (isBlank(str)) {
            return false;
        }
        return isWrap(trim(str), '{', '}');
    }

    /**
     * 将以下命名的字符串转换为小驼峰式。
     * <p>
     * 蛇形命名（Snake Case）：使用下划线_作为单词间的分隔符，所有单词首字母小写。例如："user_login_count"
     * 烤肉命名（Kebab Case）：全部小写字母，单词之间用连字符“-”连接。如first-name、user-age
     * 驼峰命名（Camel Case）：第一个单词首字母小写，后续单词首字母大写，不使用分隔符。例如："userLoginCount"
     * 帕斯卡命名（Pascal Case）：即也称大驼峰命名（Upper Camel Case），所有单词首字母大写，不使用分隔符。例如："UserLoginCount"
     *
     * @param name
     * @return
     */
    public static String toCamelCase2(CharSequence name) {
        return lowerFirst(toCamelCase(toCamelCase(name), CharUtil.DASHED));
    }


    /**
     * 包装XML或XML片段
     *
     * @param xml
     * @return
     */
    public static String wrapXml(String xml) {
        return new StrBuilder()
                .append(DEFAULT_START_ROOT_NODE)
                .append(ReUtil.replaceAll(xml, "(\\s*<\\?xml.*\\?>)?", ""))
                .append(DEFAULT_END_ROOT_NODE)
                .toString();
    }
}
