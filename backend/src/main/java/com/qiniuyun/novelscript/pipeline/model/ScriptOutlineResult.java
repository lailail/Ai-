package com.qiniuyun.novelscript.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 剧本大纲规划结果。
 */
@Data
public class ScriptOutlineResult {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 分集大纲列表。
     */
    private List<ScriptOutlineEpisode> episodes = new ArrayList<>();
}
