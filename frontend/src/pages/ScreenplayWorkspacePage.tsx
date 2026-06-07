import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Spin, Typography, message } from "antd";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  downloadProjectScreenplay,
  getProject,
  getProjectScreenplay,
  listProjectScriptVersions,
  renderProjectScreenplay,
  saveProjectScreenplay
} from "../api/projects";
import { ScreenplayActionPanel } from "../components/screenplay/ScreenplayActionPanel";
import { ScreenplayEditorPanel } from "../components/screenplay/ScreenplayEditorPanel";
import { ScreenplayVersionList } from "../components/screenplay/ScreenplayVersionList";

/**
 * 承载正式剧本阅读和编辑的独立页面。
 */
export function ScreenplayWorkspacePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const { projectId } = useParams();
  const numericProjectId = Number(projectId);
  const isValidProjectId = Number.isInteger(numericProjectId) && numericProjectId > 0;
  const initialVersionId = Number(searchParams.get("version"));
  const [selectedScriptVersionId, setSelectedScriptVersionId] = useState<number | null>(
    Number.isInteger(initialVersionId) && initialVersionId > 0 ? initialVersionId : null
  );
  const [draftSourceVersionId, setDraftSourceVersionId] = useState<number | null>(null);
  const [draftTitle, setDraftTitle] = useState("");
  const [draftMarkdownContent, setDraftMarkdownContent] = useState("");

  const projectQuery = useQuery({
    queryKey: ["project", numericProjectId],
    queryFn: () => getProject(numericProjectId),
    enabled: isValidProjectId
  });

  const versionSummariesQuery = useQuery({
    queryKey: ["project-script-versions", numericProjectId],
    queryFn: () => listProjectScriptVersions(numericProjectId),
    enabled: isValidProjectId
  });

  const selectedScreenplayQuery = useQuery({
    queryKey: ["project-screenplay", numericProjectId, selectedScriptVersionId],
    queryFn: () => getProjectScreenplay(numericProjectId, selectedScriptVersionId as number),
    enabled: isValidProjectId && selectedScriptVersionId !== null
  });

  useEffect(() => {
    const versionSummaries = versionSummariesQuery.data ?? [];
    if (versionSummaries.length === 0) {
      setSelectedScriptVersionId(null);
      return;
    }

    const stillExists = versionSummaries.some((item) => item.scriptVersionId === selectedScriptVersionId);
    if (!stillExists) {
      const nextVersionId = versionSummaries[0].scriptVersionId;
      setSelectedScriptVersionId(nextVersionId);
      setSearchParams({ version: String(nextVersionId) });
    }
  }, [selectedScriptVersionId, setSearchParams, versionSummariesQuery.data]);

  useEffect(() => {
    const screenplay = selectedScreenplayQuery.data;
    if (!screenplay || draftSourceVersionId === screenplay.scriptVersionId) {
      return;
    }

    setDraftSourceVersionId(screenplay.scriptVersionId);
    setDraftTitle(screenplay.title);
    setDraftMarkdownContent(screenplay.markdownContent);
  }, [draftSourceVersionId, selectedScreenplayQuery.data]);

  const renderScreenplayMutation = useMutation({
    mutationFn: () => renderProjectScreenplay(numericProjectId, selectedScriptVersionId as number),
    onSuccess: async (screenplay) => {
      setSelectedScriptVersionId(screenplay.scriptVersionId);
      setDraftSourceVersionId(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-screenplay", numericProjectId, screenplay.scriptVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] })
      ]);
      message.success("正式剧本已按当前 YAML 重新渲染。");
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const saveScreenplayMutation = useMutation({
    mutationFn: () => saveProjectScreenplay(
      numericProjectId,
      selectedScriptVersionId as number,
      draftTitle,
      draftMarkdownContent
    ),
    onSuccess: async (script) => {
      const nextVersionId = script.scriptVersionId;
      setSelectedScriptVersionId(nextVersionId);
      setDraftSourceVersionId(null);
      setSearchParams({ version: String(nextVersionId) });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["project-script-versions", numericProjectId] }),
        queryClient.invalidateQueries({ queryKey: ["project-screenplay", numericProjectId, nextVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-script-version", numericProjectId, nextVersionId] }),
        queryClient.invalidateQueries({ queryKey: ["project-latest-script", numericProjectId] })
      ]);
      message.success(`正式剧本已保存，并生成第 ${script.versionNo} 版 YAML。`);
    },
    onError: (error: Error) => {
      message.error(error.message);
    }
  });

  const selectedScreenplay = selectedScreenplayQuery.data ?? null;
  const hasUnsavedChanges = Boolean(
    selectedScreenplay &&
      (draftTitle !== selectedScreenplay.title || draftMarkdownContent !== selectedScreenplay.markdownContent)
  );

  if (!isValidProjectId) {
    return (
      <main className="page-shell">
        <div className="panel">
          <Typography.Title level={3}>项目地址无效</Typography.Title>
        </div>
      </main>
    );
  }

  if (projectQuery.isLoading || versionSummariesQuery.isLoading) {
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
          <Typography.Title level={3}>正式剧本页面加载失败</Typography.Title>
          <Typography.Paragraph>
            {projectQuery.error instanceof Error ? projectQuery.error.message : "请稍后重试。"}
          </Typography.Paragraph>
        </div>
      </main>
    );
  }

  return (
    <main className="page-shell">
      <section className="workspace-header">
        <div>
          <Typography.Text className="eyebrow">正式剧本页面</Typography.Text>
          <Typography.Title>{projectQuery.data.title}</Typography.Title>
          <Typography.Paragraph>
            在这个页面里，你看到的是更接近真实影视写作的剧本视图。这里的保存会自动回写 YAML，并保留版本历史。
          </Typography.Paragraph>
        </div>
      </section>

      {versionSummariesQuery.isError ? (
        <Alert
          className="workspace-alert"
          type="error"
          showIcon
          message="剧本版本列表查询失败"
          description={versionSummariesQuery.error instanceof Error ? versionSummariesQuery.error.message : "请稍后重试。"}
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

      <div className="screenplay-workspace">
        <ScreenplayVersionList
          versionSummaries={versionSummariesQuery.data ?? []}
          selectedScriptVersionId={selectedScriptVersionId}
          isLoading={versionSummariesQuery.isLoading}
          onSelectVersion={(scriptVersionId) => {
            setSelectedScriptVersionId(scriptVersionId);
            setDraftSourceVersionId(null);
            setSearchParams({ version: String(scriptVersionId) });
          }}
        />

        <ScreenplayEditorPanel
          selectedScreenplay={selectedScreenplay}
          draftTitle={draftTitle}
          draftMarkdownContent={draftMarkdownContent}
          hasUnsavedChanges={hasUnsavedChanges}
          isLoading={selectedScreenplayQuery.isLoading}
          onDraftTitleChange={setDraftTitle}
          onDraftMarkdownChange={setDraftMarkdownContent}
        />

        <ScreenplayActionPanel
          selectedScreenplay={selectedScreenplay}
          hasUnsavedChanges={hasUnsavedChanges}
          isRendering={renderScreenplayMutation.isPending}
          isSaving={saveScreenplayMutation.isPending}
          onRender={() => renderScreenplayMutation.mutate()}
          onSave={() => saveScreenplayMutation.mutate()}
          onExportMarkdown={() => {
            void downloadScreenplay(numericProjectId, selectedScreenplay?.scriptVersionId ?? null, "md");
          }}
          onExportText={() => {
            void downloadScreenplay(numericProjectId, selectedScreenplay?.scriptVersionId ?? null, "txt");
          }}
          onBackToYaml={() => {
            const suffix = selectedScriptVersionId ? `?version=${selectedScriptVersionId}` : "";
            navigate(`/projects/${numericProjectId}${suffix}`);
          }}
        />
      </div>
    </main>
  );
}

/**
 * 下载正式剧本文件，并在失败时反馈提示。
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
