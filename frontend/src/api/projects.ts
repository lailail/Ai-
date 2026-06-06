import { ApiRequestError, requestJson } from "./http";
import type { AdaptationJobSnapshot, AdaptationScript, StoryBibleSnapshot } from "../types/adaptation";
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
