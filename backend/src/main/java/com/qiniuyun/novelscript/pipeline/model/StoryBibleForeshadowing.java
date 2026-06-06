package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Story Bible 中的伏笔设定。
 */
@Data
public class StoryBibleForeshadowing {

    /**
     * 伏笔唯一标识。
     */
    private String id;

    /**
     * 伏笔铺垫内容。
     */
    private String setup;

    /**
     * 伏笔回收提示。
     */
    @JsonProperty("payoff_hint")
    @JsonAlias("payoffHint")
    private String payoffHint;

    /**
     * 伏笔来源引用。
     */
    @JsonProperty("source_refs")
    @JsonAlias("sourceRefs")
    private List<String> sourceRefs = new ArrayList<>();
}
