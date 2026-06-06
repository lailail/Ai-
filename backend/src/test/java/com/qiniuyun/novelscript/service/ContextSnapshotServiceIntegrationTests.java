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

    @Test
    void test_p3_c3_context_snapshot_save() {
        Long projectId = createProject();
        createChapter(projectId, 1, "第一章", "沈砚走进青石巷。");
        createChapter(projectId, 2, "第二章", "老周带来铜牌。");
        createChapter(projectId, 3, "第三章", "林晚说出旧案。");

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

    private Long createProject() {
        Project project = new Project();
        project.setTitle("长夜余烬");
        project.setDescription("用于验证上下文快照");
        project.setStatus("DRAFT");
        projectMapper.insert(project);
        return project.getId();
    }

    private void createChapter(Long projectId, Integer chapterNo, String title, String content) {
        SourceChapter chapter = new SourceChapter();
        chapter.setProjectId(projectId);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setWordCount(content.length());
        sourceChapterMapper.insert(chapter);
    }

    private List<ChapterContextResult> buildChapterContexts(Long projectId) {
        return List.of(
            createChapterContext(projectId, 1, "第一章", "沈砚进入青石巷。"),
            createChapterContext(projectId, 2, "第二章", "老周带来铜牌。"),
            createChapterContext(projectId, 3, "第三章", "林晚说出旧案。")
        );
    }

    private ChapterContextResult createChapterContext(Long projectId, Integer chapterNo, String title, String summary) {
        ChapterContextResult result = new ChapterContextResult();
        result.setProjectId(projectId);
        result.setChapterNo(chapterNo);
        result.setChapterTitle(title);
        result.setSummary(summary);
        result.setCharacters(List.of("角色" + chapterNo));
        result.setLocations(List.of("地点" + chapterNo));
        result.setEvents(List.of("事件" + chapterNo));
        result.setConflicts(List.of("冲突" + chapterNo));
        result.setEmotionChanges(List.of("情绪" + chapterNo));
        result.setForeshadowing(List.of("伏笔" + chapterNo));
        result.setKeyDialogues(List.of("对白" + chapterNo));
        result.setSourceRefs(List.of("chapter:" + chapterNo));
        return result;
    }

    private StoryBibleResult buildStoryBible(Long projectId) {
        StoryBibleResult result = new StoryBibleResult();
        result.setProjectId(projectId);

        StoryBibleCharacter character = new StoryBibleCharacter();
        character.setId("char_shenyan");
        character.setName("沈砚");
        character.setRole("protagonist");
        character.setGoal("查明旧案真相");
        character.setTraits(List.of("冷静", "敏锐"));

        result.setCharacters(List.of(character));
        result.setAdaptationStrategy(List.of("保留悬疑气质"));
        return result;
    }
}
