package com.ouo.mask.core.enums;

import lombok.Getter;

/***********************************************************
 * 脱敏场景
 *
 * Author:   ouo
 * Date:     2023/1/29
 ***********************************************************/
@Getter
public enum SceneEnum {
    // WEB
    WEB(10),
    // LOG
    LOG(20),
    // WEB and LOG
    ALL(0);

    private int code;

    SceneEnum(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
