package com.qiniuyun.novelscript.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiniuyun.novelscript.common.exception.ResourceNotFoundException;
import com.qiniuyun.novelscript.controller.request.ProjectCreateRequest;
import com.qiniuyun.novelscript.controller.response.ProjectResponse;
import com.qiniuyun.novelscript.domain.entity.Project;
import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import com.qiniuyun.novelscript.mapper.ProjectMapper;
import com.qiniuyun.novelscript.mapper.SourceChapterMapper;
import com.qiniuyun.novelscript.service.ProjectService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 改编项目的业务服务实现。
 */
@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final SourceChapterMapper sourceChapterMapper;

    public ProjectServiceImpl(ProjectMapper projectMapper, SourceChapterMapper sourceChapterMapper) {
        this.projectMapper = projectMapper;
        this.sourceChapterMapper = sourceChapterMapper;
    }

    /**
     * 创建新的改编项目。
     *
     * @param request 创建项目请求
     * @return 创建后的项目响应
     */
    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        log.info("【项目服务】开始创建项目，标题：{}", request.getTitle());
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setStatus("DRAFT");
        projectMapper.insert(project);
        log.info("【项目服务】项目创建成功，项目ID：{}，状态：{}", project.getId(), project.getStatus());
        return ProjectResponse.from(project, 0);
    }

    /**
     * 查询当前所有改编项目摘要。
     *
     * @return 项目列表响应
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        log.info("【项目服务】开始查询项目列表");
        List<Project> projects = projectMapper.selectList(
            new LambdaQueryWrapper<Project>().orderByDesc(Project::getUpdatedAt).orderByDesc(Project::getId)
        );
        List<ProjectResponse> responses = projects.stream()
            .map(this::toProjectResponse)
            .collect(Collectors.toList());
        log.info("【项目服务】项目列表查询完成，项目数量：{}", responses.size());
        return responses;
    }

    /**
     * 查询指定项目详情。
     *
     * @param projectId 项目 ID
     * @return 项目响应
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        log.info("【项目服务】开始查询项目详情，项目ID：{}", projectId);
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在");
        }

        Long chapterCount = sourceChapterMapper.selectCount(
            new LambdaQueryWrapper<SourceChapter>().eq(SourceChapter::getProjectId, projectId)
        );
        log.info("【项目服务】项目查询完成，项目ID：{}，章节数：{}", projectId, chapterCount == null ? 0 : chapterCount);
        return ProjectResponse.from(project, chapterCount == null ? 0 : chapterCount.intValue());
    }

    private ProjectResponse toProjectResponse(Project project) {
        Long chapterCount = sourceChapterMapper.selectCount(
            new LambdaQueryWrapper<SourceChapter>().eq(SourceChapter::getProjectId, project.getId())
        );
        return ProjectResponse.from(project, chapterCount == null ? 0 : chapterCount.intValue());
    }
}
