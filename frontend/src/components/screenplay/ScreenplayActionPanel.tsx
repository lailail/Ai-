import { Alert, Button, Space, Tag, Typography } from "antd";
import type { ScreenplaySnapshot } from "../../types/adaptation";

type ScreenplayActionPanelProps = {
  selectedScreenplay: ScreenplaySnapshot | null;
  hasUnsavedChanges: boolean;
  isRendering: boolean;
  isSaving: boolean;
  onRender: () => void;
  onSave: () => void;
  onExportMarkdown: () => void;
  onExportText: () => void;
  onBackToYaml: () => void;
};

/**
 * 展示正式剧本页面右侧的操作面板。
 */
export function ScreenplayActionPanel({
  selectedScreenplay,
  hasUnsavedChanges,
  isRendering,
  isSaving,
  onRender,
  onSave,
  onExportMarkdown,
  onExportText,
  onBackToYaml
}: ScreenplayActionPanelProps) {
  return (
    <section className="screenplay-side-panel screenplay-action-panel">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">操作区</Typography.Text>
        <Typography.Title level={5}>正式剧本操作</Typography.Title>
      </div>

      <Typography.Paragraph className="tiny-copy">
        这里展示的是适合作者直接阅读和润色的正式剧本视图。保存后，系统会把修改内容回写到 YAML 并生成新版本。
      </Typography.Paragraph>

      {selectedScreenplay ? (
        <div className="screenplay-summary">
          <Space size={[8, 8]} wrap>
            <Tag color="blue">版本 {selectedScreenplay.versionNo}</Tag>
            <Tag>{selectedScreenplay.renderVersion}</Tag>
          </Space>
          <Typography.Paragraph className="tiny-copy">
            当前正在编辑《{selectedScreenplay.title}》。
          </Typography.Paragraph>
        </div>
      ) : null}

      {hasUnsavedChanges ? (
        <Alert
          className="workspace-alert"
          type="warning"
          showIcon
          message="检测到未保存修改"
          description="点击保存正式剧本后，会自动生成新的 YAML 版本和正式剧本快照。"
        />
      ) : (
        <Alert
          className="workspace-alert"
          type="info"
          showIcon
          message="当前内容已与所选版本一致"
          description="如果想基于最新 YAML 重新生成正式剧本，可以使用下面的重新渲染按钮。"
        />
      )}

      <div className="action-row">
        <Button onClick={onRender} loading={isRendering} disabled={!selectedScreenplay}>
          重新渲染正式剧本
        </Button>
        <Button type="primary" onClick={onSave} loading={isSaving} disabled={!selectedScreenplay || !hasUnsavedChanges}>
          保存正式剧本并更新 YAML
        </Button>
        <Button onClick={onExportMarkdown} disabled={!selectedScreenplay}>
          导出 Markdown
        </Button>
        <Button onClick={onExportText} disabled={!selectedScreenplay}>
          导出 TXT
        </Button>
        <Button onClick={onBackToYaml}>返回 YAML 初稿</Button>
      </div>
    </section>
  );
}
