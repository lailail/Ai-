package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 单场剧本结果。
 */
@Data
public class ScriptSceneResult {

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
     * 场景来源章节引用。
     */
    @JsonProperty("source_refs")
    @JsonAlias("sourceRefs")
    private List<String> sourceRefs = new ArrayList<>();

    /**
     * 本场涉及的角色 ID 列表。
     */
    private List<String> characters = new ArrayList<>();

    /**
     * 动作说明列表。
     */
    private List<String> actions = new ArrayList<>();

    /**
     * 节拍列表。
     */
    private List<ScriptSceneBeat> beats = new ArrayList<>();

    /**
     * 对白列表。
     */
    private List<ScriptSceneDialogue> dialogue = new ArrayList<>();

    /**
     * 转场提示。
     */
    private String transition;

    /**
     * 场景备注。
     */
    private ScriptSceneNotes notes;
}
