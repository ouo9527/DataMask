package com.ouo.mask.core.rule;

import com.ouo.mask.core.annotation.ModeEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/***********************************************************
 * 替换脱敏规则
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Getter
@Setter
public class ReplDesensitizationRule extends DesensitizationRule {
    // 位置，按照数组顺序从左往右进行替换
    private List<Posn> posns;
    // 剩余位置替换
    private Posn surplus;

    public ReplDesensitizationRule() {
        this.mode = ModeEnum.REPL;
    }

    //位置属性
    @Setter
    @Getter
    public static class Posn {
        // 随机还是固定值替换，默认：固定值
        private boolean fixed = true;
        // 第几位（用于posns属性时必须大于0，否则无效；而用于surplus属性，无效）
        private int i;
        // 替换值，若随机，则该属性无效；若固定值，其为空时，保持原样
        private String rv;
    }
}
