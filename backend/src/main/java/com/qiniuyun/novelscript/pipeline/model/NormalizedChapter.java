package com.qiniuyun.novelscript.pipeline.model;

import lombok.Data;

/**
 * 标准化后的章节对象。
 */
@Data
public class NormalizedChapter {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 章节号。
     */
    private Integer chapterNo;

    /**
     * 标准化后的章节标题。
     */
    private String title;

    /**
     * 标准化后的章节正文。
     */
    private String content;

    /**
     * 清洗后的字数统计。
     */
    private Integer wordCount;
}
