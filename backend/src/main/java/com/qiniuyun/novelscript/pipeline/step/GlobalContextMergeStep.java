package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
     * 解析全局上下文合并结果，并补齐项目信息与来源引用。
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
            GlobalContextMergeResult result = objectMapper.readValue(aiResponse, GlobalContextMergeResult.class);
            result.setProjectId(projectId);
            result.setSummary(StringUtils.hasText(result.getSummary()) ? result.getSummary().trim() : "");
            result.setCharacters(safeList(result.getCharacters()));
            result.setLocations(safeList(result.getLocations()));
            result.setTimeline(safeList(result.getTimeline()));
            result.setRelationships(safeList(result.getRelationships()));
            result.setConflicts(safeList(result.getConflicts()));
            result.setSourceContextRefs(resolveSourceRefs(result.getSourceContextRefs(), chapterContexts));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("全局上下文合并结果无法解析为 JSON。", exception);
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
