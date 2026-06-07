import Editor from "@monaco-editor/react";
import { Alert, Input, Skeleton, Space, Tag, Typography } from "antd";
import type { ScreenplaySnapshot } from "../../types/adaptation";

type ScreenplayEditorPanelProps = {
  selectedScreenplay: ScreenplaySnapshot | null;
  draftTitle: string;
  draftMarkdownContent: string;
  hasUnsavedChanges: boolean;
  isLoading: boolean;
  onDraftTitleChange: (value: string) => void;
  onDraftMarkdownChange: (value: string) => void;
};

/**
 * 展示正式剧本正文编辑区。
 */
export function ScreenplayEditorPanel({
  selectedScreenplay,
  draftTitle,
  draftMarkdownContent,
  hasUnsavedChanges,
  isLoading,
  onDraftTitleChange,
  onDraftMarkdownChange
}: ScreenplayEditorPanelProps) {
  return (
    <section className="screenplay-editor-panel">
      {isLoading ? (
        <Skeleton active paragraph={{ rows: 16 }} />
      ) : !selectedScreenplay ? (
        <Alert
          type="info"
          showIcon
          message="请选择一个正式剧本版本"
          description="左侧会列出当前项目的剧本版本。选中后即可在这里继续修改正式剧本。"
        />
      ) : (
        <>
          <div className="yaml-editor-heading">
            <div>
              <Typography.Text className="eyebrow">正式剧本编辑器</Typography.Text>
              <Typography.Title level={4}>{selectedScreenplay.title}</Typography.Title>
            </div>
            <Space size={[8, 8]} wrap>
              <Tag color="blue">第 {selectedScreenplay.versionNo} 版</Tag>
              <Tag color="gold">{renderSourceType(selectedScreenplay.sourceType)}</Tag>
              <Tag>渲染规则 {selectedScreenplay.renderVersion}</Tag>
            </Space>
          </div>

          {hasUnsavedChanges ? (
            <Alert
              className="workspace-alert"
              type="warning"
              showIcon
              message="当前正式剧本存在未保存修改"
              description="保存后会自动回写 YAML，并落成新的剧本版本。"
            />
          ) : null}

          <div className="yaml-meta-row">
            <div className="yaml-title-field">
              <Typography.Text strong>新版本标题</Typography.Text>
              <Input
                aria-label="正式剧本版本标题"
                value={draftTitle}
                placeholder="例如：短剧节奏修订版"
                onChange={(event) => onDraftTitleChange(event.target.value)}
              />
            </div>
            <div className="yaml-meta-tags">
              <Tag>{formatVersionTime(selectedScreenplay.updatedAt)}</Tag>
            </div>
          </div>

          <div className="screenplay-editor-shell">
            <Editor
              height="720px"
              language="markdown"
              theme="vs"
              value={draftMarkdownContent}
              onChange={(value) => onDraftMarkdownChange(value ?? "")}
              options={{
                automaticLayout: true,
                fontFamily: "\"Noto Serif SC\", \"Source Han Serif SC\", serif",
                fontSize: 15,
                lineHeight: 26,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                wordWrap: "on"
              }}
            />
          </div>
        </>
      )}
    </section>
  );
}

/**
 * 将后端来源类型转换为更易读的中文标签。
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
 * 将时间字符串格式化为更易读的展示文本。
 *
 * @param updatedAt 更新时间
 * @returns 格式化后的时间
 */
function formatVersionTime(updatedAt: string) {
  return updatedAt.replace("T", " ").slice(0, 16);
}
