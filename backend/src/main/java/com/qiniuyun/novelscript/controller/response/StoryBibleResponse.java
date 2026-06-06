package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.StoryBible;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleConflict;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleForeshadowing;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleLocation;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleRelationship;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleTimelineEvent;
import java.util.List;
import lombok.Data;

/**
 * Story Bible 查询接口响应对象。
 */
@Data
public class StoryBibleResponse {

    /**
     * 项目 ID。
     */
    private Long projectId;

    /**
     * Story Bible 快照 ID。
     */
    private Long storyBibleId;

    /**
     * Story Bible 版本号。
     */
    private Integer versionNo;

    /**
     * 角色列表。
     */
    private List<StoryBibleCharacter> characters;

    /**
     * 角色关系列表。
     */
    private List<StoryBibleRelationship> relationships;

    /**
     * 地点列表。
     */
    private List<StoryBibleLocation> locations;

    /**
     * 时间线列表。
     */
    private List<StoryBibleTimelineEvent> timeline;

    /**
     * 核心冲突列表。
     */
    private List<StoryBibleConflict> conflicts;

    /**
     * 伏笔列表。
     */
    private List<StoryBibleForeshadowing> foreshadowing;

    /**
     * 改编策略建议。
     */
    private List<String> adaptationStrategy;

    /**
     * 根据 Story Bible 快照和结构化结果构建响应对象。
     *
     * @param entity Story Bible 快照实体
     * @param result Story Bible 结构化结果
     * @return 接口响应对象
     */
    public static StoryBibleResponse from(StoryBible entity, StoryBibleResult result) {
        StoryBibleResponse response = new StoryBibleResponse();
        response.setProjectId(entity.getProjectId());
        response.setStoryBibleId(entity.getId());
        response.setVersionNo(entity.getVersionNo());
        response.setCharacters(result.getCharacters());
        response.setRelationships(result.getRelationships());
        response.setLocations(result.getLocations());
        response.setTimeline(result.getTimeline());
        response.setConflicts(result.getConflicts());
        response.setForeshadowing(result.getForeshadowing());
        response.setAdaptationStrategy(result.getAdaptationStrategy());
        return response;
    }
}
