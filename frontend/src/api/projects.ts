import { ApiRequestError, requestJson } from "./http";
import type {
  AdaptationJobSnapshot,
  AdaptationScript,
  ScreenplaySnapshot,
  ScriptValidationResult,
  ScriptVersionSummary,
  StoryBibleSnapshot
} from "../types/adaptation";
import type {
  CreateChapterPayload,
  CreateProjectPayload,
  Project,
  SourceChapter,
  UpdateChapterPayload
} from "../types/project";
import { extractDownloadFileName } from "../utils/download";

type DownloadFileResult = {
  blob: Blob;
  fileName: string;
};

/**
 * 查询项目列表。
 *
 * @returns 全部项目摘要
 */
export function listProjects() {
  return requestJson<Project[]>("/api/projects");
}

/**
 * 创建改编项目。
 *
 * @param payload 项目创建参数
 * @returns 新建项目
 */
export function createProject(payload: CreateProjectPayload) {
  return requestJson<Project>("/api/projects", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

/**
 * 查询单个项目详情。
 *
 * @param projectId 项目 ID
 * @returns 项目详情
 */
export function getProject(projectId: number) {
  return requestJson<Project>(`/api/projects/${projectId}`);
}

/**
 * 查询项目章节列表。
 *
 * @param projectId 项目 ID
 * @returns 已保存章节列表
 */
export function listProjectChapters(projectId: number) {
  return requestJson<SourceChapter[]>(`/api/projects/${projectId}/chapters`);
}

/**
 * 创建章节。
 *
 * @param projectId 项目 ID
 * @param payload 章节创建参数
 * @returns 保存后的章节
 */
export function createProjectChapter(projectId: number, payload: CreateChapterPayload) {
  return requestJson<SourceChapter>(`/api/projects/${projectId}/chapters`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

/**
 * 更新章节标题和正文。
 *
 * @param projectId 项目 ID
 * @param chapterId 章节 ID
 * @param payload 章节更新参数
 * @returns 更新后的章节
 */
export function updateProjectChapter(projectId: number, chapterId: number, payload: UpdateChapterPayload) {
  return requestJson<SourceChapter>(`/api/projects/${projectId}/chapters/${chapterId}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

/**
 * 触发项目改编。
 *
 * @param projectId 项目 ID
 * @returns 最新任务快照
 */
export function generateProjectScript(projectId: number) {
  return requestJson<AdaptationJobSnapshot>(`/api/projects/${projectId}/adaptations`, {
    method: "POST"
  });
}

/**
 * 查询最新改编任务。
 *
 * @param projectId 项目 ID
 * @returns 最新任务，不存在时返回 null
 */
export async function getLatestAdaptationJob(projectId: number) {
  try {
    return await requestJson<AdaptationJobSnapshot>(`/api/projects/${projectId}/adaptations/latest-job`);
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * 查询最新 YAML 剧本版本。
 *
 * @param projectId 项目 ID
 * @returns 最新剧本，不存在时返回 null
 */
export async function getLatestProjectScript(projectId: number) {
  try {
    return await requestJson<AdaptationScript>(`/api/projects/${projectId}/scripts/latest`);
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * 查询剧本版本列表。
 *
 * @param projectId 项目 ID
 * @returns 剧本版本摘要列表
 */
export function listProjectScriptVersions(projectId: number) {
  return requestJson<ScriptVersionSummary[]>(`/api/projects/${projectId}/scripts`);
}

/**
 * 查询指定剧本版本详情。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @returns 剧本版本详情
 */
export function getProjectScriptVersion(projectId: number, scriptVersionId: number) {
  return requestJson<AdaptationScript>(`/api/projects/${projectId}/scripts/${scriptVersionId}`);
}

/**
 * 执行 YAML Schema 校验。
 *
 * @param projectId 项目 ID
 * @param yamlContent 待校验内容
 * @returns 结构化校验结果
 */
export function validateProjectScript(projectId: number, yamlContent: string) {
  return requestJson<ScriptValidationResult>(`/api/projects/${projectId}/scripts/validate`, {
    method: "POST",
    body: JSON.stringify({ yamlContent })
  });
}

/**
 * 将 YAML 另存为新版本。
 *
 * @param projectId 项目 ID
 * @param title 新版本标题
 * @param yamlContent YAML 原文
 * @returns 新剧本版本详情
 */
export function saveProjectScriptVersion(projectId: number, title: string, yamlContent: string) {
  return requestJson<AdaptationScript>(`/api/projects/${projectId}/scripts`, {
    method: "POST",
    body: JSON.stringify({ title, yamlContent })
  });
}

/**
 * 查询最新正式剧本。
 *
 * @param projectId 项目 ID
 * @returns 最新正式剧本，不存在时返回 null
 */
export async function getLatestProjectScreenplay(projectId: number) {
  try {
    return await requestJson<ScreenplaySnapshot>(`/api/projects/${projectId}/screenplays/latest`);
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * 查询指定版本的正式剧本。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @returns 正式剧本详情
 */
export async function getProjectScreenplay(projectId: number, scriptVersionId: number) {
  try {
    return await requestJson<ScreenplaySnapshot>(`/api/projects/${projectId}/screenplays/${scriptVersionId}`);
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * 重新渲染正式剧本。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @returns 最新正式剧本快照
 */
export function renderProjectScreenplay(projectId: number, scriptVersionId: number) {
  return requestJson<ScreenplaySnapshot>(`/api/projects/${projectId}/screenplays/render`, {
    method: "POST",
    body: JSON.stringify({ scriptVersionId })
  });
}

/**
 * 将正式剧本编辑结果同步回 YAML。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 原始剧本版本 ID
 * @param title 新版本标题
 * @param markdownContent 编辑后的正式剧本文本
 * @returns 新生成的 YAML 版本
 */
export function syncProjectScreenplayToYaml(
  projectId: number,
  scriptVersionId: number,
  title: string,
  markdownContent: string
) {
  return requestJson<AdaptationScript>(`/api/projects/${projectId}/screenplays/sync-yaml`, {
    method: "POST",
    body: JSON.stringify({ scriptVersionId, title, markdownContent })
  });
}

/**
 * 保存正式剧本编辑结果。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 原始剧本版本 ID
 * @param title 新版本标题
 * @param markdownContent 编辑后的正式剧本文本
 * @returns 新生成的 YAML 版本
 */
export function saveProjectScreenplay(
  projectId: number,
  scriptVersionId: number,
  title: string,
  markdownContent: string
) {
  return requestJson<AdaptationScript>(`/api/projects/${projectId}/screenplays/save`, {
    method: "POST",
    body: JSON.stringify({ scriptVersionId, title, markdownContent })
  });
}

/**
 * 下载正式剧本文件。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @param format 导出格式
 * @returns 文件内容和文件名
 */
export async function downloadProjectScreenplay(
  projectId: number,
  scriptVersionId: number,
  format: "md" | "txt"
): Promise<DownloadFileResult> {
  const response = await fetch(`/api/projects/${projectId}/screenplays/${scriptVersionId}/export?format=${format}`);
  if (!response.ok) {
    throw new ApiRequestError(`正式剧本导出失败，状态码：${response.status}`, response.status);
  }

  const blob = await response.blob();
  return {
    blob,
    fileName: extractDownloadFileName(response.headers.get("Content-Disposition"), `screenplay.${format}`)
  };
}

/**
 * 查询最新 Story Bible。
 *
 * @param projectId 项目 ID
 * @returns 最新 Story Bible，不存在时返回 null
 */
export async function getLatestStoryBible(projectId: number) {
  try {
    return await requestJson<StoryBibleSnapshot>(`/api/projects/${projectId}/story-bible/latest`);
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) {
      return null;
    }
    throw error;
  }
}
