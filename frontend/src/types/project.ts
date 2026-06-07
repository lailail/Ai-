export interface Project {
  id: number;
  title: string;
  description: string | null;
  status: string;
  chapterCount: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface SourceChapter {
  id: number;
  projectId: number;
  chapterNo: number;
  title: string | null;
  content: string;
  wordCount: number;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface CreateProjectPayload {
  title: string;
  description?: string;
}

export interface CreateChapterPayload {
  chapterNo: number;
  title?: string;
  content: string;
  wordCount?: number;
}

export interface UpdateChapterPayload {
  title?: string;
  content: string;
  wordCount?: number;
}
