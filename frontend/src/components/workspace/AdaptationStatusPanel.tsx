import { Alert, Button, Progress, Space, Steps, Tag, Typography } from "antd";
import type { AdaptationJobSnapshot, AdaptationScript } from "../../types/adaptation";
import { ADAPTATION_STAGE_ITEMS } from "../../utils/adaptation-progress";
import { canStartAdaptation } from "../../utils/chapter";

type AdaptationStatusPanelProps = {
  chapterCount: number;
  isStarting: boolean;
  latestJob: AdaptationJobSnapshot | null;
  latestScript: AdaptationScript | null;
  onOpenStoryBible: () => void;
  onOpenYamlPreview: () => void;
  onStart: () => void;
};

export function AdaptationStatusPanel({
  chapterCount,
  isStarting,
  latestJob,
  latestScript,
  onOpenStoryBible,
  onOpenYamlPreview,
  onStart
}: AdaptationStatusPanelProps) {
  const enabled = canStartAdaptation(chapterCount);
  const currentStep = Math.max((latestJob?.currentStageIndex ?? 1) - 1, 0);
  const progressStatus =
    latestJob?.status === "FAILED" ? "exception" : latestJob?.status === "SUCCEEDED" ? "success" : "active";

  return (
    <section className="panel panel-contrast">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">改编进度</Typography.Text>
        <Typography.Title level={4}>生成当前项目的剧本初稿</Typography.Title>
      </div>
      <Typography.Paragraph className="panel-copy">
        当前阶段使用分步骤流水线生成，点击后会依次完成章节上下文提取、Story Bible 构建和 YAML 初稿输出。
      </Typography.Paragraph>

      <div className="action-row">
        <Button type="primary" size="large" disabled={!enabled} loading={isStarting} onClick={onStart}>
          开始改编
        </Button>
        <Typography.Text type={enabled ? "success" : "secondary"}>
          {enabled ? "章节数量已达标，可以开始生成剧本初稿。" : "至少需要录入 3 章内容后才能开始生成剧本初稿。"}
        </Typography.Text>
      </div>

      {latestJob ? (
        <div className="progress-shell">
          <div className="progress-heading">
            <Typography.Text strong>
              {latestJob.status === "FAILED"
                ? `任务失败阶段：${latestJob.currentStageLabel}`
                : `当前正在进行：${latestJob.currentStageLabel}`}
            </Typography.Text>
            <Tag color={latestJob.status === "FAILED" ? "error" : latestJob.status === "SUCCEEDED" ? "success" : "processing"}>
              {latestJob.status}
            </Tag>
          </div>
          <Progress percent={latestJob.progressPercent} status={progressStatus} />
          <Steps
            className="workspace-steps"
            current={currentStep}
            items={ADAPTATION_STAGE_ITEMS.map((item) => ({ title: item.label }))}
            size="small"
          />

          {latestJob.status === "FAILED" ? (
            <Alert
              className="workspace-alert"
              type="error"
              showIcon
              message="改编任务执行失败"
              description={latestJob.errorMessage ?? "请检查模型返回结果或稍后重试。"}
            />
          ) : null}
        </div>
      ) : null}

      {latestScript ? (
        <div className="status-summary">
          <Typography.Text strong>当前已生成到第 {latestScript.versionNo} 版。</Typography.Text>
          <Space wrap>
            <Tag color={latestScript.validationStatus === "PASSED" ? "success" : "error"}>
              Schema 校验：{latestScript.validationStatus}
            </Tag>
            <Tag color={latestScript.jobStatus === "SUCCEEDED" ? "processing" : "gold"}>
              任务状态：{latestScript.jobStatus ?? "UNKNOWN"}
            </Tag>
          </Space>
          <Typography.Paragraph className="status-note">{latestScript.title}</Typography.Paragraph>
          <Space wrap>
            <Button onClick={onOpenStoryBible}>查看 Story Bible</Button>
            <Button onClick={onOpenYamlPreview}>查看 YAML 初稿</Button>
          </Space>
        </div>
      ) : null}

      {!latestJob && !latestScript ? (
        <Alert
          className="workspace-alert"
          type="info"
          showIcon
          message="还没有生成结果"
          description="完成第一次改编后，这里会显示最新版本号、Schema 校验状态和任务结果。"
        />
      ) : null}
    </section>
  );
}
