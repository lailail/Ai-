package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 单集大纲结果。
 */
@Data
public class ScriptOutlineEpisode {

    /**
     * 剧集 ID。
     */
    private String id;

    /**
     * 剧集标题。
     */
    private String title;

    /**
     * 剧集前提摘要。
     */
    private String premise;

    /**
     * 剧集来源章节引用。
     */
    @JsonProperty("source_refs")
    @JsonAlias("sourceRefs")
    private List<String> sourceRefs = new ArrayList<>();

    /**
     * 本集场景规划列表。
     */
    private List<ScriptOutlineScene> scenes = new ArrayList<>();
}
