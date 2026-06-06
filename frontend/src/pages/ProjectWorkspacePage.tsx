import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Divider, Form, Input, List, Space, Spin, Tag, Typography, message } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { createProjectChapter, getProject, listProjectChapters } from "../api/projects";
import { StartAdaptationCard } from "../components/project/StartAdaptationCard";
import { saveRecentProjectId } from "../utils/recent-projects";
import { getNextChapterNo } from "../utils/chapter";
import type { CreateChapterPayload } from "../types/project";

type ChapterFormValues = {
  chapterNo: number;
  title?: string;
  content: string;
};

export function ProjectWorkspacePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { projectId } = useParams();
  const numericProjectId = Number(projectId);
  const [form] = Form.useForm<ChapterFormValues>();

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

  useEffect(() => {
    if (chaptersQuery.data) {
      form.setFieldsValue({
        chapterNo: getNextChapterNo(chaptersQuery.data)
      });
    }
  }, [chaptersQuery.data, form]);

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
          <Typography.Paragraph>{projectQuery.error instanceof Error ? projectQuery.error.message : "请稍后重试。"}</Typography.Paragraph>
          <Button onClick={() => navigate("/")}>返回首页</Button>
        </div>
      </main>
    );
  }

  const project = projectQuery.data;
  const chapters = chaptersQuery.data ?? [];

  const handleSubmit = (values: ChapterFormValues) => {
    createChapterMutation.mutate({
      chapterNo: Number(values.chapterNo),
      title: values.title,
      content: values.content,
      wordCount: values.content.length
    });
  };

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

      <section className="workspace-grid">
        <div className="panel panel-soft">
          <div className="panel-heading">
            <Typography.Text className="eyebrow">章节录入</Typography.Text>
            <Typography.Title level={3}>持续补充原始小说章节</Typography.Title>
          </div>
          <Form layout="vertical" form={form} onFinish={handleSubmit} initialValues={{ chapterNo: getNextChapterNo(chapters) }}>
            <Form.Item label="章节号" name="chapterNo" rules={[{ required: true, message: "请填写章节号" }]}>
              <Input type="number" size="large" />
            </Form.Item>
            <Form.Item label="章节标题" name="title" rules={[{ max: 255, message: "章节标题不能超过 255 个字符" }]}>
              <Input placeholder="例如：风吹过旧码头" size="large" />
            </Form.Item>
            <Form.Item label="章节正文" name="content" rules={[{ required: true, message: "请填写章节正文" }]}>
              <Input.TextArea rows={12} placeholder="粘贴这一章的正文内容。" />
            </Form.Item>
            <Button type="primary" htmlType="submit" size="large" loading={createChapterMutation.isPending}>
              保存当前章节
            </Button>
          </Form>
        </div>

        <div className="workspace-side">
          <StartAdaptationCard chapterCount={project.chapterCount} onStart={() => message.info("下一次提交会接入真正的改编工作台。")} />

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
                    title={`第 ${chapter.chapterNo} 章 ${chapter.title ? `· ${chapter.title}` : ""}`}
                    description={`字数：${chapter.wordCount}`}
                  />
                </List.Item>
              )}
            />
            <Divider />
            <Typography.Paragraph className="panel-copy">
              当前 commit 先把“项目创建 + 章节录入 + 3 章校验”做扎实。下一次提交会在这里接入 Story Bible 和改编进度区。
            </Typography.Paragraph>
          </section>
        </div>
      </section>
    </main>
  );
}
