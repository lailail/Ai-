package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Story Bible 结构化结果。
 */
@Data
public class StoryBibleResult {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 角色设定列表。
     */
    private List<StoryBibleCharacter> characters = new ArrayList<>();

    /**
     * 角色关系列表。
     */
    private List<StoryBibleRelationship> relationships = new ArrayList<>();

    /**
     * 地点设定列表。
     */
    private List<StoryBibleLocation> locations = new ArrayList<>();

    /**
     * 时间线事件列表。
     */
    private List<StoryBibleTimelineEvent> timeline = new ArrayList<>();

    /**
     * 核心冲突列表。
     */
    private List<StoryBibleConflict> conflicts = new ArrayList<>();

    /**
     * 伏笔列表。
     */
    private List<StoryBibleForeshadowing> foreshadowing = new ArrayList<>();

    /**
     * 改编策略建议。
     */
    @JsonProperty("adaptation_strategy")
    @JsonAlias("adaptationStrategy")
    private List<String> adaptationStrategy = new ArrayList<>();
}
