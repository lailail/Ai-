package com.qiniuyun.novelscript.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一 API 响应包装。
 *
 * @param <T> 响应数据类型
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    /** 请求是否成功。 */
    private final boolean success;

    /** 响应消息。 */
    private final String message;

    /** 响应数据。 */
    private final T data;

    /**
     * 构建成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "ok", data);
    }

    /**
     * 构建失败响应。
     *
     * @param message 错误描述
     * @return 统一失败响应
     */
    public static ApiResponse<Void> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
