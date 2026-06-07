package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.NormalizedChapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 负责单章上下文抽取和结构化解析。
 */
@Slf4j
@Component
public class ChapterContextExtractStep {

    public static final String SYSTEM_PROMPT = """
        你是小说改编上下文分析助手。
        请严格根据提供的章节内容输出 JSON，不要输出 Markdown，不要补充额外解释。
        只保留下列字段：summary、characters、locations、events、conflicts、emotion_changes、foreshadowing、key_dialogues、source_refs。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 构造单章上下文抽取步骤。
     *
     * @param aiChatAdapter AI 文本生成适配器
     * @param promptTemplateService Prompt 模板服务
     * @param objectMapper JSON 读写工具
     */
    public ChapterContextExtractStep(
        AiChatAdapter aiChatAdapter,
        PromptTemplateService promptTemplateService,
        ObjectMapper objectMapper
    ) {
        this.aiChatAdapter = aiChatAdapter;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 对标准化后的单章文本执行上下文抽取。
     *
     * @param normalizedChapter 标准化后的章节
     * @return 单章上下文结果
     */
    public ChapterContextResult execute(NormalizedChapter normalizedChapter) {
        validateInput(normalizedChapter);
        Map<String, Object> variables = buildPromptVariables(normalizedChapter);
        String userPrompt = promptTemplateService.render("chapter-context-extract", variables);

        log.info("【章节上下文】开始抽取单章上下文，项目ID：{}，章节号：{}", normalizedChapter.getProjectId(), normalizedChapter.getChapterNo());
        String aiResponse = aiChatAdapter.chat(SYSTEM_PROMPT, userPrompt);
        ChapterContextResult result = parseResponse(aiResponse, normalizedChapter);
        log.info(
            "【章节上下文】抽取完成，项目ID：{}，章节号：{}，人物数：{}，地点数：{}",
            result.getProjectId(),
            result.getChapterNo(),
            result.getCharacters().size(),
            result.getLocations().size()
        );
        return result;
    }

    /**
     * 校验单章上下文抽取输入。
     *
     * @param normalizedChapter 标准化章节
     */
    private void validateInput(NormalizedChapter normalizedChapter) {
        if (normalizedChapter == null) {
            throw new IllegalArgumentException("章节上下文抽取输入不能为空。");
        }
        if (!StringUtils.hasText(normalizedChapter.getContent())) {
            throw new IllegalArgumentException("章节正文不能为空。");
        }
    }

    /**
     * 组装单章上下文抽取 Prompt 变量。
     *
     * @param normalizedChapter 标准化章节
     * @return Prompt 变量映射
     */
    private Map<String, Object> buildPromptVariables(NormalizedChapter normalizedChapter) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("chapterNo", normalizedChapter.getChapterNo());
        variables.put("chapterTitle", normalizedChapter.getTitle());
        variables.put("chapterContent", normalizedChapter.getContent());
        variables.put("wordCount", normalizedChapter.getWordCount());
        return variables;
    }

    /**
     * 解析模型返回的单章上下文结果，并补齐回填字段。
     *
     * @param aiResponse 模型原始返回
     * @param normalizedChapter 标准化章节
     * @return 单章上下文结果
     */
    private ChapterContextResult parseResponse(String aiResponse, NormalizedChapter normalizedChapter) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("章节上下文抽取结果为空。");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            if (rootNode == null || !rootNode.isObject()) {
                throw new IllegalStateException("章节上下文抽取结果不是合法的 JSON 对象。");
            }

            ChapterContextResult result = new ChapterContextResult();
            result.setProjectId(normalizedChapter.getProjectId());
            result.setChapterNo(normalizedChapter.getChapterNo());
            result.setChapterTitle(normalizedChapter.getTitle());
            result.setWordCount(normalizedChapter.getWordCount());
            result.setSummary(readTrimmedText(rootNode, "summary"));
            result.setCharacters(readStringList(rootNode, "characters"));
            result.setLocations(readStringList(rootNode, "locations"));
            result.setEvents(readStringList(rootNode, "events"));
            result.setConflicts(readStringList(rootNode, "conflicts"));
            result.setEmotionChanges(readStringList(rootNode, "emotion_changes", "emotionChanges"));
            result.setForeshadowing(readStringList(rootNode, "foreshadowing"));
            result.setKeyDialogues(readStringList(rootNode, "key_dialogues", "keyDialogues"));
            result.setSourceRefs(resolveSourceRefs(readStringList(rootNode, "source_refs", "sourceRefs"), normalizedChapter.getChapterNo()));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("章节上下文抽取结果不是合法的 JSON。", exception);
        }
    }

    /**
     * 过滤空字符串并清理列表项首尾空白。
     *
     * @param values 原始字符串列表
     * @return 清洗后的字符串列表
     */
    private List<String> safeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }

    /**
     * 读取并清理单个文本字段，缺失时返回空字符串。
     *
     * @param rootNode JSON 根节点
     * @param fieldNames 候选字段名
     * @return 清理后的字段值
     */
    private String readTrimmedText(JsonNode rootNode, String... fieldNames) {
        JsonNode fieldNode = findField(rootNode, fieldNames);
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }

        String value = extractStringValue(fieldNode);
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 读取列表字段并兼容字符串数组、对象数组与单值字符串三种常见返回形态。
     *
     * @param rootNode JSON 根节点
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
            fieldNode.forEach(itemNode -> appendStringValue(values, itemNode));
            return safeList(values);
        }

        appendStringValue(values, fieldNode);
        return safeList(values);
    }

    /**
     * 按候选字段名顺序查找第一个存在的字段。
     *
     * @param rootNode JSON 根节点
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
     * 将节点中的可读文本追加到结果列表，忽略空值和空白项。
     *
     * @param values 结果列表
     * @param itemNode 待提取节点
     */
    private void appendStringValue(List<String> values, JsonNode itemNode) {
        String value = extractStringValue(itemNode);
        if (StringUtils.hasText(value)) {
            values.add(value);
        }
    }

    /**
     * 从节点中提取可读文本，优先兼容大模型常见的对象结构。
     *
     * @param itemNode 待提取节点
     * @return 提取出的文本
     */
    private String extractStringValue(JsonNode itemNode) {
        if (itemNode == null || itemNode.isNull()) {
            return "";
        }
        if (itemNode.isTextual()) {
            return itemNode.asText();
        }
        if (itemNode.isNumber() || itemNode.isBoolean()) {
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
            itemNode.forEach(nestedNode -> appendStringValue(nestedValues, nestedNode));
            return String.join("；", safeList(nestedValues));
        }
        return itemNode.asText("");
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
            throw new IllegalStateException("章节上下文抽取结果中的复杂字段无法序列化。", exception);
        }
    }

    /**
     * 解析章节来源引用，缺省时自动回填当前章节号。
     *
     * @param sourceRefs 模型返回的来源引用
     * @param chapterNo 当前章节号
     * @return 规范化后的来源引用列表
     */
    private List<String> resolveSourceRefs(List<String> sourceRefs, Integer chapterNo) {
        List<String> safeSourceRefs = safeList(sourceRefs);
        if (!safeSourceRefs.isEmpty()) {
            return safeSourceRefs;
        }

        return List.of("chapter:" + chapterNo);
    }
}
