package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 剧本单集结果。
 */
@Data
public class ScriptEpisodeResult {

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
     * 当前剧集的场景结果列表。
     */
    private List<ScriptSceneResult> scenes = new ArrayList<>();

    /**
     * 从大纲结果构造剧集结果。
     *
     * @param id 剧集 ID
     * @param title 剧集标题
     * @param premise 剧集摘要
     * @param sourceRefs 来源章节引用
     * @param scenes 场景列表
     * @return 剧集结果对象
     */
    public static ScriptEpisodeResult fromOutline(
        String id,
        String title,
        String premise,
        List<String> sourceRefs,
        List<ScriptSceneResult> scenes
    ) {
        ScriptEpisodeResult result = new ScriptEpisodeResult();
        result.setId(id);
        result.setTitle(title);
        result.setPremise(premise);
        result.setSourceRefs(sourceRefs == null ? List.of() : sourceRefs);
        result.setScenes(scenes == null ? List.of() : scenes);
        return result;
    }
}
