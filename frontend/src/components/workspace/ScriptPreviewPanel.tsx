import { useState } from "react";
import Editor from "@monaco-editor/react";
import { Alert, Button, Input, Skeleton, Space, Tag, Typography } from "antd";
import type { AdaptationScript, ScriptValidationResult, ScriptVersionSummary } from "../../types/adaptation";
import { YamlFieldGuideDrawer } from "./YamlFieldGuideDrawer";
import { YamlValidationPanel } from "./YamlValidationPanel";
import { YamlVersionList } from "./YamlVersionList";

type ScriptPreviewPanelProps = {
  versionSummaries: ScriptVersionSummary[];
  selectedScript: AdaptationScript | null;
  draftTitle: string;
  draftYamlContent: string;
  validationResult: ScriptValidationResult | null;
  hasUnsavedChanges: boolean;
  isListLoading: boolean;
  isDetailLoading: boolean;
  isValidating: boolean;
  isSaving: boolean;
  onSelectVersion: (scriptVersionId: number) => void;
  onDraftTitleChange: (value: string) => void;
  onDraftYamlChange: (value: string) => void;
  onValidate: () => void;
  onSave: () => void;
  onExportYaml: () => void;
};

/**
 * 承载 YAML 初稿的版本切换、编辑、校验和另存新版本工作区。
 */
export function ScriptPreviewPanel({
  versionSummaries,
  selectedScript,
  draftTitle,
  draftYamlContent,
  validationResult,
  hasUnsavedChanges,
  isListLoading,
  isDetailLoading,
  isValidating,
  isSaving,
  onSelectVersion,
  onDraftTitleChange,
  onDraftYamlChange,
  onValidate,
  onSave,
  onExportYaml
}: ScriptPreviewPanelProps) {
  const [fieldGuideOpen, setFieldGuideOpen] = useState(false);

  if (!isListLoading && versionSummaries.length === 0) {
    return (
      <section className="panel panel-soft">
        <div className="panel-heading">
          <Typography.Text className="eyebrow">剧本工作区</Typography.Text>
          <Typography.Title level={4}>YAML 版本化编辑区</Typography.Title>
        </div>
        <Alert
          className="workspace-alert"
          type="info"
          showIcon
          message="当前项目还没有 YAML 结果"
          description="请先完成一次改编生成，系统会在这里展示版本列表、编辑器和 Schema 校验结果。"
        />
      </section>
    );
  }

  return (
    <section className="panel panel-soft">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">剧本工作区</Typography.Text>
        <Typography.Title level={4}>YAML 版本化编辑区</Typography.Title>
      </div>
      <Typography.Paragraph className="panel-copy">
        在这里维护结构化剧本初稿。你可以切换历史版本、执行 Schema 校验、查看字段说明，并把当前内容导出为 YAML。
      </Typography.Paragraph>

      <div className="yaml-workspace">
        <YamlVersionList
          versionSummaries={versionSummaries}
          selectedScriptVersionId={selectedScript?.scriptVersionId ?? null}
          isLoading={isListLoading}
          onSelectVersion={onSelectVersion}
        />

        <section className="yaml-editor-panel">
          {isDetailLoading ? (
            <Skeleton active paragraph={{ rows: 16 }} />
          ) : !selectedScript ? (
            <Alert
              type="info"
              showIcon
              message="请选择一个剧本版本"
              description="左侧会展示当前项目下的所有 YAML 版本，选择后即可在中间工作区查看和编辑。"
            />
          ) : (
            <>
              <div className="yaml-editor-heading">
                <div>
                  <Typography.Text className="eyebrow">当前版本</Typography.Text>
                  <Typography.Title level={5}>{selectedScript.title}</Typography.Title>
                </div>
                <Space size={[8, 8]} wrap>
                  <Tag color="blue">第 {selectedScript.versionNo} 版</Tag>
                  <Tag color="gold">{renderSourceType(selectedScript.sourceType)}</Tag>
                  <Tag color={selectedScript.validationStatus === "PASSED" ? "success" : "error"}>
                    {selectedScript.validationStatus === "PASSED" ? "已通过" : "未通过"}
                  </Tag>
                </Space>
              </div>

              {hasUnsavedChanges ? (
                <Alert
                  className="workspace-alert"
                  type="warning"
                  showIcon
                  message="当前初稿存在未保存修改"
                  description="你可以先执行 YAML 校验，再将修改内容另存为新版本。"
                />
              ) : null}

              <div className="yaml-meta-row">
                <div className="yaml-title-field">
                  <Typography.Text strong>版本标题</Typography.Text>
                  <Input
                    aria-label="版本标题"
                    value={draftTitle}
                    placeholder="例如：作者精修版 / 节奏优化版"
                    onChange={(event) => onDraftTitleChange(event.target.value)}
                  />
                </div>
                <div className="yaml-meta-tags">
                  <Tag>Schema {selectedScript.schemaVersion}</Tag>
                  <Tag>{formatVersionTime(selectedScript.createdAt)}</Tag>
                </div>
              </div>

              <div className="yaml-toolbar">
                <Space size={[8, 8]} wrap>
                  <Button onClick={() => setFieldGuideOpen(true)}>字段说明</Button>
                  <Button onClick={onExportYaml} disabled={!draftYamlContent.trim()}>
                    导出 YAML
                  </Button>
                </Space>
              </div>

              <div className="yaml-editor-shell">
                <Editor
                  height="620px"
                  language="yaml"
                  theme="vs"
                  value={draftYamlContent}
                  onChange={(value) => onDraftYamlChange(value ?? "")}
                  options={{
                    automaticLayout: true,
                    fontFamily: "JetBrains Mono, Cascadia Code, monospace",
                    fontSize: 13,
                    minimap: { enabled: false },
                    scrollBeyondLastLine: false,
                    wordWrap: "on"
                  }}
                />
              </div>

              <Space wrap>
                <Button onClick={onValidate} loading={isValidating} disabled={!draftYamlContent.trim()}>
                  校验 YAML
                </Button>
                <Button
                  type="primary"
                  onClick={onSave}
                  loading={isSaving}
                  disabled={!draftYamlContent.trim() || !hasUnsavedChanges}
                >
                  另存为新版本
                </Button>
              </Space>
            </>
          )}
        </section>

        <YamlValidationPanel validationResult={validationResult} />
      </div>

      <YamlFieldGuideDrawer open={fieldGuideOpen} onClose={() => setFieldGuideOpen(false)} />
    </section>
  );
}

/**
 * 将来源类型转换为更易读的中文标签。
 *
 * @param sourceType 剧本版本来源类型
 * @returns 中文展示文案
 */
function renderSourceType(sourceType: string) {
  if (sourceType === "USER_EDITED") {
    return "人工编辑";
  }
  if (sourceType === "AI_GENERATED") {
    return "AI 生成";
  }
  return sourceType;
}

/**
 * 将后端返回的时间字符串格式化为界面展示文案。
 *
 * @param createdAt 版本创建时间
 * @returns 格式化后的时间
 */
function formatVersionTime(createdAt: string) {
  return createdAt.replace("T", " ").slice(0, 16);
}
