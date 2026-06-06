package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.GlobalContextMergeResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleConflict;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleForeshadowing;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleLocation;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleRelationship;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleTimelineEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 负责基于全局上下文构建 Story Bible。
 */
@Slf4j
@Component
public class StoryBibleBuildStep {

    public static final String SYSTEM_PROMPT = """
        你是影视和短剧改编策划助手。
        请严格根据输入的全局上下文输出 JSON，不要输出 Markdown，不要补充额外解释。
        只保留以下字段：characters、relationships、locations、timeline、conflicts、foreshadowing、adaptation_strategy。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public StoryBibleBuildStep(
        AiChatAdapter aiChatAdapter,
        PromptTemplateService promptTemplateService,
        ObjectMapper objectMapper
    ) {
        this.aiChatAdapter = aiChatAdapter;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 构建 Story Bible 结构化结果。
     *
     * @param projectId 当前项目 ID
     * @param globalContext 全局上下文结果
     * @return Story Bible 结果
     */
    public StoryBibleResult execute(Long projectId, GlobalContextMergeResult globalContext) {
        validateInput(projectId, globalContext);
        String globalContextJson = writeAsJson(globalContext);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("globalContext", globalContextJson);
        String userPrompt = promptTemplateService.render("story-bible-build", variables);

        log.info("【Story Bible】开始构建 Story Bible，项目ID：{}", projectId);
        String aiResponse = aiChatAdapter.chat(SYSTEM_PROMPT, userPrompt);
        StoryBibleResult result = parseResponse(projectId, aiResponse);
        log.info(
            "【Story Bible】构建完成，项目ID：{}，角色数：{}，关系数：{}，地点数：{}",
            result.getProjectId(),
            result.getCharacters().size(),
            result.getRelationships().size(),
            result.getLocations().size()
        );
        return result;
    }

    private void validateInput(Long projectId, GlobalContextMergeResult globalContext) {
        if (projectId == null) {
            throw new IllegalArgumentException("构建 Story Bible 时项目ID不能为空。");
        }
        if (globalContext == null) {
            throw new IllegalArgumentException("构建 Story Bible 时全局上下文不能为空。");
        }
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("全局上下文无法序列化为 JSON。", exception);
        }
    }

    private StoryBibleResult parseResponse(Long projectId, String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("Story Bible 构建结果为空。");
        }

        try {
            StoryBibleResult result = objectMapper.readValue(aiResponse, StoryBibleResult.class);
            result.setProjectId(projectId);
            result.setCharacters(safeEntityList(result.getCharacters()));
            result.setRelationships(safeEntityList(result.getRelationships()));
            result.setLocations(safeEntityList(result.getLocations()));
            result.setTimeline(safeEntityList(result.getTimeline()));
            result.setConflicts(safeEntityList(result.getConflicts()));
            result.setForeshadowing(safeEntityList(result.getForeshadowing()));
            result.setAdaptationStrategy(safeStringList(result.getAdaptationStrategy()));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Story Bible 构建结果无法解析为 JSON。", exception);
        }
    }

    private <T> List<T> safeEntityList(List<T> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream().filter(java.util.Objects::nonNull).toList();
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
