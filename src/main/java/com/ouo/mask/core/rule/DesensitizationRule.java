package com.ouo.mask.core.rule;

import com.ouo.mask.core.annotation.ModeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/***********************************************************
 * 脱敏规则
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Setter
@Getter
@ToString
public abstract class DesensitizationRule {
    //字段
    protected String field;
    //模式
    //@JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    protected ModeEnum mode = ModeEnum.MASK;
}
