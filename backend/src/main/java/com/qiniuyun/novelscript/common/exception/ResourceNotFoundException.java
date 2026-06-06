package com.qiniuyun.novelscript.common.exception;

/**
 * 表示资源不存在的业务异常。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
