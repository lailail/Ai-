package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 最终 YAML 剧本文档结构。
 */
@Data
public class ScriptDocument {

    /**
     * Schema 版本号。
     */
    @JsonProperty("schema_version")
    @JsonAlias("schemaVersion")
    private String schemaVersion;

    /**
     * 项目信息。
     */
    private ScriptDocumentProject project;

    /**
     * 全局故事设定手册。
     */
    @JsonProperty("story_bible")
    @JsonAlias("storyBible")
    private StoryBibleResult storyBible;

    /**
     * 剧集列表。
     */
    private List<ScriptEpisodeResult> episodes = new ArrayList<>();

    /**
     * 元信息。
     */
    private ScriptDocumentMetadata metadata;
}
