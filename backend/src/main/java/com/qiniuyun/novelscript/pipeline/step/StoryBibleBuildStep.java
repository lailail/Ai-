package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Objects;
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
        只保留下列字段：characters、relationships、locations、timeline、conflicts、foreshadowing、adaptation_strategy。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 构造 Story Bible 构建步骤。
     *
     * @param aiChatAdapter AI 文本生成适配器
     * @param promptTemplateService Prompt 模板服务
     * @param objectMapper JSON 读写工具
     */
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
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
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

    /**
     * 校验 Story Bible 构建输入。
     *
     * @param projectId 项目 ID
     * @param globalContext 全局上下文结果
     */
    private void validateInput(Long projectId, GlobalContextMergeResult globalContext) {
        if (projectId == null) {
            throw new IllegalArgumentException("构建 Story Bible 时项目 ID 不能为空。");
        }
        if (globalContext == null) {
            throw new IllegalArgumentException("构建 Story Bible 时全局上下文不能为空。");
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
            throw new IllegalStateException("全局上下文无法序列化为 JSON。", exception);
        }
    }

    /**
     * 解析模型返回的 Story Bible 结果。
     *
     * @param projectId 项目 ID
     * @param aiResponse 模型原始返回
     * @return Story Bible 结构化结果
     */
    private StoryBibleResult parseResponse(Long projectId, String aiResponse) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("Story Bible 构建结果为空。");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            if (rootNode == null || !rootNode.isObject()) {
                throw new IllegalStateException("Story Bible 构建结果不是合法的 JSON 对象。");
            }

            StoryBibleResult result = new StoryBibleResult();
            result.setProjectId(projectId);
            result.setCharacters(parseCharacters(rootNode));
            result.setRelationships(parseRelationships(rootNode));
            result.setLocations(parseLocations(rootNode));
            result.setTimeline(parseTimeline(rootNode));
            result.setConflicts(parseConflicts(rootNode));
            result.setForeshadowing(parseForeshadowing(rootNode));
            result.setAdaptationStrategy(readStringList(rootNode, "adaptation_strategy", "adaptationStrategy"));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Story Bible 构建结果无法解析为 JSON。", exception);
        }
    }

    /**
     * 解析角色列表，并兼容模型把列表字段返回为字符串的情况。
     *
     * @param rootNode Story Bible 根节点
     * @return 角色列表
     */
    private List<StoryBibleCharacter> parseCharacters(JsonNode rootNode) {
        List<StoryBibleCharacter> characters = new ArrayList<>();
        for (JsonNode characterNode : readObjectArray(rootNode, "characters")) {
            StoryBibleCharacter character = new StoryBibleCharacter();
            character.setId(readText(characterNode, "id"));
            character.setName(readText(characterNode, "name"));
            character.setAliases(readStringList(characterNode, "aliases"));
            character.setRole(readText(characterNode, "role"));
            character.setTraits(readStringList(characterNode, "traits"));
            character.setGoal(readText(characterNode, "goal"));
            characters.add(character);
        }
        return safeEntityList(characters);
    }

    /**
     * 解析角色关系列表。
     *
     * @param rootNode Story Bible 根节点
     * @return 角色关系列表
     */
    private List<StoryBibleRelationship> parseRelationships(JsonNode rootNode) {
        List<StoryBibleRelationship> relationships = new ArrayList<>();
        for (JsonNode relationshipNode : readObjectArray(rootNode, "relationships")) {
            StoryBibleRelationship relationship = new StoryBibleRelationship();
            relationship.setFrom(readText(relationshipNode, "from"));
            relationship.setTo(readText(relationshipNode, "to"));
            relationship.setType(readText(relationshipNode, "type"));
            relationship.setDescription(readText(relationshipNode, "description"));
            relationships.add(relationship);
        }
        return safeEntityList(relationships);
    }

    /**
     * 解析地点列表。
     *
     * @param rootNode Story Bible 根节点
     * @return 地点列表
     */
    private List<StoryBibleLocation> parseLocations(JsonNode rootNode) {
        List<StoryBibleLocation> locations = new ArrayList<>();
        for (JsonNode locationNode : readObjectArray(rootNode, "locations")) {
            StoryBibleLocation location = new StoryBibleLocation();
            location.setId(readText(locationNode, "id"));
            location.setName(readText(locationNode, "name"));
            location.setDescription(readText(locationNode, "description"));
            locations.add(location);
        }
        return safeEntityList(locations);
    }

    /**
     * 解析时间线事件列表。
     *
     * @param rootNode Story Bible 根节点
     * @return 时间线事件列表
     */
    private List<StoryBibleTimelineEvent> parseTimeline(JsonNode rootNode) {
        List<StoryBibleTimelineEvent> timeline = new ArrayList<>();
        for (JsonNode eventNode : readObjectArray(rootNode, "timeline")) {
            StoryBibleTimelineEvent event = new StoryBibleTimelineEvent();
            event.setId(readText(eventNode, "id"));
            event.setOrder(readInteger(eventNode, "order"));
            event.setSummary(readText(eventNode, "summary"));
            event.setSourceRefs(readStringList(eventNode, "source_refs", "sourceRefs"));
            timeline.add(event);
        }
        return safeEntityList(timeline);
    }

    /**
     * 解析冲突列表。
     *
     * @param rootNode Story Bible 根节点
     * @return 冲突列表
     */
    private List<StoryBibleConflict> parseConflicts(JsonNode rootNode) {
        List<StoryBibleConflict> conflicts = new ArrayList<>();
        for (JsonNode conflictNode : readObjectArray(rootNode, "conflicts")) {
            StoryBibleConflict conflict = new StoryBibleConflict();
            conflict.setId(readText(conflictNode, "id"));
            conflict.setSummary(readText(conflictNode, "summary"));
            conflicts.add(conflict);
        }
        return safeEntityList(conflicts);
    }

    /**
     * 解析伏笔列表。
     *
     * @param rootNode Story Bible 根节点
     * @return 伏笔列表
     */
    private List<StoryBibleForeshadowing> parseForeshadowing(JsonNode rootNode) {
        List<StoryBibleForeshadowing> foreshadowing = new ArrayList<>();
        for (JsonNode foreshadowingNode : readObjectArray(rootNode, "foreshadowing")) {
            StoryBibleForeshadowing item = new StoryBibleForeshadowing();
            item.setId(readText(foreshadowingNode, "id"));
            item.setSetup(readText(foreshadowingNode, "setup"));
            item.setPayoffHint(readText(foreshadowingNode, "payoff_hint", "payoffHint"));
            item.setSourceRefs(readStringList(foreshadowingNode, "source_refs", "sourceRefs"));
            foreshadowing.add(item);
        }
        return safeEntityList(foreshadowing);
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
     * 读取整数字段，无法解析时返回空值。
     *
     * @param rootNode 当前节点
     * @param fieldNames 候选字段名
     * @return 整数值
     */
    private Integer readInteger(JsonNode rootNode, String... fieldNames) {
        JsonNode fieldNode = findField(rootNode, fieldNames);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isInt() || fieldNode.isLong()) {
            return fieldNode.intValue();
        }

        String value = extractStringValue(fieldNode);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException exception) {
            return null;
        }
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
            throw new IllegalStateException("Story Bible 复杂字段无法序列化。", exception);
        }
    }

    /**
     * 过滤实体列表中的空对象。
     *
     * @param values 原始实体列表
     * @param <T> 实体类型
     * @return 清洗后的实体列表
     */
    private <T> List<T> safeEntityList(List<T> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream().filter(Objects::nonNull).toList();
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
