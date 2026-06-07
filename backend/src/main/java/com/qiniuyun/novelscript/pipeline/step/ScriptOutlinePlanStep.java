package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            if (rootNode == null || !rootNode.isObject()) {
                throw new IllegalStateException("剧本大纲规划结果不是合法的 JSON 对象。");
            }

            ScriptOutlineResult result = new ScriptOutlineResult();
            result.setProjectId(projectId);
            result.setEpisodes(parseEpisodes(rootNode));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("剧本大纲规划结果无法解析为 JSON。", exception);
        }
    }

    /**
     * 解析剧集列表，并兼容单对象与列表字段漂移。
     *
     * @param rootNode 大纲根节点
     * @return 清洗后的剧集列表
     */
    private List<ScriptOutlineEpisode> parseEpisodes(JsonNode rootNode) {
        List<ScriptOutlineEpisode> episodes = new ArrayList<>();
        for (JsonNode episodeNode : readObjectArray(rootNode, "episodes")) {
            ScriptOutlineEpisode episode = new ScriptOutlineEpisode();
            episode.setId(readText(episodeNode, "id"));
            episode.setTitle(readText(episodeNode, "title"));
            episode.setPremise(readText(episodeNode, "premise"));
            episode.setSourceRefs(readStringList(episodeNode, "source_refs", "sourceRefs"));
            episode.setScenes(parseScenes(episodeNode));
            episodes.add(episode);
        }
        return safeEpisodes(episodes);
    }

    /**
     * 解析单集下的场景规划列表。
     *
     * @param episodeNode 剧集节点
     * @return 场景规划列表
     */
    private List<ScriptOutlineScene> parseScenes(JsonNode episodeNode) {
        List<ScriptOutlineScene> scenes = new ArrayList<>();
        for (JsonNode sceneNode : readObjectArray(episodeNode, "scenes")) {
            ScriptOutlineScene scene = new ScriptOutlineScene();
            scene.setId(readText(sceneNode, "id"));
            scene.setSlugline(readText(sceneNode, "slugline"));
            scene.setPurpose(readText(sceneNode, "purpose"));
            scene.setConflict(readText(sceneNode, "conflict"));
            scene.setSourceRefs(readStringList(sceneNode, "source_refs", "sourceRefs"));
            scene.setCharacters(readStringList(sceneNode, "characters"));
            scenes.add(scene);
        }
        return safeScenes(scenes);
    }

    /**
     * 读取对象数组字段，缺失或形态不符时返回空列表。
     *
     * @param rootNode 当前节点
     * @param fieldNames 候选字段名
     * @return 对象节点列表
     */
    private List<JsonNode> readObjectArray(JsonNode rootNode, String... fieldNames) {
        JsonNode fieldNode = findField(rootNode, fieldNames);
        if (fieldNode == null || fieldNode.isNull()) {
            return List.of();
        }

        List<JsonNode> results = new ArrayList<>();
        if (fieldNode.isArray()) {
            fieldNode.forEach(itemNode -> {
                if (itemNode != null && itemNode.isObject()) {
                    results.add(itemNode);
                }
            });
            return results;
        }

        if (fieldNode.isObject()) {
            results.add(fieldNode);
        }
        return results;
    }

    /**
     * 读取文本字段，兼容简单对象包装形态。
     *
     * @param rootNode 当前节点
     * @param fieldNames 候选字段名
     * @return 清理后的文本
     */
    private String readText(JsonNode rootNode, String... fieldNames) {
        JsonNode fieldNode = findField(rootNode, fieldNames);
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }

        String value = extractStringValue(fieldNode);
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 读取字符串列表，兼容数组、单值字符串和简单对象包装。
     *
     * @param rootNode 当前节点
     * @param fieldNames 候选字段名
     * @return 清洗后的字符串列表
     */
    private List<String> readStringList(JsonNode rootNode, String... fieldNames) {
        JsonNode fieldNode = findField(rootNode, fieldNames);
        if (fieldNode == null || fieldNode.isNull()) {
            return new ArrayList<>();
        }

        List<String> values = new ArrayList<>();
        if (fieldNode.isArray()) {
            fieldNode.forEach(itemNode -> appendStringValues(values, itemNode));
            return safeStringList(values);
        }

        appendStringValues(values, fieldNode);
        return safeStringList(values);
    }

    /**
     * 按候选字段名顺序查找第一个存在的字段。
     *
     * @param rootNode 当前节点
     * @param fieldNames 候选字段名
     * @return 命中的字段节点
     */
    private JsonNode findField(JsonNode rootNode, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = rootNode.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return fieldNode;
            }
        }
        return null;
    }

    /**
     * 将节点中的文本值追加到目标列表，必要时按常见分隔符拆分。
     *
     * @param values 目标列表
     * @param itemNode 待提取节点
     */
    private void appendStringValues(List<String> values, JsonNode itemNode) {
        String rawValue = extractStringValue(itemNode);
        if (!StringUtils.hasText(rawValue)) {
            return;
        }

        for (String item : splitListText(rawValue)) {
            if (StringUtils.hasText(item)) {
                values.add(item.trim());
            }
        }
    }

    /**
     * 从任意节点提取可读文本，优先兼容模型常见的对象包装返回。
     *
     * @param itemNode 待提取节点
     * @return 提取出的文本
     */
    private String extractStringValue(JsonNode itemNode) {
        if (itemNode == null || itemNode.isNull()) {
            return "";
        }
        if (itemNode.isTextual() || itemNode.isNumber() || itemNode.isBoolean()) {
            return itemNode.asText();
        }
        if (itemNode.isObject()) {
            for (String candidateField : List.of("name", "title", "summary", "text", "content", "label", "value", "description")) {
                JsonNode candidateNode = itemNode.get(candidateField);
                if (candidateNode != null && !candidateNode.isNull()) {
                    String candidateValue = extractStringValue(candidateNode);
                    if (StringUtils.hasText(candidateValue)) {
                        return candidateValue;
                    }
                }
            }
            return compactJson(itemNode);
        }
        if (itemNode.isArray()) {
            List<String> nestedValues = new ArrayList<>();
            itemNode.forEach(nestedNode -> appendStringValues(nestedValues, nestedNode));
            return String.join("；", nestedValues);
        }
        return itemNode.asText("");
    }

    /**
     * 按中文顿号、逗号、分号和换行拆分模型返回的列表文本。
     *
     * @param rawValue 原始文本
     * @return 拆分后的文本列表
     */
    private List<String> splitListText(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return List.of();
        }
        String normalized = rawValue.trim();
        if (!normalized.contains("，")
            && !normalized.contains(",")
            && !normalized.contains("、")
            && !normalized.contains("；")
            && !normalized.contains(";")
            && !normalized.contains("\n")) {
            return List.of(normalized);
        }

        String[] items = normalized.split("\\s*[，,、；;\\n]+\\s*");
        List<String> results = new ArrayList<>();
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                results.add(item.trim());
            }
        }
        return results;
    }

    /**
     * 将复杂节点压缩为单行 JSON 文本，避免兼容解析时直接失败。
     *
     * @param node 待压缩节点
     * @return 单行 JSON 文本
     */
    private String compactJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("大纲规划复杂字段无法序列化。", exception);
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
