package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.SourceChapterMapper;
import com.qiniuyun.novelscript.service.SourceChapterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小说章节的业务服务实现。
 */
@Slf4j
@Service
public class SourceChapterServiceImpl implements SourceChapterService {

    private final ProjectMapper projectMapper;
    private final SourceChapterMapper sourceChapterMapper;

    public SourceChapterServiceImpl(ProjectMapper projectMapper, SourceChapterMapper sourceChapterMapper) {
        this.projectMapper = projectMapper;
        this.sourceChapterMapper = sourceChapterMapper;
    }

    /**
     * 在指定项目下保存一章小说内容。
     *
     * @param projectId 项目 ID
     * @param request 章节保存请求
     * @return 保存后的章节响应
     */
    @Override
    @Transactional
    public SourceChapterResponse createChapter(Long projectId, SourceChapterCreateRequest request) {
        log.info(
            "【章节服务】开始保存章节，项目ID：{}，章节号：{}，正文长度：{}",
            projectId,
            request.getChapterNo(),
            request.getContent().length()
        );
        ensureProjectExists(projectId);

        SourceChapter chapter = new SourceChapter();
        chapter.setProjectId(projectId);
        chapter.setChapterNo(request.getChapterNo());
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setWordCount(resolveWordCount(request));
        sourceChapterMapper.insert(chapter);
        log.info(
            "【章节服务】章节保存成功，章节ID：{}，项目ID：{}，章节号：{}，字数：{}",
            chapter.getId(),
            projectId,
            chapter.getChapterNo(),
            chapter.getWordCount()
        );
        return SourceChapterResponse.from(chapter);
    }

    /**
     * 判断项目是否达到最小章节数量要求。
     *
     * @param projectId 项目 ID
     * @param minimumCount 最小章节数
     * @return 是否已达到最小要求
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasMinimumChapterCount(Long projectId, int minimumCount) {
        Long count = sourceChapterMapper.selectCount(
            new LambdaQueryWrapper<SourceChapter>().eq(SourceChapter::getProjectId, projectId)
        );
        log.info(
            "【章节服务】检查最小章节数，项目ID：{}，当前章节数：{}，要求最少：{}",
            projectId,
            count == null ? 0 : count,
            minimumCount
        );
        return count != null && count >= minimumCount;
    }

    private void ensureProjectExists(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在");
        }
        log.info("【章节服务】已确认项目存在，项目ID：{}，项目标题：{}", projectId, project.getTitle());
    }

    private Integer resolveWordCount(SourceChapterCreateRequest request) {
        if (request.getWordCount() != null) {
            return request.getWordCount();
        }
        return request.getContent().length();
    }
}
