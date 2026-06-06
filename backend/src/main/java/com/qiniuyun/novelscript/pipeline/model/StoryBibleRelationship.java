package com.qiniuyun.novelscript.pipeline.model;

import lombok.Data;

/**
 * Story Bible 中的角色关系。
 */
@Data
public class StoryBibleRelationship {

    /**
     * 关系起点角色 ID。
     */
    private String from;

    /**
     * 关系终点角色 ID。
     */
    private String to;

    /**
     * 关系类型。
     */
    private String type;

    /**
     * 关系说明。
     */
    private String description;
}
