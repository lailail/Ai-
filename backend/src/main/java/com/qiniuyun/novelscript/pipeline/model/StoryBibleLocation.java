package com.qiniuyun.novelscript.pipeline.model;

import lombok.Data;

/**
 * Story Bible 中的地点设定。
 */
@Data
public class StoryBibleLocation {

    /**
     * 地点唯一标识。
     */
    private String id;

    /**
     * 地点名称。
     */
    private String name;

    /**
     * 地点描述。
     */
    private String description;
}
