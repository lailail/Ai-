package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.response.StoryBibleResponse;
import com.qiniuyun.novelscript.pipeline.model.ChapterContextResult;
import com.qiniuyun.novelscript.pipeline.model.StoryBibleResult;
import java.util.List;

/**
 * 负责保存和查询改编流程中的长期上下文快照。
 */
public interface ContextSnapshotService {

    /**
     * 保存单章上下文快照。
     *
     * @param chapterContexts 单章上下文结果列表
     */
    void saveChapterContexts(List<ChapterContextResult> chapterContexts);

    /**
     * 保存 Story Bible 快照，并自动生成新版本号。
     *
     * @param storyBibleResult Story Bible 结构化结果
     * @param sourceContextIds 参与本次构建的章节上下文快照 ID 列表
     * @return 保存后的 Story Bible 快照 ID
     */
    Long saveStoryBible(StoryBibleResult storyBibleResult, List<Long> sourceContextIds);

    /**
     * 查询指定项目当前最新的 Story Bible 快照。
     *
     * @param projectId 项目 ID
     * @return 最新 Story Bible 响应
     */
    StoryBibleResponse getLatestStoryBible(Long projectId);
}
