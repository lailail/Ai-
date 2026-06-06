package com.qiniuyun.novelscript.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 单章上下文抽取结果。
 */
@Data
public class ChapterContextResult {

    /**
     * 当前改编项目 ID。
     */
    private Long projectId;

    /**
     * 章节号。
     */
    private Integer chapterNo;

    /**
     * 章节标题。
     */
    private String chapterTitle;

    /**
     * 章节字数。
     */
    private Integer wordCount;

    /**
     * 章节摘要。
     */
    private String summary;

    /**
     * 本章主要人物。
     */
    private List<String> characters = new ArrayList<>();

    /**
     * 本章主要地点。
     */
    private List<String> locations = new ArrayList<>();

    /**
     * 本章关键事件。
     */
    private List<String> events = new ArrayList<>();

    /**
     * 本章主要冲突。
     */
    private List<String> conflicts = new ArrayList<>();

    /**
     * 本章情绪变化。
     */
    @JsonProperty("emotion_changes")
    @JsonAlias("emotionChanges")
    private List<String> emotionChanges = new ArrayList<>();

    /**
     * 本章伏笔与线索。
     */
    private List<String> foreshadowing = new ArrayList<>();

    /**
     * 本章关键对白。
     */
    @JsonProperty("key_dialogues")
    @JsonAlias("keyDialogues")
    private List<String> keyDialogues = new ArrayList<>();

    /**
     * 结果对应的来源引用。
     */
    @JsonProperty("source_refs")
    @JsonAlias("sourceRefs")
    private List<String> sourceRefs = new ArrayList<>();
}
