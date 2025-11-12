package com.ouo.mask.rule;

import com.ouo.mask.annotation.Hash;
import com.ouo.mask.enums.ModeEnum;
import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * 哈希脱敏规则
 * Author:   刘春
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
