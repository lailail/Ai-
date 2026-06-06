package com.qiniuyun.novelscript.pipeline.step;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeInput;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeResult;
import com.qiniuyun.novelscript.pipeline.model.NormalizedChapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 负责章节数量校验、正文清洗、标题提取和字数统计。
 */
@Slf4j
@Component
public class ChapterNormalizeStep {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 对原始章节列表执行标准化处理。
     *
     * @param input 标准化输入
     * @return 标准化输出
     */
    public ChapterNormalizeResult execute(ChapterNormalizeInput input) {
        validateInput(input);
        List<SourceChapter> sourceChapters = input.getChapters().stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(SourceChapter::getChapterNo))
            .toList();

        if (sourceChapters.size() < 3) {
            throw new IllegalArgumentException("小说改编至少需要 3 个章节。");
        }

        log.info("【章节标准化】开始处理章节，项目ID：{}，章节数：{}", input.getProjectId(), sourceChapters.size());
        List<NormalizedChapter> normalizedChapters = new ArrayList<>();
        int totalWordCount = 0;
        Long projectId = resolveProjectId(input, sourceChapters);

        for (SourceChapter sourceChapter : sourceChapters) {
            NormalizedChapter normalizedChapter = normalizeChapter(projectId, sourceChapter);
            totalWordCount += normalizedChapter.getWordCount();
            normalizedChapters.add(normalizedChapter);
        }

        ChapterNormalizeResult result = new ChapterNormalizeResult();
        result.setProjectId(projectId);
        result.setChapterCount(normalizedChapters.size());
        result.setNormalizedChapters(normalizedChapters);
        result.setTotalWordCount(totalWordCount);
        log.info("【章节标准化】处理完成，项目ID：{}，总字数：{}", projectId, totalWordCount);
        return result;
    }

    private void validateInput(ChapterNormalizeInput input) {
        if (input == null || input.getChapters() == null) {
            throw new IllegalArgumentException("章节标准化输入不能为空。");
        }
    }

    private Long resolveProjectId(ChapterNormalizeInput input, List<SourceChapter> sourceChapters) {
        if (input.getProjectId() != null) {
            return input.getProjectId();
        }

        return sourceChapters.stream()
            .map(SourceChapter::getProjectId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private NormalizedChapter normalizeChapter(Long projectId, SourceChapter sourceChapter) {
        if (sourceChapter.getChapterNo() == null) {
            throw new IllegalArgumentException("章节号不能为空。");
        }

        String normalizedContent = normalizeContent(sourceChapter.getContent());
        String normalizedTitle = normalizeTitle(sourceChapter.getTitle(), normalizedContent, sourceChapter.getChapterNo());
        int wordCount = countWords(normalizedContent);

        NormalizedChapter normalizedChapter = new NormalizedChapter();
        normalizedChapter.setProjectId(projectId != null ? projectId : sourceChapter.getProjectId());
        normalizedChapter.setChapterNo(sourceChapter.getChapterNo());
        normalizedChapter.setTitle(normalizedTitle);
        normalizedChapter.setContent(normalizedContent);
        normalizedChapter.setWordCount(wordCount);
        return normalizedChapter;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("章节正文不能为空。");
        }

        String[] rawLines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> normalizedLines = new ArrayList<>();
        boolean previousLineBlank = false;

        for (String rawLine : rawLines) {
            String trimmedLine = rawLine == null ? "" : rawLine.trim();
            if (!StringUtils.hasText(trimmedLine)) {
                if (!normalizedLines.isEmpty() && !previousLineBlank) {
                    normalizedLines.add("");
                    previousLineBlank = true;
                }
                continue;
            }

            normalizedLines.add(trimmedLine);
            previousLineBlank = false;
        }

        while (!normalizedLines.isEmpty() && normalizedLines.get(normalizedLines.size() - 1).isEmpty()) {
            normalizedLines.remove(normalizedLines.size() - 1);
        }

        if (normalizedLines.isEmpty()) {
            throw new IllegalArgumentException("章节正文清洗后为空。");
        }

        return String.join("\n", normalizedLines);
    }

    private String normalizeTitle(String title, String normalizedContent, Integer chapterNo) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }

        return normalizedContent.lines()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("第 " + chapterNo + " 章缺少可提取标题。"));
    }

    private int countWords(String normalizedContent) {
        String compactContent = WHITESPACE_PATTERN.matcher(normalizedContent).replaceAll("");
        return compactContent.length();
    }
}
