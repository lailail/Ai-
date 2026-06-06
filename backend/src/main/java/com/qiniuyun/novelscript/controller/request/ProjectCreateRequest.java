package com.qiniuyun.novelscript.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建改编项目的请求参数。
 */
@Getter
@Setter
public class ProjectCreateRequest {

    /** 项目标题。 */
    @NotBlank(message = "项目标题不能为空")
    @Size(max = 255, message = "项目标题长度不能超过 255 个字符")
    private String title;

    /** 项目简介。 */
    @Size(max = 2000, message = "项目简介长度不能超过 2000 个字符")
    private String description;
}
