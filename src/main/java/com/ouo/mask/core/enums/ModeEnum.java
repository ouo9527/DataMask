package com.ouo.mask.core.enums;

import lombok.Getter;

/***********************************************************
 * 脱敏模式
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Getter
public enum ModeEnum {
    // 置空
    //gson枚举映射: @SerializedName(value = "empty", alternate = "EMPTY")
    EMPTY(10),
    // 哈希
    HASH(20),
    // 正则
    REGEX(30),
    // 替换
    REPL(40),
    // 掩盖
    MASK(50);

    private int code;

    ModeEnum(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
