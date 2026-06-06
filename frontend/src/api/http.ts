import type { ApiResponse } from "../types/api";

export async function requestJson<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    }
  });

  const text = await response.text();
  const json = text ? (JSON.parse(text) as ApiResponse<T>) : null;

  if (!response.ok) {
    throw new Error(json?.message ?? `请求失败，状态码：${response.status}`);
  }

  if (!json?.success) {
    throw new Error(json?.message ?? "请求未成功");
  }

  return json.data;
}
