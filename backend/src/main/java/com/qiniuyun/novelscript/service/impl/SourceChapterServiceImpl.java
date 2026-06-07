package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.request.SourceChapterUpdateRequest;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.SourceChapterMapper;
import com.qiniuyun.novelscript.service.SourceChapterService;
import java.util.List;
import java.util.stream.Collectors;
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

    /**
     * 构造章节服务实现。
     *
     * @param projectMapper 项目 Mapper
     * @param sourceChapterMapper 章节 Mapper
     */
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
        chapter.setWordCount(resolveWordCount(request.getContent(), request.getWordCount()));
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
     * 更新指定项目下某一章的标题和正文。
     *
     * @param projectId 项目 ID
     * @param chapterId 章节 ID
     * @param request 章节更新请求
     * @return 更新后的章节响应
     */
    @Override
    @Transactional
    public SourceChapterResponse updateChapter(Long projectId, Long chapterId, SourceChapterUpdateRequest request) {
        ensureProjectExists(projectId);
        SourceChapter chapter = getProjectChapter(projectId, chapterId);
        log.info("【章节服务】开始更新章节，项目ID：{}，章节ID：{}，章节号：{}", projectId, chapterId, chapter.getChapterNo());

        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setWordCount(resolveWordCount(request.getContent(), request.getWordCount()));
        sourceChapterMapper.updateById(chapter);

        log.info("【章节服务】章节更新成功，项目ID：{}，章节ID：{}，最新字数：{}", projectId, chapterId, chapter.getWordCount());
        return SourceChapterResponse.from(chapter);
    }

    /**
     * 查询指定项目下已经保存的章节列表。
     *
     * @param projectId 项目 ID
     * @return 章节列表响应
     */
    @Override
    @Transactional(readOnly = true)
    public List<SourceChapterResponse> listChapters(Long projectId) {
        ensureProjectExists(projectId);
        log.info("【章节服务】开始查询章节列表，项目ID：{}", projectId);
        List<SourceChapter> chapters = sourceChapterMapper.selectList(
            new LambdaQueryWrapper<SourceChapter>()
                .eq(SourceChapter::getProjectId, projectId)
                .orderByAsc(SourceChapter::getChapterNo)
                .orderByAsc(SourceChapter::getId)
        );
        List<SourceChapterResponse> responses = chapters.stream()
            .map(SourceChapterResponse::from)
            .collect(Collectors.toList());
        log.info("【章节服务】章节列表查询完成，项目ID：{}，章节数：{}", projectId, responses.size());
        return responses;
    }

    /**
     * 判断项目是否达到最小章节数量要求。
     *
     * @param projectId 项目 ID
     * @param minimumCount 最小章节数
     * @return 是否满足最小章节数要求
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

    /**
     * 校验项目是否存在，避免向无效项目写入章节。
     *
     * @param projectId 项目 ID
     */
    private void ensureProjectExists(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在。");
        }
        log.info("【章节服务】已确认项目存在，项目ID：{}，项目标题：{}", projectId, project.getTitle());
    }

    /**
     * 根据项目和章节 ID 查找章节实体，避免跨项目误修改。
     *
     * @param projectId 项目 ID
     * @param chapterId 章节 ID
     * @return 当前项目下的章节实体
     */
    private SourceChapter getProjectChapter(Long projectId, Long chapterId) {
        SourceChapter chapter = sourceChapterMapper.selectOne(
            new LambdaQueryWrapper<SourceChapter>()
                .eq(SourceChapter::getId, chapterId)
                .eq(SourceChapter::getProjectId, projectId)
                .last("LIMIT 1")
        );
        if (chapter == null) {
            throw new ResourceNotFoundException("章节不存在。");
        }
        return chapter;
    }

    /**
     * 统一解析章节字数，未显式传入时由正文长度计算。
     *
     * @param content 章节正文
     * @param wordCount 前端传入的章节字数
     * @return 最终入库的章节字数
     */
    private Integer resolveWordCount(String content, Integer wordCount) {
        if (wordCount != null) {
            return wordCount;
        }
        return content.length();
    }
}
