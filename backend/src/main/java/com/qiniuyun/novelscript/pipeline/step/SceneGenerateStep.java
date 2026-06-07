package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

        rootObject.set("actions", normalizeActionsNode(rootObject.get("actions")));
        return rootObject;
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
