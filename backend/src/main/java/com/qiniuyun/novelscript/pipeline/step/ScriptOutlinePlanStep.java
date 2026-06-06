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
        只保留下列字段：episodes。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 构造剧本大纲规划步骤。
     *
     * @param aiChatAdapter AI 文本生成适配器
     * @param promptTemplateService Prompt 模板服务
     * @param objectMapper JSON 读写工具
     */
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

    /**
     * 校验剧本大纲规划输入。
     *
     * @param projectId 项目 ID
     * @param storyBible Story Bible 结果
     * @param chapterContexts 单章上下文列表
     */
    private void validateInput(Long projectId, StoryBibleResult storyBible, List<ChapterContextResult> chapterContexts) {
        if (projectId == null) {
            throw new IllegalArgumentException("规划剧本大纲时项目 ID 不能为空。");
        }
        if (storyBible == null) {
            throw new IllegalArgumentException("规划剧本大纲时 Story Bible 不能为空。");
        }
        if (CollectionUtils.isEmpty(chapterContexts)) {
            throw new IllegalArgumentException("规划剧本大纲时章节上下文不能为空。");
        }
    }

    /**
     * 将对象序列化为 JSON，供 Prompt 模板渲染使用。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("大纲规划输入无法序列化为 JSON。", exception);
        }
    }

    /**
     * 解析模型返回的大纲规划结果。
     *
     * @param projectId 项目 ID
     * @param aiResponse 模型原始返回
     * @return 剧本大纲结果
     */
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

    /**
     * 清洗剧集列表中的空项，并统一规范剧集内容。
     *
     * @param episodes 原始剧集列表
     * @return 清洗后的剧集列表
     */
    private List<ScriptOutlineEpisode> safeEpisodes(List<ScriptOutlineEpisode> episodes) {
        if (episodes == null) {
            return new ArrayList<>();
        }

        return episodes.stream()
            .filter(Objects::nonNull)
            .map(this::sanitizeEpisode)
            .toList();
    }

    /**
     * 清洗单集中的来源引用和场景列表。
     *
     * @param episode 原始剧集
     * @return 清洗后的剧集
     */
    private ScriptOutlineEpisode sanitizeEpisode(ScriptOutlineEpisode episode) {
        episode.setSourceRefs(safeStringList(episode.getSourceRefs()));
        episode.setScenes(safeScenes(episode.getScenes()));
        return episode;
    }

    /**
     * 清洗场景列表中的角色和章节来源引用。
     *
     * @param scenes 原始场景列表
     * @return 清洗后的场景列表
     */
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

    /**
     * 过滤空字符串并清理列表项首尾空白。
     *
     * @param values 原始字符串列表
     * @return 清洗后的字符串列表
     */
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
