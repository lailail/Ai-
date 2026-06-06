package com.qiniuyun.novelscript.pipeline.model;

import java.util.List;

import lombok.Data;

/**
 * 章节标准化步骤的输出对象。
 */
@Data
public class ChapterNormalizeResult {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 参与标准化的章节数量。
     */
    private Integer chapterCount;

    /**
     * 标准化后的章节列表。
     */
    private List<NormalizedChapter> normalizedChapters;

    /**
     * 所有章节的总字数。
     */
    private Integer totalWordCount;
}
