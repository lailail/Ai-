import { getNextChapterNo } from "./chapter";

describe("getNextChapterNo", () => {
  it("should return one when there is no existing chapter", () => {
    expect(getNextChapterNo([])).toBe(1);
  });

  it("should return the next chapter number based on the current maximum", () => {
    expect(
      getNextChapterNo([
        { chapterNo: 1 },
        { chapterNo: 2 },
        { chapterNo: 4 }
      ])
    ).toBe(5);
  });
});
