export interface AdaptationJobSnapshot {
  projectId: number;
  jobId: number;
  status: string;
  currentStage: string;
  currentStageLabel: string;
  currentStageIndex: number;
  stageCount: number;
  progressPercent: number;
  errorStage: string | null;
  errorMessage: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface AdaptationScript {
  projectId: number;
  scriptVersionId: number;
  versionNo: number;
  title: string;
  schemaVersion: string;
  validationStatus: string;
  yamlContent: string;
  jobId: number | null;
  jobStatus: string | null;
}

export interface StoryBibleCharacter {
  id: string;
  name: string;
  aliases: string[];
  role: string;
  traits: string[];
  goal: string;
}

export interface StoryBibleRelationship {
  from: string;
  to: string;
  type: string;
  description: string;
}

export interface StoryBibleLocation {
  id: string;
  name: string;
  description: string;
}

export interface StoryBibleTimelineEvent {
  id: string;
  order: number;
  summary: string;
  sourceRefs: string[];
}

export interface StoryBibleConflict {
  id: string;
  summary: string;
}

export interface StoryBibleForeshadowing {
  id: string;
  setup: string;
  payoffHint: string;
  sourceRefs: string[];
}

export interface StoryBibleSnapshot {
  projectId: number;
  storyBibleId: number;
  versionNo: number;
  characters: StoryBibleCharacter[];
  relationships: StoryBibleRelationship[];
  locations: StoryBibleLocation[];
  timeline: StoryBibleTimelineEvent[];
  conflicts: StoryBibleConflict[];
  foreshadowing: StoryBibleForeshadowing[];
  adaptationStrategy: string[];
}
