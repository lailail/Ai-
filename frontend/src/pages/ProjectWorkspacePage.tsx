import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Form, Input, List, Space, Spin, Tabs, Tag, Typography, message } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import {
  createProjectChapter,
  generateProjectScript,
  getLatestAdaptationJob,
  getLatestProjectScript,
  getLatestStoryBible,
  getProject,
  listProjectChapters
} from "../api/projects";
import { AdaptationStatusPanel } from "../components/workspace/AdaptationStatusPanel";
import { ScriptPreviewPanel } from "../components/workspace/ScriptPreviewPanel";
import { StoryBiblePanel } from "../components/workspace/StoryBiblePanel";
import type { CreateChapterPayload } from "../types/project";
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
  yaml: "yaml"
} as const;

export function ProjectWorkspacePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { projectId } = useParams();
  const numericProjectId = Number(projectId);
  const [form] = Form.useForm<ChapterFormValues>();
  const [activeTab, setActiveTab] = useState<string>(TAB_KEYS.chapters);
  const [watchingJobId, setWatchingJobId] = useState<number | null>(null);

  useEffect(() => {
    if (Number.isInteger(numericProjectId) && numericProjectId > 0) {
      saveRecentProjectId(numericProjectId);
    }
  }, [numericProjectId]);

  const projectQuery = useQuery({
    queryKey: ["project", numericProjectId],
    queryFn: () => getProject(numericProjectId),
    enabled: Number.isInteger(numericProjectId) && numericProjectId > 0
  });

  const chaptersQuery = useQuery({
    queryKey: ["project-chapters", numericProjectId],
    queryFn: () => listProjectChapters(numericProjectId),
    enabled: Number.isInteger(numericProjectId) && numericProjectId > 0
  });

  const latestJobQuery = useQuery({
    queryKey: ["project-latest-job", numericProjectId],
    queryFn: () => getLatestAdaptationJob(numericProjectId),
    enabled: Number.isInteger(numericProjectId) && numericProjectId > 0,
    refetchInterval: (query) => (query.state.data?.status === "RUNNING" ? 1500 : false)
  });

  const latestScriptQuery = useQuery({
    queryKey: ["project-latest-script", numericProjectId],
    queryFn: () => getLatestProjectScript(numericProjectId),
    enabled: Number.isInteger(numericProjectId) && numericProjectId > 0
  });

  const latestStoryBibleQuery = useQuery({
    queryKey: ["project-latest-story-bible", numericProjectId],
    queryFn: () => getLatestStoryBible(numericProjectId),
    enabled: Number.isInteger(numericProjectId) && numericProjectId > 0
  });

  useEffect(() => {
    if (chaptersQuery.data) {
      form.setFieldsValue({
        chapterNo: getNextChapterNo(chaptersQuery.data)
      });
    }
  }, [chaptersQuery.data, form]);

  useEffect(() => {
    if (!watchingJobId || !latestJobQuery.data || latestJobQuery.data.jobId !== watchingJobId) {
      return;
    }

    if (latestJobQuery.data.status === "SUCCEEDED") {
      void Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-story-bible", numericProjectId] })
      ]);
      message.success("改编完成，最新 Story Bible 和 YAML 初稿已刷新。");
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

  const generateScriptMutation = useMutation({
    mutationFn: () => generateProjectScript(numericProjectId),
    onSuccess: async (job) => {
      queryClient.setQueryData(["project-latest-job", numericProjectId], job);
      setActiveTab(TAB_KEYS.progress);

      if (job.status === "SUCCEEDED") {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] }),
          queryClient.invalidateQueries({ queryKey: ["project-latest-story-bible", numericProjectId] })
        ]);
        message.success("改编完成，已生成最新剧本初稿。");
        return;
      }

      setWatchingJobId(job.jobId);
      message.success("改编任务已启动，正在后台生成。");
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const chapterTabContent = useMemo(() => {
    const chapters = chaptersQuery.data ?? [];

    return (
      <div className="workspace-tab-stack">
        <section className="panel panel-soft">
          <div className="panel-heading">
            <Typography.Text className="eyebrow">章节录入</Typography.Text>
            <Typography.Title level={3}>持续补充原始小说章节</Typography.Title>
          </div>
          <Form
            layout="vertical"
            form={form}
            onFinish={(values) =>
              createChapterMutation.mutate({
                chapterNo: Number(values.chapterNo),
                title: values.title,
                content: values.content,
                wordCount: values.content.length
              })
            }
            initialValues={{ chapterNo: getNextChapterNo(chapters) }}
          >
            <Form.Item label="章节号" name="chapterNo" rules={[{ required: true, message: "请填写章节号" }]}>
              <Input type="number" size="large" />
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
            <Button type="primary" htmlType="submit" size="large" loading={createChapterMutation.isPending}>
              保存当前章节
            </Button>
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
              <List.Item className="chapter-list-item">
                <List.Item.Meta
                  title={`第 ${chapter.chapterNo} 章${chapter.title ? ` · ${chapter.title}` : ""}`}
                  description={`字数：${chapter.wordCount}`}
                />
              </List.Item>
            )}
          />
        </section>
      </div>
    );
  }, [chaptersQuery.data, createChapterMutation, form]);

  if (!Number.isInteger(numericProjectId) || numericProjectId <= 0) {
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
          <Typography.Paragraph>
            {projectQuery.error instanceof Error ? projectQuery.error.message : "请稍后重试。"}
          </Typography.Paragraph>
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
          <Typography.Paragraph>{project.description || "你还没有填写项目简介，可以先从章节录入开始。"}</Typography.Paragraph>
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

      <section className="workspace-tabs-shell">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: TAB_KEYS.chapters,
              label: "章节管理",
              children: chapterTabContent
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
                  onOpenStoryBible={() => setActiveTab(TAB_KEYS.storyBible)}
                  onOpenYamlPreview={() => setActiveTab(TAB_KEYS.yaml)}
                  onStart={() => generateScriptMutation.mutate()}
                />
              )
            },
            {
              key: TAB_KEYS.storyBible,
              label: "Story Bible",
              children: (
                <StoryBiblePanel
                  storyBible={latestStoryBibleQuery.data ?? null}
                  isLoading={latestStoryBibleQuery.isLoading}
                />
              )
            },
            {
              key: TAB_KEYS.yaml,
              label: "YAML 初稿",
              children: (
                <ScriptPreviewPanel
                  latestScript={latestScriptQuery.data ?? null}
                  isLoading={latestScriptQuery.isLoading}
                />
              )
            }
          ]}
        />
      </section>
    </main>
  );
}
