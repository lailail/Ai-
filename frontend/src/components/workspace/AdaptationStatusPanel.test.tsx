import { render, screen } from "@testing-library/react";
import { AdaptationStatusPanel } from "./AdaptationStatusPanel";

describe("AdaptationStatusPanel", () => {
  it("should keep the start button disabled when chapter count is less than three", () => {
    render(
      <AdaptationStatusPanel chapterCount={2} isStarting={false} latestJob={null} latestScript={null} onStart={() => {}} />
    );

    expect(screen.getByRole("button", { name: "开始改编" })).toBeDisabled();
    expect(screen.getByText("至少需要录入 3 章内容后才能开始生成剧本初稿。")).toBeInTheDocument();
  });

  it("should show stage progress when an adaptation job is running", () => {
    render(
      <AdaptationStatusPanel
        chapterCount={3}
        isStarting={false}
        latestJob={{
          projectId: 1,
          jobId: 9,
          status: "RUNNING",
          currentStage: "SCENE_GENERATE",
          currentStageLabel: "场景生成",
          currentStageIndex: 6,
          stageCount: 9,
          progressPercent: 67,
          errorStage: null,
          errorMessage: null,
          startedAt: "2026-06-06T20:30:00",
          finishedAt: null
        }}
        latestScript={null}
        onStart={() => {}}
      />
    );

    expect(screen.getByText("当前正在进行：场景生成")).toBeInTheDocument();
    expect(screen.getByText("67%")).toBeInTheDocument();
    expect(screen.getByText("场景生成")).toBeInTheDocument();
  });

  it("should not show legacy jump buttons when the latest script is available", () => {
    render(
      <AdaptationStatusPanel
        chapterCount={3}
        isStarting={false}
        latestJob={{
          projectId: 1,
          jobId: 9,
          status: "SUCCEEDED",
          currentStage: "COMPLETED",
          currentStageLabel: "已完成",
          currentStageIndex: 9,
          stageCount: 9,
          progressPercent: 100,
          errorStage: null,
          errorMessage: null,
          startedAt: "2026-06-06T20:30:00",
          finishedAt: "2026-06-06T20:31:00"
        }}
        latestScript={{
          projectId: 1,
          scriptVersionId: 11,
          versionNo: 2,
          title: "长夜余烬 - 旧城疑影",
          sourceType: "AI_GENERATED",
          schemaVersion: "1.0",
          validationStatus: "PASSED",
          yamlContent: "schema_version: 1.0",
          validationErrors: [],
          createdAt: "2026-06-06T20:31:00",
          jobId: 9,
          jobStatus: "SUCCEEDED"
        }}
        onStart={() => {}}
      />
    );

    expect(screen.getByText("当前已生成到第 2 版。")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "查看 Story Bible" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "查看 YAML 初稿" })).not.toBeInTheDocument();
  });
});
