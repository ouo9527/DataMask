package com.ouo.mask.properties;

import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * 正则脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Setter
@Getter
public class RegexDesensitizationRule extends DesensitizationRule {
    // 正则表达式
    private String pattern;
    // 替换值
    private String rv;
}
