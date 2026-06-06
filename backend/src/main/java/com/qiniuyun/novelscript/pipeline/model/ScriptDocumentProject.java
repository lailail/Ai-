package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * YAML 剧本中的项目信息。
 */
@Data
public class ScriptDocumentProject {

    /**
     * 项目 ID。
     */
    private String id;

    /**
     * 项目标题。
     */
    private String title;

    /**
     * 来源章节编号列表。
     */
    @JsonProperty("source_chapters")
    @JsonAlias("sourceChapters")
    private List<Integer> sourceChapters = new ArrayList<>();

    /**
     * 改编模式。
     */
    @JsonProperty("adaptation_mode")
    @JsonAlias("adaptationMode")
    private String adaptationMode;
}
