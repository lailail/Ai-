package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeInput;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeResult;
import org.junit.jupiter.api.Test;

/**
 * 验证章节标准化步骤的核心行为。
 */
class ChapterNormalizeStepTests {

    private final ChapterNormalizeStep chapterNormalizeStep = new ChapterNormalizeStep();

    @Test
    void shouldNormalizeThreeChaptersAndExtractMissingTitle() {
        ChapterNormalizeInput input = new ChapterNormalizeInput();
        input.setProjectId(1001L);
        input.setChapters(List.of(
            buildChapter(1001L, 3, "  第三章 余波  ", "  余波未平。  \n\n  城门再度关闭。 "),
            buildChapter(1001L, 1, "", "\n 第一章 夜雨入城 \n\n 夜色压城。 \n\n\n 沈砚第一次走进青石巷。 \n"),
            buildChapter(1001L, 2, "第二章 铜牌", "铜牌落地。\n\n  老周没有回头。")
        ));

        ChapterNormalizeResult result = chapterNormalizeStep.execute(input);

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getChapterCount()).isEqualTo(3);
        assertThat(result.getNormalizedChapters()).hasSize(3);
        assertThat(result.getNormalizedChapters().get(0).getChapterNo()).isEqualTo(1);
        assertThat(result.getNormalizedChapters().get(0).getTitle()).isEqualTo("第一章 夜雨入城");
        assertThat(result.getNormalizedChapters().get(0).getContent()).isEqualTo("第一章 夜雨入城\n\n夜色压城。\n\n沈砚第一次走进青石巷。");
        assertThat(result.getNormalizedChapters().get(0).getWordCount()).isGreaterThan(0);
        assertThat(result.getNormalizedChapters().get(2).getTitle()).isEqualTo("第三章 余波");
        assertThat(result.getTotalWordCount()).isEqualTo(
            result.getNormalizedChapters().stream().mapToInt(chapter -> chapter.getWordCount()).sum()
        );
    }

    @Test
    void shouldRejectWhenChapterCountIsLessThanThree() {
        ChapterNormalizeInput input = new ChapterNormalizeInput();
        input.setProjectId(1002L);
        input.setChapters(List.of(
            buildChapter(1002L, 1, "第一章", "雨夜入城。"),
            buildChapter(1002L, 2, "第二章", "铜牌落地。")
        ));

        assertThatThrownBy(() -> chapterNormalizeStep.execute(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("至少")
            .hasMessageContaining("3");
    }

    private SourceChapter buildChapter(Long projectId, Integer chapterNo, String title, String content) {
        SourceChapter chapter = new SourceChapter();
        chapter.setProjectId(projectId);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setContent(content);
        return chapter;
    }
}
