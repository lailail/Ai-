package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.GlobalContextMergeResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 负责合并多章节上下文，生成可复用的全局上下文草稿。
 */
@Slf4j
@Component
public class GlobalContextMergeStep {

    public static final String SYSTEM_PROMPT = """
        你是小说改编的全局上下文整理助手。
        请严格根据输入的章节上下文输出 JSON，不要输出 Markdown，不要补充额外解释。
        只保留下列字段：summary、characters、locations、timeline、relationships、conflicts、source_context_refs。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 构造全局上下文合并步骤。
     *
     * @param aiChatAdapter AI 文本生成适配器
     * @param promptTemplateService Prompt 模板服务
     * @param objectMapper JSON 读写工具
     */
    public GlobalContextMergeStep(
        AiChatAdapter aiChatAdapter,
        PromptTemplateService promptTemplateService,
        ObjectMapper objectMapper
    ) {
        this.aiChatAdapter = aiChatAdapter;
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 合并多个章节上下文结果。
     *
     * @param projectId 当前项目 ID
     * @param chapterContexts 章节上下文列表
     * @return 全局上下文结果
     */
    public GlobalContextMergeResult execute(Long projectId, List<ChapterContextResult> chapterContexts) {
        validateInput(projectId, chapterContexts);
        String chapterContextsJson = writeAsJson(chapterContexts);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("chapterContexts", chapterContextsJson);
        String userPrompt = promptTemplateService.render("global-context-merge", variables);

        log.info("【全局上下文】开始合并章节上下文，项目ID：{}，章节数：{}", projectId, chapterContexts.size());
        String aiResponse = aiChatAdapter.chat(SYSTEM_PROMPT, userPrompt);
        GlobalContextMergeResult result = parseResponse(projectId, aiResponse, chapterContexts);
        log.info(
            "【全局上下文】合并完成，项目ID：{}，角色数：{}，地点数：{}，冲突数：{}",
            result.getProjectId(),
            result.getCharacters().size(),
            result.getLocations().size(),
            result.getConflicts().size()
        );
        return result;
    }

    /**
     * 校验全局上下文合并输入。
     *
     * @param projectId 项目 ID
     * @param chapterContexts 单章上下文列表
     */
    private void validateInput(Long projectId, List<ChapterContextResult> chapterContexts) {
        if (projectId == null) {
            throw new IllegalArgumentException("全局上下文合并时项目 ID 不能为空。");
        }
        if (CollectionUtils.isEmpty(chapterContexts)) {
            throw new IllegalArgumentException("全局上下文合并至少需要一组章节上下文。");
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
            throw new IllegalStateException("章节上下文无法序列化为 JSON。", exception);
        }
    }

    /**
     * 解析全局上下文合并结果，并补齐项目与来源引用信息。
     *
     * @param projectId 项目 ID
     * @param aiResponse 模型原始返回
     * @param chapterContexts 单章上下文列表
     * @return 全局上下文结果
     */
    private GlobalContextMergeResult parseResponse(
        Long projectId,
        String aiResponse,
        List<ChapterContextResult> chapterContexts
    ) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("全局上下文合并结果为空。");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            if (rootNode == null || !rootNode.isObject()) {
                throw new IllegalStateException("全局上下文合并结果不是合法的 JSON 对象。");
            }

            GlobalContextMergeResult result = new GlobalContextMergeResult();
            result.setProjectId(projectId);
            result.setSummary(readTrimmedText(rootNode, "summary"));
            result.setCharacters(readStringList(rootNode, "characters"));
            result.setLocations(readStringList(rootNode, "locations"));
            result.setTimeline(readStringList(rootNode, "timeline"));
            result.setRelationships(readStringList(rootNode, "relationships"));
            result.setConflicts(readStringList(rootNode, "conflicts"));
            result.setSourceContextRefs(
                resolveSourceRefs(
                    readStringList(rootNode, "source_context_refs", "sourceContextRefs"),
                    chapterContexts
                )
            );
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("全局上下文合并结果无法解析为 JSON。", exception);
        }
    }

    /**
     * 读取单个文本字段，缺失时返回空字符串。
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
     * 读取字符串列表字段，兼容数组、单值字符串与简单对象包装。
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
            fieldNode.forEach(itemNode -> appendStringValues(values, itemNode));
            return safeList(values);
        }

        appendStringValues(values, fieldNode);
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
     * 从任意节点提取可读文本，优先兼容模型常见的对象包装形态。
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
            throw new IllegalStateException("全局上下文复杂字段无法序列化。", exception);
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
     * 解析全局上下文来源引用，缺省时从单章上下文中汇总。
     *
     * @param sourceRefs 模型返回的来源引用
     * @param chapterContexts 单章上下文列表
     * @return 规范化后的来源引用列表
     */
    private List<String> resolveSourceRefs(List<String> sourceRefs, List<ChapterContextResult> chapterContexts) {
        List<String> safeSourceRefs = safeList(sourceRefs);
        if (!safeSourceRefs.isEmpty()) {
            return safeSourceRefs;
        }

        return chapterContexts.stream()
            .flatMap(result -> result.getSourceRefs().stream())
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }
}
