package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.config.ai.DeepSeekProperties;
import com.qiniuyun.novelscript.domain.entity.ChapterContext;
import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.domain.entity.StoryBible;
import com.qiniuyun.novelscript.mapper.ChapterContextMapper;
import com.qiniuyun.novelscript.mapper.SourceChapterMapper;
import com.qiniuyun.novelscript.mapper.StoryBibleMapper;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import com.qiniuyun.novelscript.service.ContextSnapshotService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 改编上下文快照保存服务实现。
 */
@Slf4j
@Service
public class ContextSnapshotServiceImpl implements ContextSnapshotService {

    private final SourceChapterMapper sourceChapterMapper;
    private final ChapterContextMapper chapterContextMapper;
    private final StoryBibleMapper storyBibleMapper;
    private final ObjectMapper objectMapper;
    private final DeepSeekProperties deepSeekProperties;

    public ContextSnapshotServiceImpl(
        SourceChapterMapper sourceChapterMapper,
        ChapterContextMapper chapterContextMapper,
        StoryBibleMapper storyBibleMapper,
        ObjectMapper objectMapper,
        DeepSeekProperties deepSeekProperties
    ) {
        this.sourceChapterMapper = sourceChapterMapper;
        this.chapterContextMapper = chapterContextMapper;
        this.storyBibleMapper = storyBibleMapper;
        this.objectMapper = objectMapper;
        this.deepSeekProperties = deepSeekProperties;
    }

    /**
     * 批量保存单章上下文快照。
     *
     * @param chapterContexts 单章上下文结果列表
     */
    @Override
    @Transactional
    public void saveChapterContexts(List<ChapterContextResult> chapterContexts) {
        if (CollectionUtils.isEmpty(chapterContexts)) {
            log.info("【上下文快照】本次没有可保存的单章上下文。");
            return;
        }

        for (ChapterContextResult chapterContextResult : chapterContexts) {
            SourceChapter chapter = findChapter(chapterContextResult.getProjectId(), chapterContextResult.getChapterNo());
            ChapterContext entity = findExistingChapterContext(chapterContextResult.getProjectId(), chapter.getId());
            if (entity == null) {
                entity = new ChapterContext();
                entity.setProjectId(chapterContextResult.getProjectId());
                entity.setChapterId(chapter.getId());
            }

            entity.setContextJson(writeAsJson(chapterContextResult));
            entity.setModelName(deepSeekProperties.getModel());
            entity.setStatus("SUCCEEDED");

            if (entity.getId() == null) {
                chapterContextMapper.insert(entity);
            }
            else {
                chapterContextMapper.updateById(entity);
            }
        }

        Long projectId = chapterContexts.get(0).getProjectId();
        log.info("【上下文快照】单章上下文保存完成，项目ID：{}，数量：{}", projectId, chapterContexts.size());
    }

    /**
     * 保存 Story Bible 快照，并自动创建新版本。
     *
     * @param storyBibleResult Story Bible 结构化结果
     * @param sourceContextIds 章节上下文快照 ID 列表
     * @return 保存后的 Story Bible 快照 ID
     */
    @Override
    @Transactional
    public Long saveStoryBible(StoryBibleResult storyBibleResult, List<Long> sourceContextIds) {
        if (storyBibleResult == null || storyBibleResult.getProjectId() == null) {
            throw new IllegalArgumentException("保存 Story Bible 时项目ID不能为空。");
        }

        StoryBible storyBible = new StoryBible();
        storyBible.setProjectId(storyBibleResult.getProjectId());
        storyBible.setBibleJson(writeAsJson(storyBibleResult));
        storyBible.setVersionNo(resolveNextVersionNo(storyBibleResult.getProjectId()));
        storyBible.setSourceContextIds(writeAsJson(sourceContextIds));
        storyBibleMapper.insert(storyBible);

        log.info(
            "【上下文快照】Story Bible 保存完成，项目ID：{}，版本号：{}，快照ID：{}",
            storyBibleResult.getProjectId(),
            storyBible.getVersionNo(),
            storyBible.getId()
        );
        return storyBible.getId();
    }

    private SourceChapter findChapter(Long projectId, Integer chapterNo) {
        SourceChapter chapter = sourceChapterMapper.selectOne(
            new LambdaQueryWrapper<SourceChapter>()
                .eq(SourceChapter::getProjectId, projectId)
                .eq(SourceChapter::getChapterNo, chapterNo)
                .last("limit 1")
        );
        if (chapter == null) {
            throw new ResourceNotFoundException("未找到对应的原始章节，无法保存章节上下文。");
        }
        return chapter;
    }

    private ChapterContext findExistingChapterContext(Long projectId, Long chapterId) {
        return chapterContextMapper.selectOne(
            new LambdaQueryWrapper<ChapterContext>()
                .eq(ChapterContext::getProjectId, projectId)
                .eq(ChapterContext::getChapterId, chapterId)
                .last("limit 1")
        );
    }

    private Integer resolveNextVersionNo(Long projectId) {
        StoryBible latestStoryBible = storyBibleMapper.selectOne(
            new LambdaQueryWrapper<StoryBible>()
                .eq(StoryBible::getProjectId, projectId)
                .orderByDesc(StoryBible::getVersionNo)
                .last("limit 1")
        );
        if (latestStoryBible == null || latestStoryBible.getVersionNo() == null) {
            return 1;
        }
        return latestStoryBible.getVersionNo() + 1;
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("上下文快照序列化失败。", exception);
        }
    }
}
