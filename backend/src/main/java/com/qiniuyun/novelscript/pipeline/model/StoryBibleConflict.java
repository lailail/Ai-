package com.qiniuyun.novelscript.pipeline.model;

import lombok.Data;

/**
 * Story Bible 中的核心冲突。
 */
@Data
public class StoryBibleConflict {

    /**
     * 冲突唯一标识。
     */
    private String id;

    /**
     * 冲突摘要。
     */
    private String summary;
}
