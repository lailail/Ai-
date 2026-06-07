package com.qiniuyun.novelscript.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 正式剧本同步回 YAML 的请求参数。
 */
@Getter
@Setter
public class ScreenplaySyncYamlRequest {

    /** 需要同步的原始剧本版本 ID。 */
    @NotNull(message = "剧本版本 ID 不能为空")
    @Min(value = 1, message = "剧本版本 ID 必须大于 0")
    private Long scriptVersionId;

    /** 新生成的剧本版本标题。 */
    @NotBlank(message = "新版本标题不能为空")
    private String title;

    /** 用户编辑后的正式剧本 Markdown 内容。 */
    @NotBlank(message = "正式剧本内容不能为空")
    private String markdownContent;
}
