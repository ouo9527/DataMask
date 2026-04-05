package com.ouo.mask.core.rule;

import com.ouo.mask.core.annotation.Hash;
import com.ouo.mask.core.enums.ModeEnum;
import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * 哈希脱敏规则
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Getter
@Setter
public class HashDesensitizationRule extends DesensitizationRule {
    //Hash算法
    //@JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    private Hash.AlgorithmEnum algorithm = Hash.AlgorithmEnum.SM3;
    //盐
    private String salt;

    public HashDesensitizationRule() {
        this.mode = ModeEnum.HASH;
    }
}
