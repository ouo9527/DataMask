package com.ouo.mask.util;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.ReUtil;

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
     * 判断字符串是否是JSON字符串（包含JSONObject或JSONArray类型）
     *
     * @param str 字符串
     * @return 是否为JSONObject或JSONArray类型的字符串
     */
    public static boolean isTypeJson(CharSequence str) {
        return isTypeJSONObject(str) || isTypeJSONArray(str);
    }

    /**
     * 判断是否为JSONArray类型的字符串，首尾都为中括号判定为JSONArray字符串
     *
     * @param str 字符串
     * @return 是否为JSONArray类型字符串
     */
    public static boolean isTypeJSONArray(CharSequence str) {
        if (isBlank(str)) {
            return false;
        }
        return StrUtil.isWrap(StrUtil.trim(str), '[', ']');
    }


    /**
     * 判断是否为JSONObject类型字符串，首尾都为大括号判定为JSONObject字符串
     *
     * @param str 字符串
     * @return 是否为JSON字符串
     */
    public static boolean isTypeJSONObject(CharSequence str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        return StrUtil.isWrap(StrUtil.trim(str), '{', '}');
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
