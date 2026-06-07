import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Form, Input, List, Space, Spin, Tabs, Tag, Typography, message } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import {
  createProjectChapter,
  downloadProjectScreenplay,
  generateProjectScript,
  getLatestAdaptationJob,
  getLatestProjectScript,
  getLatestStoryBible,
  getProject,
  getProjectScreenplay,
  getProjectScriptVersion,
  listProjectChapters,
  listProjectScriptVersions,
  renderProjectScreenplay,
  saveProjectScreenplay,
  saveProjectScriptVersion,
  updateProjectChapter,
  validateProjectScript
} from "../api/projects";
import { ScreenplayActionPanel } from "../components/screenplay/ScreenplayActionPanel";
import { ScreenplayEditorPanel } from "../components/screenplay/ScreenplayEditorPanel";
import { ScreenplayVersionList } from "../components/screenplay/ScreenplayVersionList";
import { AdaptationStatusPanel } from "../components/workspace/AdaptationStatusPanel";
import { ScriptPreviewPanel } from "../components/workspace/ScriptPreviewPanel";
import { StoryBiblePanel } from "../components/workspace/StoryBiblePanel";
import type { AdaptationScript, ScriptValidationResult } from "../types/adaptation";
import type { CreateChapterPayload, SourceChapter, UpdateChapterPayload } from "../types/project";
import { getNextChapterNo } from "../utils/chapter";
import { saveRecentProjectId } from "../utils/recent-projects";

type ChapterFormValues = {
  chapterNo: number;
  title?: string;
  content: string;
};

const TAB_KEYS = {
  chapters: "chapters",
  progress: "progress",
  storyBible: "story-bible",
  yaml: "yaml",
  screenplay: "screenplay"
} as const;

/**
 * 承载单个小说改编项目主工作台页面。
 */
export function ProjectWorkspacePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { projectId } = useParams();
  const numericProjectId = Number(projectId);
  const isValidProjectId = Number.isInteger(numericProjectId) && numericProjectId > 0;
  const [form] = Form.useForm<ChapterFormValues>();
  const [activeTab, setActiveTab] = useState<string>(TAB_KEYS.chapters);
  const [watchingJobId, setWatchingJobId] = useState<number | null>(null);
  const [editingChapterId, setEditingChapterId] = useState<number | null>(null);
  const [selectedScriptVersionId, setSelectedScriptVersionId] = useState<number | null>(null);
  const [draftSourceVersionId, setDraftSourceVersionId] = useState<number | null>(null);
  const [draftTitle, setDraftTitle] = useState("");
  const [draftYamlContent, setDraftYamlContent] = useState("");
  const [validationResult, setValidationResult] = useState<ScriptValidationResult | null>(null);
  const [screenplayDraftSourceVersionId, setScreenplayDraftSourceVersionId] = useState<number | null>(null);
  const [screenplayDraftTitle, setScreenplayDraftTitle] = useState("");
  const [screenplayDraftMarkdownContent, setScreenplayDraftMarkdownContent] = useState("");

  useEffect(() => {
    if (isValidProjectId) {
      saveRecentProjectId(numericProjectId);
    }
  }, [isValidProjectId, numericProjectId]);

  const projectQuery = useQuery({
    queryKey: ["project", numericProjectId],
    queryFn: () => getProject(numericProjectId),
    enabled: isValidProjectId
  });

  const chaptersQuery = useQuery({
    queryKey: ["project-chapters", numericProjectId],
    queryFn: () => listProjectChapters(numericProjectId),
    enabled: isValidProjectId
  });

  const latestJobQuery = useQuery({
    queryKey: ["project-latest-job", numericProjectId],
    queryFn: () => getLatestAdaptationJob(numericProjectId),
    enabled: isValidProjectId,
    refetchInterval: (query) => (query.state.data?.status === "RUNNING" ? 1500 : false)
  });

  const latestScriptQuery = useQuery({
    queryKey: ["project-latest-script", numericProjectId],
    queryFn: () => getLatestProjectScript(numericProjectId),
    enabled: isValidProjectId
  });

  const latestStoryBibleQuery = useQuery({
    queryKey: ["project-latest-story-bible", numericProjectId],
    queryFn: () => getLatestStoryBible(numericProjectId),
    enabled: isValidProjectId
  });

  const scriptVersionsQuery = useQuery({
    queryKey: ["project-script-versions", numericProjectId],
    queryFn: () => listProjectScriptVersions(numericProjectId),
    enabled: isValidProjectId
  });

  const selectedScriptQuery = useQuery({
    queryKey: ["project-script-version", numericProjectId, selectedScriptVersionId],
    queryFn: () => getProjectScriptVersion(numericProjectId, selectedScriptVersionId as number),
    enabled: isValidProjectId && selectedScriptVersionId !== null
  });

  const selectedScreenplayQuery = useQuery({
    queryKey: ["project-screenplay", numericProjectId, selectedScriptVersionId],
    queryFn: () => getProjectScreenplay(numericProjectId, selectedScriptVersionId as number),
    enabled: isValidProjectId && selectedScriptVersionId !== null
  });

  useEffect(() => {
    if (chaptersQuery.data && editingChapterId === null) {
      form.setFieldsValue({
        chapterNo: getNextChapterNo(chaptersQuery.data)
      });
    }
  }, [chaptersQuery.data, editingChapterId, form]);

  useEffect(() => {
    const versionSummaries = scriptVersionsQuery.data ?? [];
    if (versionSummaries.length === 0) {
      setSelectedScriptVersionId(null);
      setDraftSourceVersionId(null);
      setScreenplayDraftSourceVersionId(null);
      return;
    }

    const stillExists = versionSummaries.some((item) => item.scriptVersionId === selectedScriptVersionId);
    if (!stillExists) {
      setSelectedScriptVersionId(versionSummaries[0].scriptVersionId);
      setDraftSourceVersionId(null);
      setScreenplayDraftSourceVersionId(null);
    }
  }, [scriptVersionsQuery.data, selectedScriptVersionId]);

  useEffect(() => {
    const selectedScript = selectedScriptQuery.data;
    if (!selectedScript || draftSourceVersionId === selectedScript.scriptVersionId) {
      return;
    }

    setDraftSourceVersionId(selectedScript.scriptVersionId);
    setDraftTitle(selectedScript.title);
    setDraftYamlContent(selectedScript.yamlContent);
    setValidationResult(buildValidationResultFromScript(selectedScript));
  }, [draftSourceVersionId, selectedScriptQuery.data]);

  useEffect(() => {
    const selectedScreenplay = selectedScreenplayQuery.data;
    if (!selectedScreenplay || screenplayDraftSourceVersionId === selectedScreenplay.scriptVersionId) {
      return;
    }

    setScreenplayDraftSourceVersionId(selectedScreenplay.scriptVersionId);
    setScreenplayDraftTitle(selectedScreenplay.title);
    setScreenplayDraftMarkdownContent(selectedScreenplay.markdownContent);
  }, [screenplayDraftSourceVersionId, selectedScreenplayQuery.data]);

  useEffect(() => {
    if (!watchingJobId || !latestJobQuery.data || latestJobQuery.data.jobId !== watchingJobId) {
      return;
    }

    if (latestJobQuery.data.status === "SUCCEEDED") {
      void Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-story-bible", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] })
      ]);
      message.success("改编完成，最新 Story Bible 和剧本版本已刷新。");
      setWatchingJobId(null);
    }

    if (latestJobQuery.data.status === "FAILED") {
      message.error(latestJobQuery.data.errorMessage ?? "改编任务执行失败，请稍后重试。");
      setWatchingJobId(null);
    }
  }, [latestJobQuery.data, numericProjectId, queryClient, watchingJobId]);

  const createChapterMutation = useMutation({
    mutationFn: (payload: CreateChapterPayload) => createProjectChapter(numericProjectId, payload),
    onSuccess: async (chapter) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-chapters", numericProjectId] })
      ]);
      form.resetFields(["title", "content"]);
      form.setFieldsValue({ chapterNo: chapter.chapterNo + 1 });
      message.success(`第 ${chapter.chapterNo} 章已保存。`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const updateChapterMutation = useMutation({
    mutationFn: ({ chapterId, payload }: { chapterId: number; payload: UpdateChapterPayload }) =>
      updateProjectChapter(numericProjectId, chapterId, payload),
    onSuccess: async (chapter) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-chapters", numericProjectId] })
      ]);
      handleStopEditing(chapter.chapterNo + 1);
      message.success(`第 ${chapter.chapterNo} 章已更新。`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const generateScriptMutation = useMutation({
    mutationFn: () => generateProjectScript(numericProjectId),
    onSuccess: async (job) => {
      queryClient.setQueryData(["project-latest-job", numericProjectId], job);
      setActiveTab(TAB_KEYS.progress);

      if (job.status === "SUCCEEDED") {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] }),
          queryClient.invalidateQueries({ queryKey: ["project-latest-story-bible", numericProjectId] }),
          queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] })
        ]);
        message.success("改编完成，已生成最新剧本初稿。");
        return;
      }

      setWatchingJobId(job.jobId);
      message.success("改编任务已启动，正在后台分阶段生成。");
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const validateScriptMutation = useMutation({
    mutationFn: () => validateProjectScript(numericProjectId, draftYamlContent),
    onSuccess: (result) => {
      setValidationResult(result);
      if (result.valid) {
        message.success("YAML 校验通过。");
        return;
      }
      message.warning(`YAML 校验未通过，共发现 ${result.errors.length} 个问题。`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const saveScriptVersionMutation = useMutation({
    mutationFn: () => saveProjectScriptVersion(numericProjectId, draftTitle, draftYamlContent),
    onSuccess: async (script) => {
      setSelectedScriptVersionId(script.scriptVersionId);
      setDraftSourceVersionId(null);
      setValidationResult(buildValidationResultFromScript(script));
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-script-version", numericProjectId, script.scriptVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] })
      ]);

      if (script.validationStatus === "PASSED") {
        message.success(`新版本已保存：第 ${script.versionNo} 版。`);
        return;
      }
      message.warning(`新版本已保存为第 ${script.versionNo} 版，但当前 YAML 仍未通过校验。`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const renderScreenplayMutation = useMutation({
    mutationFn: () => renderProjectScreenplay(numericProjectId, selectedScriptVersionId as number),
    onSuccess: async (screenplay) => {
      setSelectedScriptVersionId(screenplay.scriptVersionId);
      setScreenplayDraftSourceVersionId(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-screenplay", numericProjectId, screenplay.scriptVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] })
      ]);
      message.success("正式剧本已根据当前 YAML 重新渲染。");
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const saveScreenplayMutation = useMutation({
    mutationFn: () =>
      saveProjectScreenplay(
        numericProjectId,
        selectedScriptVersionId as number,
        screenplayDraftTitle,
        screenplayDraftMarkdownContent
      ),
    onSuccess: async (script) => {
      const nextVersionId = script.scriptVersionId;
      setSelectedScriptVersionId(nextVersionId);
      setDraftSourceVersionId(null);
      setScreenplayDraftSourceVersionId(null);
      setValidationResult(buildValidationResultFromScript(script));
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-script-version", numericProjectId, nextVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-screenplay", numericProjectId, nextVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] })
      ]);
      message.success(`正式剧本已保存，并生成第 ${script.versionNo} 版 YAML。`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const selectedScript = selectedScriptQuery.data ?? null;
  const selectedScreenplay = selectedScreenplayQuery.data ?? null;
  const chapters = chaptersQuery.data ?? [];
  const hasUnsavedChanges = Boolean(
    selectedScript && (draftTitle !== selectedScript.title || draftYamlContent !== selectedScript.yamlContent)
  );
  const hasUnsavedScreenplayChanges = Boolean(
    selectedScreenplay &&
      (screenplayDraftTitle !== selectedScreenplay.title ||
        screenplayDraftMarkdownContent !== selectedScreenplay.markdownContent)
  );
  const isEditingChapter = editingChapterId !== null;

  /**
   * 停止章节编辑模式，并恢复到下一章节录入状态。
   *
   * @param nextChapterNo 下一次默认填充的章节号
   */
  function handleStopEditing(nextChapterNo?: number) {
    setEditingChapterId(null);
    form.resetFields(["title", "content"]);
    form.setFieldsValue({
      chapterNo: nextChapterNo ?? getNextChapterNo(chapters)
    });
  }

  /**
   * 打开指定章节的查看与编辑表单。
   *
   * @param chapter 目标章节
   */
  function openChapterForEditing(chapter: SourceChapter) {
    setEditingChapterId(chapter.id);
    form.setFieldsValue({
      chapterNo: chapter.chapterNo,
      title: chapter.title ?? undefined,
      content: chapter.content
    });
  }

  /**
   * 提交章节表单，按当前模式选择创建或更新。
   *
   * @param values 表单值
   */
  function handleSubmitChapter(values: ChapterFormValues) {
    if (editingChapterId !== null) {
      updateChapterMutation.mutate({
        chapterId: editingChapterId,
        payload: {
          title: values.title,
          content: values.content,
          wordCount: values.content.length
        }
      });
      return;
    }

    createChapterMutation.mutate({
      chapterNo: Number(values.chapterNo),
      title: values.title,
      content: values.content,
      wordCount: values.content.length
    });
  }

  /**
   * 切换剧本版本时同步清空当前草稿来源标记。
   *
   * @param scriptVersionId 剧本版本 ID
   */
  function handleSelectScriptVersion(scriptVersionId: number) {
    setSelectedScriptVersionId(scriptVersionId);
    setDraftSourceVersionId(null);
    setScreenplayDraftSourceVersionId(null);
  }

  if (!isValidProjectId) {
    return (
      <main className="page-shell">
        <div className="panel">
          <Typography.Title level={3}>项目地址无效</Typography.Title>
          <Button onClick={() => navigate("/")}>返回首页</Button>
        </div>
      </main>
    );
  }

  if (projectQuery.isLoading || chaptersQuery.isLoading) {
    return (
      <main className="page-shell">
        <div className="empty-state">
          <Spin size="large" />
        </div>
      </main>
    );
  }

  if (projectQuery.isError || !projectQuery.data) {
    return (
      <main className="page-shell">
        <div className="panel">
          <Typography.Title level={3}>项目加载失败</Typography.Title>
          <Typography.Paragraph>{projectQuery.error instanceof Error ? projectQuery.error.message : "请稍后重试。"}</Typography.Paragraph>
          <Button onClick={() => navigate("/")}>返回首页</Button>
        </div>
      </main>
    );
  }

  const project = projectQuery.data;

  return (
    <main className="page-shell">
      <section className="workspace-header">
        <div>
          <Typography.Text className="eyebrow">项目工作台</Typography.Text>
          <Typography.Title>{project.title}</Typography.Title>
          <Typography.Paragraph>
            {project.description || "你还没有填写项目简介，可以先从章节录入开始。"}
          </Typography.Paragraph>
        </div>
        <Space size="middle" wrap>
          <Tag color="blue">{project.status}</Tag>
          <Tag color={project.chapterCount >= 3 ? "success" : "gold"}>已录入 {project.chapterCount} 章</Tag>
          <Button onClick={() => navigate("/")}>返回项目首页</Button>
        </Space>
      </section>

      {latestJobQuery.isError ? (
        <Alert
          className="workspace-alert"
          type="error"
          showIcon
          message="改编任务查询失败"
          description={latestJobQuery.error instanceof Error ? latestJobQuery.error.message : "请稍后重试。"}
        />
      ) : null}

      {latestScriptQuery.isError ? (
        <Alert
          className="workspace-alert"
          type="error"
          showIcon
          message="最新剧本查询失败"
          description={latestScriptQuery.error instanceof Error ? latestScriptQuery.error.message : "请稍后重试。"}
        />
      ) : null}

      {latestStoryBibleQuery.isError ? (
        <Alert
          className="workspace-alert"
          type="error"
          showIcon
          message="Story Bible 查询失败"
          description={latestStoryBibleQuery.error instanceof Error ? latestStoryBibleQuery.error.message : "请稍后重试。"}
        />
      ) : null}

      {scriptVersionsQuery.isError ? (
        <Alert
          className="workspace-alert"
          type="error"
          showIcon
          message="剧本版本列表查询失败"
          description={scriptVersionsQuery.error instanceof Error ? scriptVersionsQuery.error.message : "请稍后重试。"}
        />
      ) : null}

      {selectedScreenplayQuery.isError ? (
        <Alert
          className="workspace-alert"
          type="error"
          showIcon
          message="正式剧本查询失败"
          description={selectedScreenplayQuery.error instanceof Error ? selectedScreenplayQuery.error.message : "请稍后重试。"}
        />
      ) : null}

      <section className="workspace-tabs-shell">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: TAB_KEYS.chapters,
              label: "章节管理",
              children: (
                <div className="workspace-tab-stack">
                  <section className="panel panel-soft">
                    <div className="panel-heading">
                      <Typography.Text className="eyebrow">{isEditingChapter ? "章节查看 / 编辑" : "章节录入"}</Typography.Text>
                      <Typography.Title level={3}>
                        {isEditingChapter ? "查看并修改当前章节内容" : "持续补充原始小说章节"}
                      </Typography.Title>
                    </div>
                    <Form
                      layout="vertical"
                      form={form}
                      onFinish={handleSubmitChapter}
                      initialValues={{ chapterNo: getNextChapterNo(chapters) }}
                    >
                      <Form.Item label="章节号" name="chapterNo" rules={[{ required: true, message: "请填写章节号" }]}>
                        <Input type="number" size="large" disabled={isEditingChapter} />
                      </Form.Item>
                      <Form.Item
                        label="章节标题"
                        name="title"
                        rules={[{ max: 255, message: "章节标题不能超过 255 个字符" }]}
                      >
                        <Input placeholder="例如：风吹过旧码头" size="large" />
                      </Form.Item>
                      <Form.Item label="章节正文" name="content" rules={[{ required: true, message: "请填写章节正文" }]}>
                        <Input.TextArea rows={12} placeholder="粘贴这一章的正文内容。" />
                      </Form.Item>
                      <Space wrap>
                        <Button
                          type="primary"
                          htmlType="submit"
                          size="large"
                          loading={createChapterMutation.isPending || updateChapterMutation.isPending}
                        >
                          {isEditingChapter ? "保存章节修改" : "保存当前章节"}
                        </Button>
                        {isEditingChapter ? (
                          <Button size="large" onClick={() => handleStopEditing()}>
                            取消查看 / 编辑
                          </Button>
                        ) : null}
                      </Space>
                    </Form>
                  </section>

                  <section className="panel">
                    <div className="panel-heading">
                      <Typography.Text className="eyebrow">章节概览</Typography.Text>
                      <Typography.Title level={4}>当前项目已录入章节</Typography.Title>
                    </div>
                    <List
                      dataSource={chapters}
                      locale={{ emptyText: "还没有章节，先录入第一章吧。" }}
                      renderItem={(chapter) => (
                        <List.Item
                          className="chapter-list-item"
                          actions={[
                            <Button key={`view-${chapter.id}`} type="link" onClick={() => openChapterForEditing(chapter)}>
                              查看 / 编辑
                            </Button>
                          ]}
                        >
                          <List.Item.Meta
                            title={`第 ${chapter.chapterNo} 章${chapter.title ? ` · ${chapter.title}` : ""}`}
                            description={`字数：${chapter.wordCount}`}
                          />
                        </List.Item>
                      )}
                    />
                  </section>
                </div>
              )
            },
            {
              key: TAB_KEYS.progress,
              label: "改编进度",
              children: (
                <AdaptationStatusPanel
                  chapterCount={project.chapterCount}
                  isStarting={generateScriptMutation.isPending}
                  latestJob={latestJobQuery.data ?? null}
                  latestScript={latestScriptQuery.data ?? null}
                  onStart={() => generateScriptMutation.mutate()}
                />
              )
            },
            {
              key: TAB_KEYS.storyBible,
              label: "Story Bible",
              children: (
                <StoryBiblePanel storyBible={latestStoryBibleQuery.data ?? null} isLoading={latestStoryBibleQuery.isLoading} />
              )
            },
            {
              key: TAB_KEYS.yaml,
              label: "YAML 初稿",
              children: (
                <ScriptPreviewPanel
                  versionSummaries={scriptVersionsQuery.data ?? []}
                  selectedScript={selectedScript}
                  draftTitle={draftTitle}
                  draftYamlContent={draftYamlContent}
                  validationResult={validationResult}
                  hasUnsavedChanges={hasUnsavedChanges}
                  isListLoading={scriptVersionsQuery.isLoading}
                  isDetailLoading={selectedScriptQuery.isLoading}
                  isValidating={validateScriptMutation.isPending}
                  isSaving={saveScriptVersionMutation.isPending}
                  onSelectVersion={handleSelectScriptVersion}
                  onDraftTitleChange={setDraftTitle}
                  onDraftYamlChange={(value) => {
                    setDraftYamlContent(value);
                    setValidationResult(null);
                  }}
                  onValidate={() => validateScriptMutation.mutate()}
                  onSave={() => saveScriptVersionMutation.mutate()}
                  onExportYaml={() => {
                    downloadTextFile(`${sanitizeFileName(draftTitle || "script")}.yaml`, draftYamlContent, "text/yaml");
                    message.success("YAML 文件已开始下载。");
                  }}
                />
              )
            },
            {
              key: TAB_KEYS.screenplay,
              label: "正式剧本",
              children: (
                <section className="panel panel-soft">
                  <div className="panel-heading">
                    <Typography.Text className="eyebrow">正式剧本工作区</Typography.Text>
                    <Typography.Title level={4}>面向作者阅读与润色的剧本视图</Typography.Title>
                  </div>
                  <Typography.Paragraph className="panel-copy">
                    这里展示的是由 YAML 结构稿渲染得到的正式剧本视图。你可以切换版本、重新渲染、直接编辑并保存回 YAML，
                    同时导出 Markdown 或 TXT。
                  </Typography.Paragraph>

                  <div className="screenplay-workspace">
                    <ScreenplayVersionList
                      versionSummaries={scriptVersionsQuery.data ?? []}
                      selectedScriptVersionId={selectedScriptVersionId}
                      isLoading={scriptVersionsQuery.isLoading}
                      onSelectVersion={handleSelectScriptVersion}
                    />

                    <ScreenplayEditorPanel
                      selectedScreenplay={selectedScreenplay}
                      draftTitle={screenplayDraftTitle}
                      draftMarkdownContent={screenplayDraftMarkdownContent}
                      hasUnsavedChanges={hasUnsavedScreenplayChanges}
                      isLoading={selectedScreenplayQuery.isLoading}
                      onDraftTitleChange={setScreenplayDraftTitle}
                      onDraftMarkdownChange={setScreenplayDraftMarkdownContent}
                    />

                    <ScreenplayActionPanel
                      selectedScreenplay={selectedScreenplay}
                      canRender={selectedScriptVersionId !== null}
                      hasUnsavedChanges={hasUnsavedScreenplayChanges}
                      isRendering={renderScreenplayMutation.isPending}
                      isSaving={saveScreenplayMutation.isPending}
                      onRender={() => renderScreenplayMutation.mutate()}
                      onSave={() => saveScreenplayMutation.mutate()}
                      onExportMarkdown={() => {
                        void downloadScreenplay(numericProjectId, selectedScriptVersionId, "md");
                      }}
                      onExportText={() => {
                        void downloadScreenplay(numericProjectId, selectedScriptVersionId, "txt");
                      }}
                    />
                  </div>
                </section>
              )
            }
          ]}
        />
      </section>
    </main>
  );
}

/**
 * 根据当前剧本版本详情生成界面需要的校验结果快照。
 *
 * @param script 剧本版本详情
 * @returns 可直接展示的校验结果；若无明确信息则返回空
 */
function buildValidationResultFromScript(script: AdaptationScript): ScriptValidationResult | null {
  if (script.validationStatus === "PASSED") {
    return {
      projectId: script.projectId,
      schemaVersion: script.schemaVersion,
      valid: true,
      errors: []
    };
  }

  if (script.validationErrors.length > 0) {
    return {
      projectId: script.projectId,
      schemaVersion: script.schemaVersion,
      valid: false,
      errors: script.validationErrors
    };
  }

  return null;
}

/**
 * 触发浏览器下载纯文本文件。
 *
 * @param fileName 下载文件名
 * @param content 文件内容
 * @param mimeType MIME 类型
 */
function downloadTextFile(fileName: string, content: string, mimeType: string) {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

/**
 * 下载指定版本的正式剧本文件。
 *
 * @param projectId 项目 ID
 * @param scriptVersionId 剧本版本 ID
 * @param format 导出格式
 */
async function downloadScreenplay(projectId: number, scriptVersionId: number | null, format: "md" | "txt") {
  if (!scriptVersionId) {
    message.warning("请先选择要导出的正式剧本版本。");
    return;
  }

  const file = await downloadProjectScreenplay(projectId, scriptVersionId, format);
  const url = URL.createObjectURL(file.blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = file.fileName;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
  message.success(`正式剧本 ${format.toUpperCase()} 文件已开始下载。`);
}

/**
 * 将标题规范化为适合下载的文件名。
 *
 * @param value 原始标题
 * @returns 安全文件名
 */
function sanitizeFileName(value: string) {
  const nextValue = value.trim().replace(/[\\/:*?"<>|]/g, "_");
  return nextValue || "script";
}
