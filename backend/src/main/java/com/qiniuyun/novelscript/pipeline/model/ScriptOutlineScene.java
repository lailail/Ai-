package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 场景规划结果。
 */
@Data
public class ScriptOutlineScene {

    /**
     * 场景 ID。
     */
    private String id;

    /**
     * 场景标题行。
     */
    private String slugline;

    /**
     * 场景目的。
     */
    private String purpose;

    /**
     * 场景核心冲突。
     */
    private String conflict;

    /**
     * 场景来源章节引用。
     */
    @JsonProperty("source_refs")
    @JsonAlias("sourceRefs")
    private List<String> sourceRefs = new ArrayList<>();

    /**
     * 本场涉及的角色 ID 列表。
     */
    private List<String> characters = new ArrayList<>();
}
