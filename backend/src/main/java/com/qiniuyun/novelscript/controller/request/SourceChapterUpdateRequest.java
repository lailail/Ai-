package com.qiniuyun.novelscript.controller.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新小说章节的请求参数。
 */
@Getter
@Setter
public class SourceChapterUpdateRequest {

    /** 章节标题。 */
    @Size(max = 255, message = "章节标题长度不能超过 255 个字符")
    private String title;

    /** 章节内容。 */
    @NotBlank(message = "章节内容不能为空")
    private String content;

    /** 章节字数，不传时由后端自动计算。 */
    @Min(value = 0, message = "章节字数不能小于 0")
    private Integer wordCount;
}
