package com.ouo.mask.rule;

import com.ouo.mask.enums.ModeEnum;

/***********************************************************
 * 置空脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public class EmptyDesensitizationRule extends DesensitizationRule {
    public EmptyDesensitizationRule() {
        this.mode = ModeEnum.EMPTY;
    }
}
