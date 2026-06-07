import { fireEvent, render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { ScreenplayEditorPanel } from "./ScreenplayEditorPanel";

vi.mock("@monaco-editor/react", () => ({
  default: ({
    value,
    onChange
  }: {
    value?: string;
    onChange?: (value: string) => void;
  }) => (
    <textarea
      data-testid="screenplay-editor"
      value={value ?? ""}
      onChange={(event) => onChange?.(event.target.value)}
    />
  )
}));

describe("ScreenplayEditorPanel", () => {
  it("should render screenplay title and editor draft", () => {
    render(
      <ScreenplayEditorPanel
        selectedScreenplay={{
          projectId: 1,
          scriptVersionId: 8,
          versionNo: 3,
          title: "河岸重访",
          sourceType: "USER_EDITED",
          renderVersion: "v1",
          markdownContent: "# 河岸重访",
          createdAt: "2026-06-06T16:00:00",
          updatedAt: "2026-06-06T16:20:00"
        }}
        draftTitle="作者修订版剧本"
        draftMarkdownContent="# 河岸重访\n\n阿述望向河面。"
        hasUnsavedChanges={true}
        isLoading={false}
        onDraftTitleChange={() => {}}
        onDraftMarkdownChange={() => {}}
      />
    );

    expect(screen.getByText("河岸重访")).toBeInTheDocument();
    expect(screen.getByDisplayValue("作者修订版剧本")).toBeInTheDocument();
    expect(screen.getByTestId("screenplay-editor")).toHaveValue("# 河岸重访\\n\\n阿述望向河面。");
    expect(screen.getByText("当前正式剧本存在未保存修改")).toBeInTheDocument();
  });

  it("should trigger title and markdown change callbacks", () => {
    const handleDraftTitleChange = vi.fn();
    const handleDraftMarkdownChange = vi.fn();

    render(
      <ScreenplayEditorPanel
        selectedScreenplay={{
          projectId: 1,
          scriptVersionId: 8,
          versionNo: 3,
          title: "河岸重访",
          sourceType: "USER_EDITED",
          renderVersion: "v1",
          markdownContent: "# 河岸重访",
          createdAt: "2026-06-06T16:00:00",
          updatedAt: "2026-06-06T16:20:00"
        }}
        draftTitle="作者修订版剧本"
        draftMarkdownContent="# 河岸重访"
        hasUnsavedChanges={false}
        isLoading={false}
        onDraftTitleChange={handleDraftTitleChange}
        onDraftMarkdownChange={handleDraftMarkdownChange}
      />
    );

    fireEvent.change(screen.getByLabelText("正式剧本版本标题"), { target: { value: "短剧精修版" } });
    fireEvent.change(screen.getByTestId("screenplay-editor"), { target: { value: "# 新版剧本" } });

    expect(handleDraftTitleChange).toHaveBeenCalledWith("短剧精修版");
    expect(handleDraftMarkdownChange).toHaveBeenCalledWith("# 新版剧本");
  });
});
