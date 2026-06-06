package com.qiniuyun.novelscript.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 校验 YAML 内容时使用的请求参数。
 */
@Getter
@Setter
public class ScriptValidateRequest {

    /** 待校验的 YAML 原文。 */
    @NotBlank(message = "YAML 内容不能为空")
    private String yamlContent;
}
