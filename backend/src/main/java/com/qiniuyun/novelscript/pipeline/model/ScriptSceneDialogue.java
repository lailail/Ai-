package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 场景对白结构。
 */
@Data
public class ScriptSceneDialogue {

    /**
     * 对白角色 ID。
     */
    @JsonProperty("character_id")
    @JsonAlias("characterId")
    private String characterId;

    /**
     * 括号提示。
     */
    private String parenthetical;

    /**
     * 对白台词。
     */
    private String line;

    /**
     * 潜台词。
     */
    private String subtext;
}
