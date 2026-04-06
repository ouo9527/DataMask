package com.ouo.mask.core;

import cn.hutool.core.util.ModifierUtil;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.enums.SceneEnum;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

/***********************************************************
 * 脱敏器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public interface Desensitizer {

    /**
     * key-data任意类型脱敏
     *
     * @param scene      脱敏场景(若为NULL，则模式视为ALL)
     * @param fieldName  待脱敏字段
     * @param data       待脱敏数据
     * @param annotation 注解式脱敏规则
     * @return 返回已脱敏数据
     */
    <T> T desensitized(SceneEnum scene, String fieldName, T data, Annotation annotation);

    /**
     * 按照场景进行key-val、key-vals或data对象数据脱敏
     *
     * @param scene     脱敏场景(若为NULL，则模式视为ALL)
     * @param fieldName 待脱敏字段名
     * @param data      待脱敏的集合数据
     * @return 返回已脱敏数据
     */
    default <T> T desensitized(SceneEnum scene, String fieldName, T data) {
        return this.desensitized(scene, fieldName, data, null);
    }

    /**
     * 按照场景进行单个key-val或key-vals数据脱敏
     *
     * @param scene 脱敏场景(若为NULL，则模式视为ALL)
     * @param field 待脱敏字段
     * @param data  待脱敏的数据
     * @return 返回已脱敏数据
     */
    default <T> T desensitized(SceneEnum scene, Field field, T data) {
        // 静态字段属于类属性即类成员共享，若修改后会造成共享不一致问题，其次常量字段即编译时常量，若修改后会造成不可见问题即通过get方法访问和直接访问字段，其值是不一样的，故脱敏都不建议修改
        if (null == field || ModifierUtil.isStatic(field)
                || ModifierUtil.hasModifier(field, ModifierUtil.ModifierType.FINAL)) return data;

        return this.desensitized(scene, field.getName(), data, Arrays.stream(field.getAnnotations())
                .filter(a -> a instanceof Empty || a instanceof Hash || a instanceof Regex
                        || a instanceof Repl || a instanceof Mask)
                .findAny().orElse(null));
    }


    /**
     * 按照场景进行Java Bean、Map、XML、JSON、集合等数据脱敏
     *
     * @param scene 脱敏场景(若为NULL，则模式视为ALL)
     * @param data  待脱敏的Java Bean、Map、XML、JSON、集合等数据
     * @return 返回已脱敏Java Bean或Map数据
     */
    default <T> T desensitized(SceneEnum scene, T data) {
        return this.desensitized(scene, null, data, null);
    }
}
