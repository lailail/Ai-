/**
 * 从下载响应头中提取浏览器可用的文件名。
 *
 * @param contentDisposition 下载响应头
 * @param fallback 默认文件名
 * @returns 优先使用 UTF-8 文件名参数的结果
 */
export function extractDownloadFileName(contentDisposition: string | null, fallback: string) {
  if (!contentDisposition) {
    return fallback;
  }

  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return fallback;
    }
  }

  const quotedMatch = /filename="([^"]+)"/i.exec(contentDisposition);
  if (quotedMatch?.[1]) {
    return quotedMatch[1];
  }

  return fallback;
}
