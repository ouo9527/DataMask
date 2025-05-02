package com.ouo.mask.handler;

import com.ouo.mask.enums.SceneEnum;

import java.lang.reflect.Field;

/***********************************************************
 * 脱敏处理器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public interface DesensitizationHandler {

    /**
     * 根据脱敏策略验证是否支持脱敏
     *
     * @param context 待脱敏对象所被使用的上下文即在那个类中使用
     * @return
     */
    boolean supports(String context);

    /**
     *  按照场景进行key-val、key-vals或data对象数据脱敏
     *
     * @param scene         场景
     * @param fieldName     待脱敏字段名
     * @param data          待脱敏的集合数据
     * @return 返回已脱敏数据
     */
    <T> T desensitized(SceneEnum scene, String fieldName, T data);

    /**
     *  按照场景进行单个key-val或key-vals数据脱敏
     *
     * @param scene         场景
     * @param field         待脱敏字段
     * @param data           待脱敏的数据
     * @return 返回已脱敏数据
     */
    <T> T desensitized(SceneEnum scene, Field field, T data);

    /**
     * 按照场景进行Java Bean或Map数据脱敏
     *
     * @param scene 场景
     * @param data  待脱敏的Java Bean或Map数据
     * @return 返回已脱敏Java Bean或Map数据
     */
    default <T> T desensitized(SceneEnum scene, T data) {
        return this.desensitized(scene, (String) null, data);
    }
}
