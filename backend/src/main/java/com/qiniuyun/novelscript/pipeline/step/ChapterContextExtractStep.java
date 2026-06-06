package com.qiniuyun.novelscript.pipeline.step;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.prompt.PromptTemplateService;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.NormalizedChapter;
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
        只保留以下字段：summary、characters、locations、events、conflicts、emotion_changes、foreshadowing、key_dialogues、source_refs。
        """;

    private final AiChatAdapter aiChatAdapter;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

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

    private void validateInput(NormalizedChapter normalizedChapter) {
        if (normalizedChapter == null) {
            throw new IllegalArgumentException("章节上下文抽取输入不能为空。");
        }
        if (!StringUtils.hasText(normalizedChapter.getContent())) {
            throw new IllegalArgumentException("章节正文不能为空。");
        }
    }

    private Map<String, Object> buildPromptVariables(NormalizedChapter normalizedChapter) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("chapterNo", normalizedChapter.getChapterNo());
        variables.put("chapterTitle", normalizedChapter.getTitle());
        variables.put("chapterContent", normalizedChapter.getContent());
        variables.put("wordCount", normalizedChapter.getWordCount());
        return variables;
    }

    private ChapterContextResult parseResponse(String aiResponse, NormalizedChapter normalizedChapter) {
        if (!StringUtils.hasText(aiResponse)) {
            throw new IllegalStateException("章节上下文抽取结果为空。");
        }

        try {
            ChapterContextResult result = objectMapper.readValue(aiResponse, ChapterContextResult.class);
            result.setProjectId(normalizedChapter.getProjectId());
            result.setChapterNo(normalizedChapter.getChapterNo());
            result.setChapterTitle(normalizedChapter.getTitle());
            result.setWordCount(normalizedChapter.getWordCount());
            result.setSummary(StringUtils.hasText(result.getSummary()) ? result.getSummary().trim() : "");
            result.setCharacters(safeList(result.getCharacters()));
            result.setLocations(safeList(result.getLocations()));
            result.setEvents(safeList(result.getEvents()));
            result.setConflicts(safeList(result.getConflicts()));
            result.setEmotionChanges(safeList(result.getEmotionChanges()));
            result.setForeshadowing(safeList(result.getForeshadowing()));
            result.setKeyDialogues(safeList(result.getKeyDialogues()));
            result.setSourceRefs(resolveSourceRefs(result.getSourceRefs(), normalizedChapter.getChapterNo()));
            return result;
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("章节上下文抽取结果无法解析为 JSON。", exception);
        }
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }

    private List<String> resolveSourceRefs(List<String> sourceRefs, Integer chapterNo) {
        List<String> safeSourceRefs = safeList(sourceRefs);
        if (!safeSourceRefs.isEmpty()) {
            return safeSourceRefs;
        }

        return List.of("chapter:" + chapterNo);
    }
}
