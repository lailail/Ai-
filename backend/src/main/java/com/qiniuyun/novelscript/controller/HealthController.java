package com.qiniuyun.novelscript.controller;

import com.qiniuyun.novelscript.common.response.ApiResponse;
import com.qiniuyun.novelscript.controller.response.HealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 返回当前后端的基本健康状态。
     *
     * @return 健康检查响应
     */
    @GetMapping
    public ApiResponse<HealthResponse> getHealth() {
        log.info("收到健康检查请求，返回应用状态：UP");
        return ApiResponse.success(new HealthResponse("UP", "novel-script-backend"));
    }
}
