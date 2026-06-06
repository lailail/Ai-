package com.qiniuyun.novelscript.pipeline.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeInput;
import com.qiniuyun.novelscript.pipeline.model.ChapterNormalizeResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 验证章节标准化步骤的核心行为。
 */
class ChapterNormalizeStepTests {

    private final ChapterNormalizeStep chapterNormalizeStep = new ChapterNormalizeStep();

    /**
     * 验证三章输入能够被标准化，并且缺失标题时会自动提取。
     */
    @Test
    void shouldNormalizeThreeChaptersAndExtractMissingTitle() {
        ChapterNormalizeInput input = new ChapterNormalizeInput();
        input.setProjectId(1001L);
        input.setChapters(List.of(
            buildChapter(1001L, 3, "  Chapter 3 Echo  ", "  Aftershock remains.  \n\n  The gate closes again.  "),
            buildChapter(1001L, 1, "", "\n Chapter 1 Rainy Arrival \n\n Night presses down.\n\n\n Shen Yan steps into the alley.\n"),
            buildChapter(1001L, 2, "Chapter 2 Token", "The token falls.\n\n Lao Zhou does not look back.")
        ));

        ChapterNormalizeResult result = chapterNormalizeStep.execute(input);

        assertThat(result.getProjectId()).isEqualTo(1001L);
        assertThat(result.getChapterCount()).isEqualTo(3);
        assertThat(result.getNormalizedChapters()).hasSize(3);
        assertThat(result.getNormalizedChapters().get(0).getChapterNo()).isEqualTo(1);
        assertThat(result.getNormalizedChapters().get(0).getTitle()).isEqualTo("Chapter 1 Rainy Arrival");
        assertThat(result.getNormalizedChapters().get(0).getContent()).isEqualTo(
            "Chapter 1 Rainy Arrival\n\nNight presses down.\n\nShen Yan steps into the alley."
        );
        assertThat(result.getNormalizedChapters().get(0).getWordCount()).isGreaterThan(0);
        assertThat(result.getNormalizedChapters().get(2).getTitle()).isEqualTo("Chapter 3 Echo");
        assertThat(result.getTotalWordCount()).isEqualTo(
            result.getNormalizedChapters().stream().mapToInt(chapter -> chapter.getWordCount()).sum()
        );
    }

    /**
     * 验证章节数量不足三章时会被拒绝。
     */
    @Test
    void shouldRejectWhenChapterCountIsLessThanThree() {
        ChapterNormalizeInput input = new ChapterNormalizeInput();
        input.setProjectId(1002L);
        input.setChapters(List.of(
            buildChapter(1002L, 1, "Chapter 1", "Rainy arrival."),
            buildChapter(1002L, 2, "Chapter 2", "The token drops.")
        ));

        assertThatThrownBy(() -> chapterNormalizeStep.execute(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("3");
    }

    /**
     * 构造测试用原始章节实体。
     *
     * @param projectId 项目 ID
     * @param chapterNo 章节号
     * @param title 标题
     * @param content 正文
     * @return 原始章节实体
     */
    private SourceChapter buildChapter(Long projectId, Integer chapterNo, String title, String content) {
        SourceChapter chapter = new SourceChapter();
        chapter.setProjectId(projectId);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setContent(content);
        return chapter;
    }
}
