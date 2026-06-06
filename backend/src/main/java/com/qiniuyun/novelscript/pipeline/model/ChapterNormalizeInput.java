package com.qiniuyun.novelscript.pipeline.model;

import java.util.List;

import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import lombok.Data;

/**
 * 章节标准化步骤的输入对象。
 */
@Data
public class ChapterNormalizeInput {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 待标准化的原始章节列表。
     */
    private List<SourceChapter> chapters;
}
