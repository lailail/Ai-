package com.qiniuyun.novelscript.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 保存剧本 YAML 新版本时使用的请求参数。
 */
@Getter
@Setter
public class ScriptVersionSaveRequest {

    /** 用户为当前版本填写的标题。 */
    @Size(max = 255, message = "版本标题长度不能超过 255 个字符")
    private String title;

    /** 用户编辑后的 YAML 原文。 */
    @NotBlank(message = "YAML 内容不能为空")
    private String yamlContent;
}
