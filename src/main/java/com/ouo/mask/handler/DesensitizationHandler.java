package com.ouo.mask.handler;

import com.ouo.mask.enums.SceneEnum;

/***********************************************************
 * TODO:     脱敏处理器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public interface DesensitizationHandler {

    /**
     * todo: 根据脱敏策略验证是否支持脱敏
     *
     * @param context 待脱敏对象所被使用的上下文即在那个类中使用
     * @param data    待脱敏数据
     * @return
     */
    <T> boolean supports(String context, T data);

    /**
     * todo： 在上下文内按照场景进行数据脱敏
     *
     * @param context   待脱敏对象所被使用的上下文即在那个类中使用
     * @param scene     场景
     * @param fieldName 待脱敏字段，若不为空，则在待脱敏数据中查找该字段并脱敏，否则脱敏整个待脱敏数据
     * @param data      待脱敏的数据
     * @return 返回已脱敏数据
     */
    <T> T desensitized(String context, SceneEnum scene, String fieldName, T data);
}
