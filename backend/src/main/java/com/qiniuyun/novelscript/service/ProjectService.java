package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.request.ProjectCreateRequest;
import com.qiniuyun.novelscript.controller.response.ProjectResponse;
import java.util.List;

/**
 * 改编项目的业务服务接口。
 */
public interface ProjectService {

    /**
     * 创建新的改编项目。
     *
     * @param request 创建项目请求
     * @return 创建后的项目响应
     */
    ProjectResponse createProject(ProjectCreateRequest request);

    /**
     * 查询当前所有改编项目摘要。
     *
     * @return 项目列表响应
     */
    List<ProjectResponse> listProjects();

    /**
     * 查询指定项目详情。
     *
     * @param projectId 项目 ID
     * @return 项目响应
     */
    ProjectResponse getProject(Long projectId);
}
