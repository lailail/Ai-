const RECENT_PROJECT_IDS_KEY = "novel-script-recent-project-ids";
const MAX_RECENT_PROJECTS = 6;

export function getRecentProjectIds() {
  if (typeof window === "undefined") {
    return [] as number[];
  }

  const rawValue = window.localStorage.getItem(RECENT_PROJECT_IDS_KEY);
  if (!rawValue) {
    return [] as number[];
  }

  try {
    const parsed = JSON.parse(rawValue) as number[];
    return Array.isArray(parsed) ? parsed.filter((value) => Number.isInteger(value)) : [];
  } catch {
    return [] as number[];
  }
}

export function saveRecentProjectId(projectId: number) {
  if (typeof window === "undefined") {
    return;
  }

  const currentIds = getRecentProjectIds().filter((id) => id !== projectId);
  const nextIds = [projectId, ...currentIds].slice(0, MAX_RECENT_PROJECTS);
  window.localStorage.setItem(RECENT_PROJECT_IDS_KEY, JSON.stringify(nextIds));
}
