package com.ouo.mask.core.rule;

import com.ouo.mask.core.annotation.ModeEnum;
import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * 正则脱敏规则
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Setter
@Getter
public class RegexDesensitizationRule extends DesensitizationRule {
    // 正则表达式
    private String pattern;
    // 替换值
    private String rv;

    public RegexDesensitizationRule() {
        this.mode = ModeEnum.REGEX;
    }
}
