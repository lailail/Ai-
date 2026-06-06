package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineEpisode;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineResult;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineScene;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 负责基于 Story Bible 规划剧本大纲。
 */
@Slf4j
@Component
public class ScriptOutlinePlanStep {

    public static final String SYSTEM_PROMPT = """
        你是短剧与影视剧本大纲规划助手。
        请严格根据输入内容输出 JSON，不要输出 Markdown，不要补充额外解释。
        只保留以下字段：episodes。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public ScriptOutlinePlanStep(
        AiChatAdapter aiChatAdapter,
        PromptTemplateService promptTemplateService,
        ObjectMapper objectMapper
    ) {
        this.aiChatAdapter = aiChatAdapter;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 规划剧本大纲。
     *
     * @param projectId 当前项目 ID
     * @param storyBible Story Bible 结果
     * @param chapterContexts 章节上下文列表
     * @return 剧本大纲结果
     */
    public ScriptOutlineResult execute(
        Long projectId,
        StoryBibleResult storyBible,
        List<ChapterContextResult> chapterContexts
    ) {
        validateInput(projectId, storyBible, chapterContexts);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("storyBible", writeAsJson(storyBible));
        variables.put("chapterContexts", writeAsJson(chapterContexts));
        String userPrompt = promptTemplateService.render("script-outline-plan", variables);

        log.info("【大纲规划】开始规划剧本大纲，项目ID：{}，章节数：{}", projectId, chapterContexts.size());
        String aiResponse = aiChatAdapter.chat(SYSTEM_PROMPT, userPrompt);
        ScriptOutlineResult result = parseResponse(projectId, aiResponse);
        log.info("【大纲规划】规划完成，项目ID：{}，剧集数：{}", projectId, result.getEpisodes().size());
        return result;
    }

    private void validateInput(Long projectId, StoryBibleResult storyBible, List<ChapterContextResult> chapterContexts) {
        if (projectId == null) {
            throw new IllegalArgumentException("规划剧本大纲时项目ID不能为空。");
        }
        if (storyBible == null) {
            throw new IllegalArgumentException("规划剧本大纲时 Story Bible 不能为空。");
        }
        if (CollectionUtils.isEmpty(chapterContexts)) {
            throw new IllegalArgumentException("规划剧本大纲时章节上下文不能为空。");
        }
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("大纲规划输入无法序列化为 JSON。", exception);
        }
    }

    private ScriptOutlineResult parseResponse(Long projectId, String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("剧本大纲规划结果为空。");
        }

        try {
            ScriptOutlineResult result = objectMapper.readValue(aiResponse, ScriptOutlineResult.class);
            result.setProjectId(projectId);
            result.setEpisodes(safeEpisodes(result.getEpisodes()));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("剧本大纲规划结果无法解析为 JSON。", exception);
        }
    }

    private List<ScriptOutlineEpisode> safeEpisodes(List<ScriptOutlineEpisode> episodes) {
        if (episodes == null) {
            return new ArrayList<>();
        }

        return episodes.stream()
            .filter(Objects::nonNull)
            .map(this::sanitizeEpisode)
            .toList();
    }

    private ScriptOutlineEpisode sanitizeEpisode(ScriptOutlineEpisode episode) {
        episode.setSourceRefs(safeStringList(episode.getSourceRefs()));
        episode.setScenes(safeScenes(episode.getScenes()));
        return episode;
    }

    private List<ScriptOutlineScene> safeScenes(List<ScriptOutlineScene> scenes) {
        if (scenes == null) {
            return new ArrayList<>();
        }

        return scenes.stream()
            .filter(Objects::nonNull)
            .map(scene -> {
                scene.setSourceRefs(safeStringList(scene.getSourceRefs()));
                scene.setCharacters(safeStringList(scene.getCharacters()));
                return scene;
            })
            .toList();
    }

    private List<String> safeStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }
}
