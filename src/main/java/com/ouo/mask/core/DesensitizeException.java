package com.ouo.mask.core;

/***********************************************************
 * 自定义脱敏异常
 *
 * Author:   ouo
 * Date:     2026/4/5
 ***********************************************************/
public class DesensitizeException extends RuntimeException {

    public DesensitizeException() {
        super();
    }

    public DesensitizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DesensitizeException(String message) {
        super(message);
    }

    public DesensitizeException(Throwable cause) {
        super(cause);
    }
}
