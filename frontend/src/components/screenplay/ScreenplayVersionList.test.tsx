import { fireEvent, render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { ScreenplayVersionList } from "./ScreenplayVersionList";

describe("ScreenplayVersionList", () => {
  it("should render select and switch screenplay version", async () => {
    const handleSelectVersion = vi.fn();

    render(
      <ScreenplayVersionList
        versionSummaries={[
          {
            projectId: 1,
            scriptVersionId: 21,
            versionNo: 4,
            title: "短剧精修版",
            sourceType: "USER_EDITED",
            validationStatus: "PASSED",
            latest: true,
            createdAt: "2026-06-07T10:00:00"
          },
          {
            projectId: 1,
            scriptVersionId: 20,
            versionNo: 3,
            title: "AI 初稿",
            sourceType: "AI_GENERATED",
            validationStatus: "FAILED",
            latest: false,
            createdAt: "2026-06-07T09:10:00"
          }
        ]}
        selectedScriptVersionId={21}
        isLoading={false}
        onSelectVersion={handleSelectVersion}
      />
    );

    expect(screen.getByRole("combobox", { name: "正式剧本版本选择" })).toBeInTheDocument();
    expect(screen.getAllByText("第 4 版 · 短剧精修版")).toHaveLength(2);
    expect(screen.getByText("YAML 已通过")).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole("combobox", { name: "正式剧本版本选择" }));
    fireEvent.click(await screen.findByText("第 3 版 · AI 初稿"));

    expect(handleSelectVersion).toHaveBeenCalledWith(
      20,
      expect.objectContaining({
        value: 20,
        label: "第 3 版 · AI 初稿"
      })
    );
  });
});
