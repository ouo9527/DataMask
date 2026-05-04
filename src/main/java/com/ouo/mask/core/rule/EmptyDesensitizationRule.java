package com.ouo.mask.core.rule;

import com.ouo.mask.core.annotation.ModeEnum;

/***********************************************************
 * 置空脱敏规则
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
public class EmptyDesensitizationRule extends DesensitizationRule {
    public EmptyDesensitizationRule() {
        this.mode = ModeEnum.EMPTY;
    }
}
