import { requestJson } from "./http";
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
