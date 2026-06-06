package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Story Bible 中的时间线事件。
 */
@Data
public class StoryBibleTimelineEvent {

    /**
     * 事件唯一标识。
     */
    private String id;

    /**
     * 事件排序。
     */
    private Integer order;

    /**
     * 事件摘要。
     */
    private String summary;

    /**
     * 事件对应的来源引用。
     */
    @JsonProperty("source_refs")
    @JsonAlias("sourceRefs")
    private List<String> sourceRefs = new ArrayList<>();
}
