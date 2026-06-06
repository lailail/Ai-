import { render, screen } from "@testing-library/react";
import { StartAdaptationCard } from "./StartAdaptationCard";

describe("StartAdaptationCard", () => {
  it("should disable start button when there are fewer than three chapters", () => {
    render(<StartAdaptationCard chapterCount={2} onStart={() => {}} />);

    expect(screen.getByRole("button", { name: "开始改编" })).toBeDisabled();
    expect(screen.getByText("至少需要录入 3 章内容后才能进入改编流程。")).toBeInTheDocument();
  });

  it("should enable start button when chapter count reaches three", () => {
    render(<StartAdaptationCard chapterCount={3} onStart={() => {}} />);

    expect(screen.getByRole("button", { name: "开始改编" })).toBeEnabled();
  });
});
