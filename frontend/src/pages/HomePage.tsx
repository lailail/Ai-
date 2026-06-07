import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRightOutlined } from "@ant-design/icons";
import { Button, Form, Input, Select, Spin, Tag, Typography, message } from "antd";
import { useNavigate } from "react-router-dom";
import { createProject, listProjects } from "../api/projects";
import type { CreateProjectPayload } from "../types/project";
import { getRecentProjectIds, saveRecentProjectId } from "../utils/recent-projects";

type ProjectFormValues = {
  title: string;
  description?: string;
};

/**
 * 项目首页，负责创建项目和进入已有项目。
 */
export function HomePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form] = Form.useForm<ProjectFormValues>();
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
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

  const selectedProject = useMemo(
    () => orderedProjects.find((project) => project.id === selectedProjectId) ?? null,
    [orderedProjects, selectedProjectId]
  );

  useEffect(() => {
    if (orderedProjects.length === 0) {
      setSelectedProjectId(null);
      return;
    }

    const nextProject = orderedProjects.find((project) => recentProjectIds.includes(project.id)) ?? orderedProjects[0];
    setSelectedProjectId((current) => {
      if (current && orderedProjects.some((project) => project.id === current)) {
        return current;
      }
      return nextProject.id;
    });
  }, [orderedProjects, recentProjectIds]);

  /**
   * 提交项目创建表单。
   *
   * @param values 项目表单值
   */
  const handleSubmit = (values: ProjectFormValues) => {
    createProjectMutation.mutate(values);
  };

  /**
   * 进入当前选中的项目工作台。
   */
  const openSelectedProject = () => {
    if (!selectedProject) {
      return;
    }
    saveRecentProjectId(selectedProject.id);
    navigate(`/projects/${selectedProject.id}`);
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
            这里先完成项目前台入口。你可以创建改编项目、持续补充章节，并用真实章节数量卡住后续 AI
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
            <Typography.Text className="eyebrow">继续已有项目</Typography.Text>
            <Typography.Title level={3}>从下拉列表进入已有工作台</Typography.Title>
          </div>

          {projectsQuery.isLoading ? (
            <div className="empty-state">
              <Spin />
            </div>
          ) : orderedProjects.length === 0 ? (
            <div className="empty-state">
              <Typography.Paragraph>还没有项目，先创建一个吧。</Typography.Paragraph>
            </div>
          ) : (
            <div className="workspace-tab-stack">
              <Select
                aria-label="项目选择"
                size="large"
                showSearch
                placeholder="选择一个项目"
                value={selectedProjectId ?? undefined}
                onChange={setSelectedProjectId}
                optionFilterProp="label"
                options={orderedProjects.map((project) => ({
                  value: project.id,
                  label: project.title
                }))}
              />

              {selectedProject ? (
                <div className="project-list-item">
                  <div className="list-title-row">
                    <Typography.Title level={5} style={{ marginBottom: 0 }}>
                      {selectedProject.title}
                    </Typography.Title>
                    {recentProjectIds.includes(selectedProject.id) ? <Tag color="blue">最近使用</Tag> : null}
                  </div>
                  <div className="list-description">
                    <span>{selectedProject.description || "暂无项目简介"}</span>
                    <span>已录入 {selectedProject.chapterCount} 章</span>
                  </div>
                  <Button type="primary" icon={<ArrowRightOutlined />} onClick={openSelectedProject}>
                    进入项目
                  </Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
