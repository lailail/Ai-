package com.qiniuyun.novelscript.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 健康检查结果。
 */
@Getter
@AllArgsConstructor
public class HealthResponse {

    /** 应用健康状态。 */
    private final String status;

    /** 应用名称。 */
    private final String application;
}
