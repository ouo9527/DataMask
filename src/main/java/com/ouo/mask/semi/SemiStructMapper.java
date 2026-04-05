package com.ouo.mask.semi;

import java.util.Properties;

/***********************************************************
 * 半结构化(如：JSON/XML)映射器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public interface SemiStructMapper {

    /**
     * 将半结构化(如：JSON/XML)字符串转Java Bean
     *
     * @param str       半结构化(如：JSON/XML)字符串
     * @param valueType Bean类型
     * @return 返回Bean实例
     */
    <T> T toBean(String str, Class<T> valueType);

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj  任意对象
     * @param type 半结构化类型
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    default String toString(Object obj, SemiStructType type) {
        return this.toString(obj, type, null);
    }

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj  任意对象
     * @param type 半结构化类型
     * @param prop 上下文属性
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    default String toString(Object obj, SemiStructType type, Properties prop) {
        return this.toString(obj, type, null, prop);
    }

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj      任意对象
     * @param type     半结构化类型
     * @param rootName 根节点名称（只针对像XML结构才有用）
     * @param prop     上下文属性
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    String toString(Object obj, SemiStructType type, String rootName, Properties prop);

    /**
     * 将对象转具有格式化的半结构化(如：JSON/XML)字符串
     *
     * @param obj  任意对象
     * @param type 半结构化类型
     * @return 返回具有格式化的半结构化(如 ： JSON / XML)字符串
     */
    default String toPrettyString(Object obj, SemiStructType type) {
        return this.toPrettyString(obj, type, null);
    }

    /**
     * 将对象转具有格式化的半结构化(如：JSON/XML)字符串
     *
     * @param obj  任意对象
     * @param type 半结构化类型
     * @param prop 上下文属性
     * @return 返回具有格式化的半结构化(如 ： JSON / XML)字符串
     */
    default String toPrettyString(Object obj, SemiStructType type, Properties prop) {
        return this.toPrettyString(obj, type, null, prop);
    }

    /**
     * 将对象转具有格式化的半结构化(如：JSON/XML)字符串
     *
     * @param obj      任意对象
     * @param type     半结构化类型
     * @param rootName 根节点名称（只针对像XML结构才有用）
     * @param prop     上下文属性
     * @return 返回具有格式化的半结构化(如 ： JSON / XML)字符串
     */
    String toPrettyString(Object obj, SemiStructType type, String rootName, Properties prop);
}
