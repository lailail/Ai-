export const ADAPTATION_STAGE_ITEMS = [
  { key: "CHAPTER_NORMALIZE", label: "章节标准化" },
  { key: "CHAPTER_CONTEXT_EXTRACT", label: "单章上下文提取" },
  { key: "GLOBAL_CONTEXT_MERGE", label: "全局上下文合并" },
  { key: "STORY_BIBLE_BUILD", label: "Story Bible 构建" },
  { key: "SCRIPT_OUTLINE_PLAN", label: "剧本大纲规划" },
  { key: "SCENE_GENERATE", label: "场景生成" },
  { key: "YAML_SERIALIZE", label: "YAML 序列化" },
  { key: "SCHEMA_VALIDATE", label: "Schema 校验" },
  { key: "VERSION_SAVE", label: "版本保存" }
] as const;
