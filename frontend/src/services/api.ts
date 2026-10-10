import axios from "axios";
import type { ResumePage, ResumeRecord } from "../types/resume";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000/api",
  timeout: 30_000,
});

export async function parseResume(
  file: File,
  onProgress: (percent: number) => void,
): Promise<ResumeRecord> {
  const body = new FormData();
  body.append("file", file);
  const response = await api.post<ResumeRecord>("/resumes/parse", body, {
    onUploadProgress: (event) => {
      if (event.total) onProgress(Math.round((event.loaded / event.total) * 100));
    },
  });
  return response.data;
}

export async function getResume(id: string): Promise<ResumeRecord> {
  return (await api.get<ResumeRecord>(`/resumes/${encodeURIComponent(id)}`)).data;
}

export async function getHistory(page: number, pageSize = 10): Promise<ResumePage> {
  return (await api.get<ResumePage>("/resumes", { params: { page, page_size: pageSize } })).data;
}

export async function deleteResume(id: string): Promise<void> {
  await api.delete(`/resumes/${encodeURIComponent(id)}`);
}

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<{ detail?: string }>(error)) {
    return error.response?.data?.detail || "The API could not be reached. Check that the backend is running.";
  }
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}
