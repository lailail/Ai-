package com.qiniuyun.novelscript.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 正式剧本渲染请求参数。
 */
@Getter
@Setter
public class ScreenplayRenderRequest {

    /** 需要渲染的剧本版本 ID。 */
    @NotNull(message = "剧本版本 ID 不能为空")
    @Min(value = 1, message = "剧本版本 ID 必须大于 0")
    private Long scriptVersionId;
}
