import { describe, expect, it } from "vitest";
import { extractDownloadFileName } from "./download";

describe("extractDownloadFileName", () => {
  it("should prefer utf8 filename star when content disposition contains chinese file name", () => {
    const contentDisposition =
      "attachment; filename=\"screenplay-v3.md\"; filename*=UTF-8''%E9%95%BF%E5%A4%9C%E4%BD%99%E7%83%AC.md";

    expect(extractDownloadFileName(contentDisposition, "screenplay.md")).toBe("长夜余烬.md");
  });

  it("should fall back to quoted filename when filename star is absent", () => {
    const contentDisposition = "attachment; filename=\"screenplay-v3.txt\"";

    expect(extractDownloadFileName(contentDisposition, "screenplay.txt")).toBe("screenplay-v3.txt");
  });

  it("should return fallback when content disposition is missing", () => {
    expect(extractDownloadFileName(null, "screenplay.txt")).toBe("screenplay.txt");
  });
});
