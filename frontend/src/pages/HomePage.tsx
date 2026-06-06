import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRightOutlined } from "@ant-design/icons";
import { Button, Form, Input, List, Spin, Tag, Typography, message } from "antd";
import { useNavigate } from "react-router-dom";
import { createProject, listProjects } from "../api/projects";
import type { CreateProjectPayload, Project } from "../types/project";
import { getRecentProjectIds, saveRecentProjectId } from "../utils/recent-projects";

type ProjectFormValues = {
  title: string;
  description?: string;
};

export function HomePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form] = Form.useForm<ProjectFormValues>();
  const recentProjectIds = getRecentProjectIds();

  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: listProjects
  });

  const createProjectMutation = useMutation({
    mutationFn: (payload: CreateProjectPayload) => createProject(payload),
    onSuccess: async (project) => {
      saveRecentProjectId(project.id);
      await queryClient.invalidateQueries({ queryKey: ["projects"] });
      message.success("项目创建成功，已进入章节录入页。");
      navigate(`/projects/${project.id}`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const orderedProjects = useMemo(() => {
    const projects = projectsQuery.data ?? [];
    const recentSet = new Set(recentProjectIds);
    return [...projects].sort((left, right) => {
      const leftRecent = recentSet.has(left.id) ? 1 : 0;
      const rightRecent = recentSet.has(right.id) ? 1 : 0;
      if (leftRecent !== rightRecent) {
        return rightRecent - leftRecent;
      }
      return right.id - left.id;
    });
  }, [projectsQuery.data, recentProjectIds]);

  const handleSubmit = (values: ProjectFormValues) => {
    createProjectMutation.mutate(values);
  };

  const openProject = (project: Project) => {
    saveRecentProjectId(project.id);
    navigate(`/projects/${project.id}`);
  };

  return (
    <main className="page-shell">
      <section className="hero-block">
        <div className="hero-copy">
          <Typography.Text className="eyebrow">AI Novel Script Studio</Typography.Text>
          <Typography.Title>
            先整理小说项目，
            <br />
            再让剧本改编有上下文可循。
          </Typography.Title>
          <Typography.Paragraph>
            这个阶段先完成项目前台入口。你可以创建改编项目、持续补充章节，并用真实章节数量卡住后续 AI
            改编入口，让生成过程始终建立在完整上下文之上。
          </Typography.Paragraph>
        </div>
        <div className="paper-note">
          <span>3 章以上</span>
          <span>项目独立上下文</span>
          <span>以 Story Bible 做准绳</span>
        </div>
      </section>

      <section className="home-grid">
        <div className="panel">
          <div className="panel-heading">
            <Typography.Text className="eyebrow">新建项目</Typography.Text>
            <Typography.Title level={3}>创建一本新的小说改编项目</Typography.Title>
          </div>
          <Form layout="vertical" form={form} onFinish={handleSubmit}>
            <Form.Item
              label="项目标题"
              name="title"
              rules={[
                { required: true, message: "请填写项目标题" },
                { max: 255, message: "项目标题不能超过 255 个字符" }
              ]}
            >
              <Input placeholder="例如：长夜余烬" size="large" />
            </Form.Item>
            <Form.Item
              label="项目简介"
              name="description"
              rules={[{ max: 2000, message: "简介不能超过 2000 个字符" }]}
            >
              <Input.TextArea placeholder="写下题材、基调、预计改编方向。" rows={5} />
            </Form.Item>
            <Button type="primary" htmlType="submit" size="large" loading={createProjectMutation.isPending}>
              创建项目并进入录入
            </Button>
          </Form>
        </div>

        <div className="panel panel-soft">
          <div className="panel-heading">
            <Typography.Text className="eyebrow">项目列表</Typography.Text>
            <Typography.Title level={3}>继续上次的录入工作</Typography.Title>
          </div>
          {projectsQuery.isLoading ? (
            <div className="empty-state">
              <Spin />
            </div>
          ) : (
            <List
              dataSource={orderedProjects}
              locale={{ emptyText: "还没有项目，先创建一个吧。" }}
              renderItem={(project) => (
                <List.Item
                  className="project-list-item"
                  actions={[
                    <Button type="link" key="open" icon={<ArrowRightOutlined />} onClick={() => openProject(project)}>
                      进入
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <div className="list-title-row">
                        <span>{project.title}</span>
                        {recentProjectIds.includes(project.id) ? <Tag color="blue">最近使用</Tag> : null}
                      </div>
                    }
                    description={
                      <div className="list-description">
                        <span>{project.description || "暂无项目简介"}</span>
                        <span>已录入 {project.chapterCount} 章</span>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </div>
      </section>
    </main>
  );
}
