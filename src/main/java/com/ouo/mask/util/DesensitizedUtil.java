package com.ouo.mask.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.digest.MD5;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.rule.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/***********************************************************
 * 字段脱敏工具
 *
 * Author:   ouo
 * Date:     2024/4/26
 ***********************************************************/
@Slf4j
public abstract class DesensitizedUtil {

    /**
     * 置空模式(脱敏后不等长)：根据规则脱敏该字段的数据
     *
     * @param rule          脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String emptyDesensitized(EmptyDesensitizationRule rule, String fieldName, String data) {
        return matches(rule, fieldName, data) ? "" : data;
    }

    /**
     * 置空模式(脱敏后不等长)：根据注解中规则脱敏该字段的数据
     *
     * @param annotation    脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String emptyDesensitized(Empty annotation, String fieldName, String data) {
        if (null == annotation) return data;
        EmptyDesensitizationRule rule = new EmptyDesensitizationRule();
        rule.setField(fieldName);

        return emptyDesensitized(rule, fieldName, data);
    }

    /**
     * Hash模式(脱敏后不等长)：根据规则脱敏该字段的数据
     *
     * @param rule          脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String hashDesensitized(HashDesensitizationRule rule, String fieldName, String data) {
        if (!matches(rule, fieldName, data)) return data;
        switch (rule.getAlgorithm()) {
            case HASH256:
                return new Digester(DigestAlgorithm.SHA256)
                        .setSalt(StrUtil.trimToEmpty(rule.getSalt()).getBytes())
                        .digestHex(data);
            case MD5:
                return new MD5()
                        .setSalt(StrUtil.trimToEmpty(rule.getSalt()).getBytes())
                        .digestHex(data);
            case SM3:
            default:
                return SmUtil.sm3WithSalt(StrUtil.trimToEmpty(rule.getSalt()).getBytes())
                        .digestHex(data);
        }
    }

    /**
     * Hash模式(脱敏后不等长)：根据Hash注解中规则脱敏该字段的数据
     *
     * @param annotation    脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String hashDesensitized(Hash annotation, String fieldName, String data) {
        if (null == annotation) return data;
        HashDesensitizationRule rule = new HashDesensitizationRule();
        rule.setField(fieldName);
        rule.setAlgorithm(annotation.algorithm());
        rule.setSalt(annotation.salt());

        return desensitized(rule, fieldName, data);
    }

    /**
     * 正则模式(脱敏后可能不等长)：根据规则脱敏该字段的数据
     *
     * @param rule          脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String regexDesensitized(RegexDesensitizationRule rule, String fieldName, String data) {
        if (!matches(rule, fieldName, data)) return data;
        //Java正则特殊符号必须使用1个反斜杠进行转义，如：(\\d{3})\\d{4}(\\d{4})，yml配置文件中无需使用\转义符，只需符合正则语法即可，但properties中如Java，经验证，可一个或三个，最终都会被换成1个
        //替换值：$1####$2，如：17788485848脱敏后177####5848
        if (StrUtil.isNotBlank(rule.getPattern())) {
            return ReUtil.replaceAll(data, rule.getPattern(), rule.getRv());
        }
        return data;
    }

    /**
     * 正则模式(脱敏后不等长)：根据正则注解中规则脱敏该字段的数据
     *
     * @param annotation    脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String regexDesensitized(Regex annotation, String fieldName, String data) {
        if (null == annotation) return data;
        RegexDesensitizationRule rule = new RegexDesensitizationRule();
        rule.setField(fieldName);
        rule.setPattern(annotation.pattern());
        rule.setRv(annotation.rv());

        return desensitized(rule, fieldName, data);
    }

    /**
     * 替换模式(脱敏后等长)：根据规则脱敏该字段的数据
     *
     * @param rule          脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String replDesensitized(ReplDesensitizationRule rule, String fieldName, String data) {
        if (!matches(rule, fieldName, data)) return data;
        int index = 0;
        String val = data;
        //前几位
        List<ReplDesensitizationRule.Posn> posns = rule.getPosns();
        if (null != posns) {
            for (ReplDesensitizationRule.Posn posn : posns) {
                if (index >= val.length()) break;
                if (null == posn) continue;
                int i = posn.getI();
                val = rpv(posn, val, index, false);
                index = i;
            }
        }
        //剩余位
        ReplDesensitizationRule.Posn surplus = rule.getSurplus();
        if (null != surplus && index < val.length()) {
            return rpv(surplus, val, index, true);
        }
        return val;
    }

    /**
     * 替换模式(脱敏后等长)：根据替换注解中规则脱敏该字段的数据
     *
     * @param annotation    脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String replDesensitized(Repl annotation, String fieldName, String data) {
        if (null == annotation) return data;
        ReplDesensitizationRule rule = new ReplDesensitizationRule();
        rule.setField(fieldName);

        ReplDesensitizationRule.Posn surplus = new ReplDesensitizationRule.Posn();
        if (null != annotation.surplus()) {
            surplus.setFixed(annotation.surplus().fixed());
            surplus.setRv(annotation.surplus().rv());
            rule.setSurplus(surplus);
        }

        List<ReplDesensitizationRule.Posn> posns = new ArrayList<>();
        if (null != annotation.posns()) {
            for (Repl.Posn p : annotation.posns()) {
                if (null == p) continue;
                ReplDesensitizationRule.Posn posn = new ReplDesensitizationRule.Posn();
                posn.setI(p.i());
                posn.setFixed(p.fixed());
                posn.setRv(p.rv());

                posns.add(posn);
            }
        }
        rule.setPosns(posns);

        return replDesensitized(rule, fieldName, data);
    }

    /**
     * 掩盖模式(脱敏后等长)：根据掩盖注解中规则脱敏该字段的数据
     *
     * @param annotation 脱敏规则
     * @param fieldName  待脱敏字段名
     * @param data       待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String maskDesensitized(Mask annotation, String fieldName, String data) {
        if (null == annotation) return data;
        MaskDesensitizationRule rule = new MaskDesensitizationRule();
        rule.setField(fieldName);
        rule.setType(annotation.type());

        if (null != annotation.show()) {
            MaskDesensitizationRule.CustomShow show = new MaskDesensitizationRule.CustomShow();
            show.setPre(annotation.show().pre());
            show.setSuf(annotation.show().suf());
            rule.setShow(show);
        }
        return maskDesensitized(rule, fieldName, data);
    }

    /**
     * 掩盖模式(脱敏后等长)：根据规则脱敏该字段的数据
     * 1、姓名：默认自动根据字符长度显示，当长度小于等于2，则显示第1个字符，否则显示前2个字符
     * 2、手机号：大陆-11位、台湾-10位、香港澳门-8位。默认自动根据字符长度显示大陆-前3后4、台湾-前3后3、香港澳门-前2后2
     * 3、固话：由3～4位区号+7～8位固定数字组成。默认自动根据字符长度显示，当区号小于等于3，则显示前3后2，否则前4后2
     * 4、身份证号：由6位地址码+8位出生日期+3位顺序码+1位校验码，有15位或18位。默认显示前3后4
     * 5、地址：默认自动根据正则：(.+省)?(.+市)?(.+自治区)?(.+行政区)?(.+县)?(.+区)?.+  进行对省以下进行如：广西壮族自治区**显
     * 6、电子邮件：默认自动根据@前字符长度显示，且@后字符显示，当@前字符长度小于3，则@前字符全显示，否则显示前三位及@后
     * 7、中国大陆车牌：由1个汉字+1个字母+5～6字母和数字组成。默认显示前2后2
     * 8、银行卡：默认显示前6后4
     * 9、护照：由1位字母（护照类型）+8位数字组成。默认显示前1后3
     * 10、数值：默认显示第1位
     *
     * @param rule          脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String maskDesensitized(MaskDesensitizationRule rule, String fieldName, String data) {
        if (!matches(rule, fieldName, data)) return data;
        int len = StrUtil.length(data);
        if ((null == rule.getShow()) || (0 >= rule.getShow().getPre() && 0 >= rule.getShow().getSuf())) {
            switch (rule.getType()) {
                case FULL_NAME:
                    return StrUtil.hide(data, 2 < len ? 2 : 1, len);
                case MOBILE_PHONE: {
                    int pre = 0;
                    int suf = 0;
                    if (8 >= len) {
                        pre = suf = 2;
                    } else if (10 >= len) {
                        pre = suf = 3;
                    } else {
                        pre = 3;
                        suf = 4;
                    }
                    return StrUtil.hide(data, pre, len - suf);
                }
                case FIXED_PHONE: {
                    List<String> newData = StrUtil.splitTrim(data, '-');
                    int pre = 0;
                    int suf = 2;
                    if (2 == newData.size()) {
                        pre = StrUtil.length(newData.get(0)) + 1;
                    } else if (10 >= len) {
                        pre = 3;
                    } else {
                        pre = 4;
                    }
                    return StrUtil.hide(data, pre, len - suf);
                }
                case ID_CARD:
                    // 香港（8～9位）、澳门（7～8位）
                    if (10 > len) return StrUtil.hide(data, 0, len - 2);
                    // 台湾（10位）
                    if (10 == len) return StrUtil.hide(data, 2, len - 3);
                    return StrUtil.hide(data, 3, len - 4);
                case ADDRESS: {
                    List<String> addrs = ReUtil.getAllGroups(Pattern.compile("(.+省)?(.+市)?(.+自治区)?(.+行政区)?(.+县)?(.+区)?.+"), data, false);
                    if (CollUtil.isEmpty(addrs)) return data;
                    StringBuilder newData = new StringBuilder();
                    int i = 0;
                    if (StrUtil.isNotBlank(addrs.get(0))) {
                        newData.append(addrs.get(0));
                        i += addrs.get(0).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(1))) {
                        newData.append(StrUtil.hide(addrs.get(1), -1, addrs.get(1).length() - 1));
                        i += addrs.get(1).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(2))) {
                        newData.append(StrUtil.hide(addrs.get(2), -1, addrs.get(2).length() - 3));
                        i += addrs.get(2).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(3))) {
                        newData.append(StrUtil.hide(addrs.get(3), -1, addrs.get(3).length() - 3));
                        i += addrs.get(3).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(4))) {
                        newData.append(StrUtil.hide(addrs.get(4), -1, addrs.get(4).length() - 1));
                        i += addrs.get(4).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(5))) {
                        newData.append(StrUtil.hide(addrs.get(5), -1, addrs.get(5).length() - 1));
                        i += addrs.get(5).length();
                    }
                    return 0 == i ? StrUtil.hide(data, 7, len)
                            : newData.append(StrUtil.repeat('*', len - i)).toString();
                }
                case EMAIL: {
                    List<String> email = StrUtil.splitTrim(data, '@');
                    if (2 == email.size()) {
                        StringBuilder newData = new StringBuilder();
                        return newData
                                .append(StrUtil.hide(email.get(0), 3, StrUtil.length(email.get(0))))
                                .append('@')
                                .append(email.get(1))
                                .toString();
                    } else return data;
                }
                case CAR_LICENSE:
                    return StrUtil.hide(data, 2, len - 2);
                case BANK_CARD:
                    // 银联信用卡/Visa/Mastercard 标准信用卡/部分借记卡（一般16位）、极少数借记卡（17位）、银联借记卡（一般18～19位）、美国运通卡（AmEx）（15位）、部分Visa旧版卡（一般13～14位）
                    // 联名卡/预付卡或虚拟卡（19位以上）
                    if (16 <= len) StrUtil.hide(data, 6, len - 4);
                    if (16 > len && 13 <= len) StrUtil.hide(data, 4, len - 4);
                    return StrUtil.hide(data, 2, len - 2);
                case PASSPORT:
                    return StrUtil.hide(data, 1, len - 3);
                case NUMBER:
                    return StrUtil.hide(data, 1, len);
                default:
                    return data;
            }
        } else {
            if (0 >= rule.getShow().getSuf()) {
                return StrUtil.hide(data, rule.getShow().getPre(), len);
            }
            return StrUtil.hide(data, rule.getShow().getPre(), len - rule.getShow().getSuf());
        }
    }

    /**
     * 根据规则脱敏该字段的数据
     *
     * @param rule          脱敏规则
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏数据
     * @return 返回脱敏后数据
     */
    public static String desensitized(DesensitizationRule rule, String fieldName, String data) {
        if (StrUtil.isBlank(fieldName) || StrUtil.isBlank(data)) {
            return data;
        }
        //脱敏规则
        if (rule instanceof EmptyDesensitizationRule) {//置空(脱敏后不等长)
            return emptyDesensitized((EmptyDesensitizationRule) rule, fieldName, data);
        }
        if (rule instanceof HashDesensitizationRule) {//HASH(脱敏后不等长)
            return hashDesensitized((HashDesensitizationRule) rule, fieldName, data);
        }
        if (rule instanceof RegexDesensitizationRule) {//正则(脱敏后可能不等长)
            return regexDesensitized((RegexDesensitizationRule) rule, fieldName, data);
        }
        if (rule instanceof ReplDesensitizationRule) {//替换(脱敏后等长)
            return replDesensitized((ReplDesensitizationRule) rule, fieldName, data);
        }
        if (rule instanceof MaskDesensitizationRule) {//掩盖(脱敏后等长)
            return maskDesensitized((MaskDesensitizationRule) rule, fieldName, data);
        }
        // 根据正则进行数据特征匹配脱敏（精确度不，待定）
        return data;
    }

    /**
     * 位置所对应的值替换
     *
     * @param posn          脱敏位置
     * @param val           待脱敏数据
     * @param index         脱敏位置
     * @param fromSurplus   是否从剩余位置替换
     * @return 返回替换后的值
     */
    private static String rpv(ReplDesensitizationRule.Posn posn, String val, int index, boolean fromSurplus) {
        if (null == posn || StrUtil.isEmpty(val)) return val;
        int i = fromSurplus ? val.length() : posn.getI();
        int span = i - index >= val.length() ? val.length() - index : i - index;
        if (posn.isFixed()) {//固定值
            String rv = posn.getRv();
            if (StrUtil.isNotEmpty(rv)) {//若替换值为空保持原值
                if (span >= rv.length()) //替换值长度小于所要填充位置时，需填充
                    return StrUtil.replaceByCodePoint(val, index, i, StrUtil.repeatByLength(rv, span));
                else return StrUtil.replaceByCodePoint(val, index, i, StrUtil.subPre(rv, span));
            }
        } else {//随机值
            return StrUtil.replaceByCodePoint(val, index, i, RandomUtil.randomString(span));
        }
        return val;
    }

    /**
     * 校验脱敏是否匹配
     *
     * @param rule      脱敏规则
     * @param fieldName 待脱敏字段名
     * @param data      待脱敏数据
     * @return 返回匹配结果
     */
    private static boolean matches(DesensitizationRule rule, String fieldName, String data) {
        log.debug("校验是否不能脱敏：脱敏场景={}, 脱敏规则={}, 待脱敏字段={}", rule, fieldName);
        //通过驼峰匹配
        return !((StrUtil.isBlank(data) || null == rule || !StrUtil.equals(StrUtil.toCamelCase2(fieldName), StrUtil.toCamelCase2(rule.getField()))));
    }
}
