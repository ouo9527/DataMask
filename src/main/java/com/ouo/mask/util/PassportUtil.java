package com.ouo.mask.util;

import cn.hutool.core.util.ReUtil;

import java.util.regex.Pattern;

/***********************************************************
 * 护照工具
 *
 * Author:   ouo
 * Date:     2024/11/28
 ***********************************************************/
public final class PassportUtil {

    public static final Pattern E_PASSPORT = Pattern.compile("^(1[45]|E[A-Z])\\d{7}$|^[E|G]\\d{8}$"); //普通电子护照
    public static final Pattern S_PASSPORT = Pattern.compile("^S\\d{7,8}$|^SE\\d{7}$"); //务电子护照
    public static final Pattern D_PASSPORT = Pattern.compile("^D\\d{8}$|^DE\\d{7}$"); //外交护照
    public static final Pattern P_PASSPORT = Pattern.compile("^P\\d{8}$|^PE\\d{7}$"); //因公普通护照

    public static final Pattern C_PASSPORT = Pattern.compile("^[W|C]\\d{8}$|^C[A-Z]\\d{7}$"); //大陆往来港澳通行证
    public static final Pattern L_PASSPORT = Pattern.compile("^[T|L]\\d{8}$"); //大陆往来台湾通行证
    public static final Pattern H_PASSPORT = Pattern.compile("^[K|H]\\d{8}$"); //香港回乡通行证
    public static final Pattern M_PASSPORT = Pattern.compile("^[M]\\d{8}$"); //澳门回乡通行证
    public static final Pattern TW_PASSPORT = Pattern.compile("^\\d{8}$|^\\d{10}$|^\\d{18}$"); //台湾台胞证

    private PassportUtil() {
    }

    /**
     * 是否有效护照号，忽略大陆（G因私普照/P因公普照/S公务护照/D外交护照）、港澳通行证（H/M）的大小写<br>
     * 如果身份证号码中含有空格始终返回｛@code false｝
     *
     * @param passport 护照号，支持大陆和港澳台，以及通行证
     * @return 是否有效
     */
    public static boolean isValidPassport(String passport) {
        return isValidEPassport(passport)
                || isValidSPassport(passport)
                || isValidDPassport(passport)
                || isValidPPassport(passport)
                || isValidCPassport(passport)
                || isValidLPassport(passport)
                || isValidHPassport(passport)
                || isValidMPassport(passport)
                || isValidTWPassport(passport);
    }

    /**
     * 验证普通电子护照，旧版：E+8位数字或G+8位数字或 14/15开头+7位数字，新版：E+字母（A-Z，除1和 O）+7位数字<br>
     * 如果普通电子护照中含有空格始终返回｛@code false｝
     *
     * @param passport 普通电子护照
     * @return 是否有效
     */
    public static boolean isValidEPassport(String passport) {
        return ReUtil.isMatch(E_PASSPORT, passport);
    }

    /**
     * 验证公务护照，旧版：S+7位数字或S+8位数字，新版：SE+7位数字<br>
     * 如果公务护照中含有空格始终返回 {@code false}
     *
     * @param passport 公务护照
     * @return 是否有效
     */
    public static boolean isValidSPassport(String passport) {
        return ReUtil.isMatch(S_PASSPORT, passport);
    }

    /**
     * 验证因公普通护照，旧版：P+8位数字，新版：PE+7位数字<br>
     * 如果因公普通护照中含有空格始终返回 {@code false}
     *
     * @param passport 因公普通护照
     * @return 是否有效
     */
    public static boolean isValidPPassport(String passport) {
        return ReUtil.isMatch(P_PASSPORT, passport);
    }

    /**
     * 验证外交护照，旧版：D+8位数字，新版：DE+7位数字<br>
     * 如果外交护照中含有空格始终返回 {@code false}
     *
     * @param passport 外交护照
     * @return 是否有效
     */
    public static boolean isValidDPassport(String passport) {
        return ReUtil.isMatch(D_PASSPORT, passport);
    }

    /**
     * 验证香港回乡证（即香港来往大陆通行证），旧版：K+8位数字，新版：H+8位数字<br>
     * 如果香港回乡证中含有空格始终返回｛@code false｝
     *
     * @param passport 香港回乡证
     * @return 是否有效
     */
    public static boolean isValidHPassport(String passport) {
        return ReUtil.isMatch(H_PASSPORT, passport);
    }

    /**
     * 验证澳门回乡证（即澳门来往大陆通行证），M+8位数字<br>
     * 如果澳门回乡证中含有空格始终返回 {@code false}
     *
     * @param passport 澳门回乡证
     * @return 是否有效
     */
    public static boolean isValidMPassport(String passport) {
        return ReUtil.isMatch(M_PASSPORT, passport);
    }

    /**
     * 验证港澳通行证（即大陆往来港澳通行证），C+8位数字<br>
     * 如果港澳通行证中含有空格始终返回 {@code false}
     *
     * @param passport 港澳通行证
     * @return 是否有效
     */
    public static boolean isValidCPassport(String passport) {
        return ReUtil.isMatch(C_PASSPORT, passport);
    }

    /**
     * 验证台湾台胞证（即台湾来往大陆通行证），8位数字或10位数字或18位数字<br>
     * 如果台胞证中含有空格始终返回
     * {@code false}
     *
     * @param passport AleiF
     * @return 是否有效
     */
    public static boolean isValidTWPassport(String passport) {
        return ReUtil.isMatch(TW_PASSPORT, passport);
    }

    /**
     * 验证大陆往来台湾通行证，旧版：T+8位数字，新版：L+8位数字<br>
     * 如果台湾通行证中含有空格始终返回 {@code false}
     *
     * @param passport 台湾通行证
     * @return 是否有效
     */
    public static boolean isValidLPassport(String passport) {
        return ReUtil.isMatch(L_PASSPORT, passport);
    }
}
