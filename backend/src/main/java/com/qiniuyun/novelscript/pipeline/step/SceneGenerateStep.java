package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
        只保留下列字段：id、slugline、purpose、source_refs、characters、actions、beats、dialogue、transition、notes。
        actions 必须是字符串数组，每一项都是单条纯文本动作描述，不要输出对象项。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 构造场景生成步骤。
     *
     * @param aiChatAdapter AI 文本生成适配器
     * @param promptTemplateService Prompt 模板服务
     * @param objectMapper JSON 读写工具
     */
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
        log.info(
            "【场景生成】生成完成，场景ID：{}，动作数：{}，对白数：{}",
            result.getId(),
            result.getActions().size(),
            result.getDialogue().size()
        );
        return result;
    }

    /**
     * 校验场景生成输入。
     *
     * @param storyBible Story Bible 结果
     * @param scenePlan 单场规划结果
     */
    private void validateInput(StoryBibleResult storyBible, ScriptOutlineScene scenePlan) {
        if (storyBible == null) {
            throw new IllegalArgumentException("生成场景时 Story Bible 不能为空。");
        }
        if (scenePlan == null) {
            throw new IllegalArgumentException("生成场景时场景规划不能为空。");
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
            throw new IllegalStateException("场景生成输入无法序列化为 JSON。", exception);
        }
    }

    /**
     * 解析模型返回的场景结果，并补齐兜底字段。
     *
     * @param aiResponse 模型原始返回
     * @param scenePlan 单场规划结果
     * @return 单场剧本结果
     */
    private ScriptSceneResult parseResponse(String aiResponse, ScriptOutlineScene scenePlan) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("场景生成结果为空。");
        }

        try {
            JsonNode normalizedResponse = normalizeSceneResponse(aiResponse);
            ScriptSceneResult result = objectMapper.treeToValue(normalizedResponse, ScriptSceneResult.class);
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

    /**
     * 兼容模型偶发返回的 actions 对象数组，统一收敛为字符串数组后再反序列化。
     *
     * @param aiResponse 模型原始返回
     * @return 标准化后的 JSON 节点
     * @throws JsonProcessingException 解析失败时抛出
     */
    private JsonNode normalizeSceneResponse(String aiResponse) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(aiResponse);
        if (!(rootNode instanceof ObjectNode rootObject)) {
            return rootNode;
        }

        normalizeTextField(rootObject, "id");
        normalizeTextField(rootObject, "slugline");
        normalizeTextField(rootObject, "purpose");
        normalizeTextField(rootObject, "transition");
        rootObject.set("source_refs", normalizeStringArrayNode(readField(rootObject, "source_refs", "sourceRefs")));
        rootObject.set("characters", normalizeStringArrayNode(rootObject.get("characters")));
        rootObject.set("actions", normalizeActionsNode(rootObject.get("actions")));
        rootObject.set("beats", normalizeBeatsNode(rootObject.get("beats")));
        rootObject.set("dialogue", normalizeDialogueNode(rootObject.get("dialogue")));
        rootObject.set("notes", normalizeNotesNode(rootObject.get("notes")));
        return rootObject;
    }

    /**
     * 读取节点的第一个命中字段。
     *
     * @param rootObject 当前对象节点
     * @param fieldNames 候选字段名
     * @return 命中的字段节点
     */
    private JsonNode readField(ObjectNode rootObject, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = rootObject.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return fieldNode;
            }
        }
        return null;
    }

    /**
     * 将指定字段归一化为纯文本节点，兼容简单对象包装。
     *
     * @param rootObject 当前对象节点
     * @param fieldName 目标字段名
     */
    private void normalizeTextField(ObjectNode rootObject, String fieldName) {
        JsonNode fieldNode = rootObject.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return;
        }

        String value = extractReadableText(fieldNode);
        rootObject.set(fieldName, TextNode.valueOf(value));
    }

    /**
     * 将任意列表字段统一整理成字符串数组。
     *
     * @param fieldNode 原始字段节点
     * @return 标准化后的字符串数组节点
     */
    private ArrayNode normalizeStringArrayNode(JsonNode fieldNode) {
        ArrayNode normalizedValues = objectMapper.createArrayNode();
        if (fieldNode == null || fieldNode.isNull()) {
            return normalizedValues;
        }

        if (fieldNode.isArray()) {
            for (JsonNode itemNode : fieldNode) {
                addNormalizedStrings(normalizedValues, itemNode);
            }
            return normalizedValues;
        }

        addNormalizedStrings(normalizedValues, fieldNode);
        return normalizedValues;
    }

    /**
     * 将单个节点中可读的文本值拆分后写入字符串数组。
     *
     * @param normalizedValues 标准化字符串数组
     * @param itemNode 原始字段节点
     */
    private void addNormalizedStrings(ArrayNode normalizedValues, JsonNode itemNode) {
        String rawValue = extractReadableText(itemNode);
        if (!StringUtils.hasText(rawValue)) {
            return;
        }

        for (String item : splitListText(rawValue)) {
            if (StringUtils.hasText(item)) {
                normalizedValues.add(item.trim());
            }
        }
    }

    /**
     * 将 actions 节点统一整理成字符串数组。
     *
     * @param actionsNode 原始 actions 节点
     * @return 标准化后的字符串数组节点
     */
    private ArrayNode normalizeActionsNode(JsonNode actionsNode) {
        ArrayNode normalizedActions = objectMapper.createArrayNode();
        if (actionsNode == null || actionsNode.isNull()) {
            return normalizedActions;
        }

        if (actionsNode.isArray()) {
            for (JsonNode actionNode : actionsNode) {
                addNormalizedAction(normalizedActions, actionNode);
            }
            return normalizedActions;
        }

        addNormalizedAction(normalizedActions, actionsNode);
        return normalizedActions;
    }

    /**
     * 将 beats 节点统一整理成对象数组。
     *
     * @param beatsNode 原始 beats 节点
     * @return 标准化后的节拍数组节点
     */
    private ArrayNode normalizeBeatsNode(JsonNode beatsNode) {
        ArrayNode normalizedBeats = objectMapper.createArrayNode();
        if (beatsNode == null || beatsNode.isNull()) {
            return normalizedBeats;
        }

        if (beatsNode.isArray()) {
            for (JsonNode beatNode : beatsNode) {
                addNormalizedBeat(normalizedBeats, beatNode);
            }
            return normalizedBeats;
        }

        addNormalizedBeat(normalizedBeats, beatsNode);
        return normalizedBeats;
    }

    /**
     * 将 dialogue 节点统一整理成对象数组。
     *
     * @param dialogueNode 原始对白节点
     * @return 标准化后的对白数组节点
     */
    private ArrayNode normalizeDialogueNode(JsonNode dialogueNode) {
        ArrayNode normalizedDialogue = objectMapper.createArrayNode();
        if (dialogueNode == null || dialogueNode.isNull()) {
            return normalizedDialogue;
        }

        if (dialogueNode.isArray()) {
            for (JsonNode itemNode : dialogueNode) {
                addNormalizedDialogue(normalizedDialogue, itemNode);
            }
            return normalizedDialogue;
        }

        addNormalizedDialogue(normalizedDialogue, dialogueNode);
        return normalizedDialogue;
    }

    /**
     * 将 notes 节点统一整理成对象节点。
     *
     * @param notesNode 原始备注节点
     * @return 标准化后的备注对象
     */
    private ObjectNode normalizeNotesNode(JsonNode notesNode) {
        ObjectNode normalizedNotes = objectMapper.createObjectNode();
        if (notesNode == null || notesNode.isNull()) {
            return normalizedNotes;
        }
        if (notesNode.isTextual()) {
            normalizedNotes.put("todo", notesNode.asText().trim());
            return normalizedNotes;
        }
        if (!notesNode.isObject()) {
            return normalizedNotes;
        }

        normalizedNotes.put("emotion", extractReadableText(readObjectField(notesNode, "emotion")));
        normalizedNotes.put("pacing", extractReadableText(readObjectField(notesNode, "pacing")));
        normalizedNotes.put("todo", extractReadableText(readObjectField(notesNode, "todo", "notes", "remark")));
        return normalizedNotes;
    }

    /**
     * 将单条动作写入标准化数组。
     *
     * @param normalizedActions 标准化动作数组
     * @param actionNode 原始动作节点
     */
    private void addNormalizedAction(ArrayNode normalizedActions, JsonNode actionNode) {
        String actionText = extractActionText(actionNode);
        if (StringUtils.hasText(actionText)) {
            normalizedActions.add(actionText.trim());
        }
    }

    /**
     * 将单个节拍节点写入标准化数组。
     *
     * @param normalizedBeats 标准化节拍数组
     * @param beatNode 原始节拍节点
     */
    private void addNormalizedBeat(ArrayNode normalizedBeats, JsonNode beatNode) {
        if (beatNode == null || beatNode.isNull()) {
            return;
        }

        ObjectNode normalizedBeat = objectMapper.createObjectNode();
        if (beatNode.isObject()) {
            normalizedBeat.put("id", extractReadableText(readObjectField(beatNode, "id")));
            normalizedBeat.put("action", extractReadableText(readObjectField(beatNode, "action", "text", "content", "summary")));
        }
        else {
            normalizedBeat.put("action", extractReadableText(beatNode));
        }

        if (StringUtils.hasText(normalizedBeat.path("id").asText())
            || StringUtils.hasText(normalizedBeat.path("action").asText())) {
            normalizedBeats.add(normalizedBeat);
        }
    }

    /**
     * 将单个对白节点写入标准化数组。
     *
     * @param normalizedDialogue 标准化对白数组
     * @param dialogueNode 原始对白节点
     */
    private void addNormalizedDialogue(ArrayNode normalizedDialogue, JsonNode dialogueNode) {
        if (dialogueNode == null || dialogueNode.isNull()) {
            return;
        }

        ObjectNode normalizedItem = objectMapper.createObjectNode();
        if (dialogueNode.isObject()) {
            normalizedItem.put(
                "character_id",
                extractReadableText(readObjectField(dialogueNode, "character_id", "characterId", "speaker", "name"))
            );
            normalizedItem.put(
                "parenthetical",
                extractReadableText(readObjectField(dialogueNode, "parenthetical", "emotion", "tone"))
            );
            normalizedItem.put("line", extractReadableText(readObjectField(dialogueNode, "line", "text", "content", "dialogue")));
            normalizedItem.put("subtext", extractReadableText(readObjectField(dialogueNode, "subtext", "note", "notes")));
        }
        else {
            normalizedItem.put("line", extractReadableText(dialogueNode));
        }

        if (StringUtils.hasText(normalizedItem.path("character_id").asText())
            || StringUtils.hasText(normalizedItem.path("line").asText())
            || StringUtils.hasText(normalizedItem.path("subtext").asText())) {
            normalizedDialogue.add(normalizedItem);
        }
    }

    /**
     * 从对象节点中按候选字段名读取第一个命中字段。
     *
     * @param objectNode 对象节点
     * @param fieldNames 候选字段名
     * @return 命中的字段节点
     */
    private JsonNode readObjectField(JsonNode objectNode, String... fieldNames) {
        if (objectNode == null || !objectNode.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = objectNode.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return fieldNode;
            }
        }
        return null;
    }

    /**
     * 从动作节点中提取可读文本。
     *
     * @param actionNode 原始动作节点
     * @return 动作文案
     */
    private String extractActionText(JsonNode actionNode) {
        if (actionNode == null || actionNode.isNull()) {
            return null;
        }
        if (actionNode.isTextual()) {
            return actionNode.asText();
        }
        if (actionNode.isNumber() || actionNode.isBoolean()) {
            return actionNode.asText();
        }
        if (actionNode.isObject()) {
            JsonNode textNode = actionNode.get("text");
            if (textNode != null && textNode.isTextual()) {
                return textNode.asText();
            }
            JsonNode actionTextNode = actionNode.get("action");
            if (actionTextNode != null && actionTextNode.isTextual()) {
                return actionTextNode.asText();
            }
        }
        return null;
    }

    /**
     * 从任意节点中提取可读文本，优先兼容大模型常见的对象包装形态。
     *
     * @param fieldNode 原始字段节点
     * @return 提取出的文本
     */
    private String extractReadableText(JsonNode fieldNode) {
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }
        if (fieldNode.isTextual() || fieldNode.isNumber() || fieldNode.isBoolean()) {
            return fieldNode.asText();
        }
        if (fieldNode.isObject()) {
            for (String candidateField : List.of("name", "title", "summary", "text", "content", "label", "value", "description")) {
                JsonNode candidateNode = fieldNode.get(candidateField);
                if (candidateNode != null && !candidateNode.isNull()) {
                    String candidateValue = extractReadableText(candidateNode);
                    if (StringUtils.hasText(candidateValue)) {
                        return candidateValue;
                    }
                }
            }
            return compactJson(fieldNode);
        }
        if (fieldNode.isArray()) {
            List<String> nestedValues = new ArrayList<>();
            fieldNode.forEach(itemNode -> {
                String itemValue = extractReadableText(itemNode);
                if (StringUtils.hasText(itemValue)) {
                    nestedValues.add(itemValue.trim());
                }
            });
            return String.join("；", nestedValues);
        }
        return fieldNode.asText("");
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
            throw new IllegalStateException("场景生成复杂字段无法序列化。", exception);
        }
    }

    /**
     * 使用场景规划结果补齐模型未返回的关键字段。
     *
     * @param result 场景生成结果
     * @param scenePlan 场景规划结果
     */
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

    /**
     * 过滤节拍列表中的空对象。
     *
     * @param beats 原始节拍列表
     * @return 清洗后的节拍列表
     */
    private List<ScriptSceneBeat> safeBeats(List<ScriptSceneBeat> beats) {
        if (beats == null) {
            return new ArrayList<>();
        }
        return beats.stream().filter(Objects::nonNull).toList();
    }

    /**
     * 过滤对白列表中的空对象。
     *
     * @param dialogue 原始对白列表
     * @return 清洗后的对白列表
     */
    private List<ScriptSceneDialogue> safeDialogues(List<ScriptSceneDialogue> dialogue) {
        if (dialogue == null) {
            return new ArrayList<>();
        }
        return dialogue.stream().filter(Objects::nonNull).toList();
    }
}
