import { ApiRequestError, requestJson } from "./http";
import type {
  AdaptationJobSnapshot,
  AdaptationScript,
  ScriptValidationResult,
  ScriptVersionSummary,
  StoryBibleSnapshot
} from "../types/adaptation";
import type { CreateChapterPayload, CreateProjectPayload, Project, SourceChapter } from "../types/project";

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
