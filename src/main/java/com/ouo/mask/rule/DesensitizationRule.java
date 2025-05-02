package com.ouo.mask.rule;

import com.ouo.mask.enums.ModeEnum;
import com.ouo.mask.enums.SceneEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/***********************************************************
 * 脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Setter
@Getter
@ToString
public abstract class DesensitizationRule {
    //场景
    //@JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    protected SceneEnum scene;
    //字段
    protected String field;
    //模式
    //@JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    protected ModeEnum mode;
}
