package com.qiniuyun.novelscript.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiniuyun.novelscript.domain.entity.ChapterContext;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.domain.entity.StoryBible;
import com.qiniuyun.novelscript.mapper.ChapterContextMapper;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.SourceChapterMapper;
import com.qiniuyun.novelscript.mapper.StoryBibleMapper;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleCharacter;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证上下文快照保存服务的落库行为。
 */
@SpringBootTest
@ActiveProfiles("test")
class ContextSnapshotServiceIntegrationTests {

    @Autowired
    private ContextSnapshotService contextSnapshotService;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private SourceChapterMapper sourceChapterMapper;

    @Autowired
    private ChapterContextMapper chapterContextMapper;

    @Autowired
    private StoryBibleMapper storyBibleMapper;

    /**
     * 验证章节上下文和 Story Bible 快照都能成功保存并递增版本号。
     */
    @Test
    void test_p3_c3_context_snapshot_save() {
        Long projectId = createProject();
        createChapter(projectId, 1, "Chapter 1", "Shen Yan enters Stone Alley.");
        createChapter(projectId, 2, "Chapter 2", "Lao Zhou brings the token.");
        createChapter(projectId, 3, "Chapter 3", "Lin Wan reveals the old case.");

        contextSnapshotService.saveChapterContexts(buildChapterContexts(projectId));

        Long firstStoryBibleId = contextSnapshotService.saveStoryBible(buildStoryBible(projectId), List.of(1L, 2L, 3L));
        Long secondStoryBibleId = contextSnapshotService.saveStoryBible(buildStoryBible(projectId), List.of(1L, 2L, 3L));

        assertThat(firstStoryBibleId).isNotNull();
        assertThat(secondStoryBibleId).isNotNull();
        assertThat(secondStoryBibleId).isNotEqualTo(firstStoryBibleId);

        List<ChapterContext> chapterContexts = chapterContextMapper.selectList(
            new LambdaQueryWrapper<ChapterContext>().eq(ChapterContext::getProjectId, projectId)
        );
        assertThat(chapterContexts).hasSize(3);
        assertThat(chapterContexts)
            .extracting(ChapterContext::getStatus)
            .containsOnly("SUCCEEDED");

        List<StoryBible> storyBibles = storyBibleMapper.selectList(
            new LambdaQueryWrapper<StoryBible>()
                .eq(StoryBible::getProjectId, projectId)
                .orderByAsc(StoryBible::getVersionNo)
        );
        assertThat(storyBibles).hasSize(2);
        assertThat(storyBibles)
            .extracting(StoryBible::getVersionNo)
            .containsExactly(1, 2);
        assertThat(storyBibles.get(0).getSourceContextIds()).contains("[1,2,3]");
    }

    /**
     * 构造测试用项目实体。
     *
     * @return 项目 ID
     */
    private Long createProject() {
        Project project = new Project();
        project.setTitle("Long Night Ember");
        project.setDescription("Used to verify context snapshots");
        project.setStatus("DRAFT");
        projectMapper.insert(project);
        return project.getId();
    }

    /**
     * 构造并保存测试用章节。
     *
     * @param projectId 项目 ID
     * @param chapterNo 章节号
     * @param title 标题
     * @param content 正文
     */
    private void createChapter(Long projectId, Integer chapterNo, String title, String content) {
        SourceChapter chapter = new SourceChapter();
        chapter.setProjectId(projectId);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setWordCount(content.length());
        sourceChapterMapper.insert(chapter);
    }

    /**
     * 构造测试用章节上下文列表。
     *
     * @param projectId 项目 ID
     * @return 章节上下文列表
     */
    private List<ChapterContextResult> buildChapterContexts(Long projectId) {
        return List.of(
            createChapterContext(projectId, 1, "Chapter 1", "Shen Yan enters Stone Alley."),
            createChapterContext(projectId, 2, "Chapter 2", "Lao Zhou brings the token."),
            createChapterContext(projectId, 3, "Chapter 3", "Lin Wan reveals the old case.")
        );
    }

    /**
     * 构造单条章节上下文测试数据。
     *
     * @param projectId 项目 ID
     * @param chapterNo 章节号
     * @param title 标题
     * @param summary 摘要
     * @return 单章上下文结果
     */
    private ChapterContextResult createChapterContext(Long projectId, Integer chapterNo, String title, String summary) {
        ChapterContextResult result = new ChapterContextResult();
        result.setProjectId(projectId);
        result.setChapterNo(chapterNo);
        result.setChapterTitle(title);
        result.setSummary(summary);
        result.setCharacters(List.of("Character" + chapterNo));
        result.setLocations(List.of("Location" + chapterNo));
        result.setEvents(List.of("Event" + chapterNo));
        result.setConflicts(List.of("Conflict" + chapterNo));
        result.setEmotionChanges(List.of("Emotion" + chapterNo));
        result.setForeshadowing(List.of("Foreshadowing" + chapterNo));
        result.setKeyDialogues(List.of("Dialogue" + chapterNo));
        result.setSourceRefs(List.of("chapter:" + chapterNo));
        return result;
    }

    /**
     * 构造测试用 Story Bible 结果。
     *
     * @param projectId 项目 ID
     * @return Story Bible 结果
     */
    private StoryBibleResult buildStoryBible(Long projectId) {
        StoryBibleResult result = new StoryBibleResult();
        result.setProjectId(projectId);

        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("Shen Yan");
        character.setRole("protagonist");
        character.setGoal("Find the truth of the old case");
        character.setTraits(List.of("calm", "sharp"));

        result.setCharacters(List.of(character));
        result.setAdaptationStrategy(List.of("Keep the suspense atmosphere"));
        return result;
    }
}
