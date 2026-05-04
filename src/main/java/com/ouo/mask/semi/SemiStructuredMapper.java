package com.ouo.mask.semi;

import com.ouo.mask.util.StrUtil;

/***********************************************************
 * 半结构化(如：JSON/XML)映射器
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public interface SemiStructuredMapper {

    /**
     * 将半结构化(如：JSON/XML)字符串转Java Bean
     *
     * @param str 半结构化(如：JSON/XML)字符串
     * @param valueType Bean类型
     * @return 返回Bean实例
     */
    <T> T toBean(String str, Class<T> valueType);

    /**
     * 获取具体半结构化数据映射器
     *
     * @param type 半结构化(如：JSON/XML)类型
     * @return 返回半结构化数据映射器
     */
    <T> T getMapper(SemiStructType type);

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param str 半结构化字符串
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    default String toString(String str) {
        return this.toString(str, StrUtil.isTypeJson(str) ? SemiStructType.JSON : SemiStructType.XML);
    }

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj 任意对象
     * @param type 半结构化类型
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    default String toString(Object obj, SemiStructType type) {
        return this.toString(obj, type, null);
    }

    /**
     * 将对象转半结构化(如：JSON/XML)字符串
     *
     * @param obj 任意对象
     * @param type 半结构化类型
     * @param rootName 根节点名称（只针对像XML结构才有用）
     * @return 返回半结构化(如 ： JSON / XML)字符串
     */
    String toString(Object obj, SemiStructType type, String rootName);

    /**
     * 将对象转具有格式化的半结构化(如：JSON/XML)字符串
     *
     * @param obj 任意对象
     * @param type 半结构化类型
     * @return 返回具有格式化的半结构化(如 ： JSON / XML)字符串
     */
    default String toPrettyString(Object obj, SemiStructType type) {
        return this.toPrettyString(obj, type, null);
    }

    /**
     * 将对象转具有格式化的半结构化(如：JSON/XML)字符串
     *
     * @param obj 任意对象
     * @param type 半结构化类型
     * @param rootName 根节点名称（只针对像XML结构才有用）
     * @return 返回具有格式化的半结构化(如 ： JSON / XML)字符串
     */
    String toPrettyString(Object obj, SemiStructType type, String rootName);
}
