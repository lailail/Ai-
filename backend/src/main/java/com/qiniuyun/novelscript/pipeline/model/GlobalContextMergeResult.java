package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 多章节上下文合并后的全局上下文结果。
 */
@Data
public class GlobalContextMergeResult {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 多章合并后的摘要。
     */
    private String summary;

    /**
     * 合并去重后的角色列表。
     */
    private List<String> characters = new ArrayList<>();

    /**
     * 合并去重后的地点列表。
     */
    private List<String> locations = new ArrayList<>();

    /**
     * 按时间顺序整理后的事件线。
     */
    private List<String> timeline = new ArrayList<>();

    /**
     * 角色关系或关系变化描述。
     */
    private List<String> relationships = new ArrayList<>();

    /**
     * 全局核心冲突列表。
     */
    private List<String> conflicts = new ArrayList<>();

    /**
     * 参与本次合并的上下文来源引用。
     */
    @JsonProperty("source_context_refs")
    @JsonAlias("sourceContextRefs")
    private List<String> sourceContextRefs = new ArrayList<>();
}
