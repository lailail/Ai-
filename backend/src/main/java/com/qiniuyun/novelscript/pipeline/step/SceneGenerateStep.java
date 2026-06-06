package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ScriptOutlineScene;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneBeat;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneDialogue;
import com.qiniuyun.novelscript.pipeline.model.ScriptSceneResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 负责根据场景规划生成单场剧本内容。
 */
@Slf4j
@Component
public class SceneGenerateStep {

    public static final String SYSTEM_PROMPT = """
        你是影视剧本写作助手。
        请严格根据输入内容输出 JSON，不要输出 Markdown，不要补充额外解释。
        只保留以下字段：id、slugline、purpose、source_refs、characters、actions、beats、dialogue、transition、notes。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public SceneGenerateStep(
        AiChatAdapter aiChatAdapter,
        PromptTemplateService promptTemplateService,
        ObjectMapper objectMapper
    ) {
        this.aiChatAdapter = aiChatAdapter;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 生成单场剧本。
     *
     * @param storyBible Story Bible 结果
     * @param scenePlan 单场规划结果
     * @return 单场剧本结果
     */
    public ScriptSceneResult execute(StoryBibleResult storyBible, ScriptOutlineScene scenePlan) {
        validateInput(storyBible, scenePlan);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("storyBible", writeAsJson(storyBible));
        variables.put("scenePlan", writeAsJson(scenePlan));
        String userPrompt = promptTemplateService.render("scene-generate", variables);

        log.info("【场景生成】开始生成场景，场景ID：{}，标题：{}", scenePlan.getId(), scenePlan.getSlugline());
        String aiResponse = aiChatAdapter.chat(SYSTEM_PROMPT, userPrompt);
        ScriptSceneResult result = parseResponse(aiResponse, scenePlan);
        log.info("【场景生成】生成完成，场景ID：{}，动作数：{}，对白数：{}", result.getId(), result.getActions().size(), result.getDialogue().size());
        return result;
    }

    private void validateInput(StoryBibleResult storyBible, ScriptOutlineScene scenePlan) {
        if (storyBible == null) {
            throw new IllegalArgumentException("生成场景时 Story Bible 不能为空。");
        }
        if (scenePlan == null) {
            throw new IllegalArgumentException("生成场景时场景规划不能为空。");
        }
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("场景生成输入无法序列化为 JSON。", exception);
        }
    }

    private ScriptSceneResult parseResponse(String aiResponse, ScriptOutlineScene scenePlan) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("场景生成结果为空。");
        }

        try {
            ScriptSceneResult result = objectMapper.readValue(aiResponse, ScriptSceneResult.class);
            fillFallbackFields(result, scenePlan);
            result.setSourceRefs(safeStringList(result.getSourceRefs()));
            result.setCharacters(safeStringList(result.getCharacters()));
            result.setActions(safeStringList(result.getActions()));
            result.setBeats(safeBeats(result.getBeats()));
            result.setDialogue(safeDialogues(result.getDialogue()));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("场景生成结果无法解析为 JSON。", exception);
        }
    }

    private void fillFallbackFields(ScriptSceneResult result, ScriptOutlineScene scenePlan) {
        if (!StringUtils.hasText(result.getId())) {
            result.setId(scenePlan.getId());
        }
        if (!StringUtils.hasText(result.getSlugline())) {
            result.setSlugline(scenePlan.getSlugline());
        }
        if (!StringUtils.hasText(result.getPurpose())) {
            result.setPurpose(scenePlan.getPurpose());
        }
        if (result.getSourceRefs() == null || result.getSourceRefs().isEmpty()) {
            result.setSourceRefs(scenePlan.getSourceRefs());
        }
        if (result.getCharacters() == null || result.getCharacters().isEmpty()) {
            result.setCharacters(scenePlan.getCharacters());
        }
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

    private List<ScriptSceneBeat> safeBeats(List<ScriptSceneBeat> beats) {
        if (beats == null) {
            return new ArrayList<>();
        }
        return beats.stream().filter(Objects::nonNull).toList();
    }

    private List<ScriptSceneDialogue> safeDialogues(List<ScriptSceneDialogue> dialogue) {
        if (dialogue == null) {
            return new ArrayList<>();
        }
        return dialogue.stream().filter(Objects::nonNull).toList();
    }
}
