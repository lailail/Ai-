import { ApiRequestError, requestJson } from "./http";
import type {
  AdaptationJobSnapshot,
  AdaptationScript,
  ScreenplaySnapshot,
  ScriptValidationResult,
  ScriptVersionSummary,
  StoryBibleSnapshot
} from "../types/adaptation";
import type { CreateChapterPayload, CreateProjectPayload, Project, SourceChapter } from "../types/project";

type DownloadFileResult = {
  blob: Blob;
  fileName: string;
};

export function listProjects() {
  return requestJson<Project[]>("/api/projects");
}

export function createProject(payload: CreateProjectPayload) {
  return requestJson<Project>("/api/projects", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getProject(projectId: number) {
  return requestJson<Project>(`/api/projects/${projectId}`);
}

export function listProjectChapters(projectId: number) {
  return requestJson<SourceChapter[]>(`/api/projects/${projectId}/chapters`);
}

export function createProjectChapter(projectId: number, payload: CreateChapterPayload) {
  return requestJson<SourceChapter>(`/api/projects/${projectId}/chapters`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function generateProjectScript(projectId: number) {
  return requestJson<AdaptationJobSnapshot>(`/api/projects/${projectId}/adaptations`, {
    method: "POST"
  });
}

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
 * 查询指定项目下的剧本版本列表。
 *
 * @param projectId 项目 ID
 * @returns 剧本版本摘要列表
 */
export function listProjectScriptVersions(projectId: number) {
  return requestJson<ScriptVersionSummary[]>(`/api/projects/${projectId}/scripts`);
}

/**
 * 查询指定项目下的某个剧本版本详情。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @returns 剧本版本详情
 */
export function getProjectScriptVersion(projectId: number, scriptVersionId: number) {
  return requestJson<AdaptationScript>(`/api/projects/${projectId}/scripts/${scriptVersionId}`);
}

/**
 * 对当前 YAML 草稿执行后端 Schema 校验。
 *
 * @param projectId 项目 ID
 * @param yamlContent 待校验的 YAML 原文
 * @returns 结构化校验结果
 */
export function validateProjectScript(projectId: number, yamlContent: string) {
  return requestJson<ScriptValidationResult>(`/api/projects/${projectId}/scripts/validate`, {
    method: "POST",
    body: JSON.stringify({ yamlContent })
  });
}

/**
 * 将当前 YAML 草稿另存为新的剧本版本。
 *
 * @param projectId 项目 ID
 * @param title 新版本标题
 * @param yamlContent 编辑后的 YAML 原文
 * @returns 新创建的剧本版本详情
 */
export function saveProjectScriptVersion(projectId: number, title: string, yamlContent: string) {
  return requestJson<AdaptationScript>(`/api/projects/${projectId}/scripts`, {
    method: "POST",
    body: JSON.stringify({ title, yamlContent })
  });
}

/**
 * 查询指定项目的最新正式剧本。
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
export function getProjectScreenplay(projectId: number, scriptVersionId: number) {
  return requestJson<ScreenplaySnapshot>(`/api/projects/${projectId}/screenplays/${scriptVersionId}`);
}

/**
 * 触发指定版本的正式剧本重新渲染。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @returns 渲染后的正式剧本
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
 * @returns 新生成的 YAML 版本详情
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
 * @returns 新生成的 YAML 版本详情
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
 * 下载指定版本的正式剧本文件。
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

/**
 * 从下载响应头中提取文件名。
 *
 * @param contentDisposition 下载响应头
 * @param fallback 默认文件名
 * @returns 可用于浏览器下载的文件名
 */
function extractDownloadFileName(contentDisposition: string | null, fallback: string) {
  if (!contentDisposition) {
    return fallback;
  }

  const matches = /filename="([^"]+)"/.exec(contentDisposition);
  return matches?.[1] ?? fallback;
}
