import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { FileText, FileUp, LoaderCircle, ShieldCheck, Trash2 } from "lucide-react";
import { errorMessage, parseResume } from "../services/api";
import { useToast } from "../components/ToastProvider";

const configuredMax = Number(import.meta.env.VITE_MAX_UPLOAD_MB || "10");
const MAX_UPLOAD_MB = Number.isFinite(configuredMax) && configuredMax >= 1 && configuredMax <= 100 ? configuredMax : 10;
const MAX_FILE_BYTES = MAX_UPLOAD_MB * 1024 * 1024;
const ACCEPTED_MIME: Record<string, string[]> = {
  pdf: ["application/pdf", "application/octet-stream"],
  docx: ["application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream"],
};

function validateFile(file: File): string | null {
  const extension = file.name.split(".").pop()?.toLowerCase() || "";
  if (!(extension in ACCEPTED_MIME)) return "Choose a PDF or DOCX file.";
  if (!ACCEPTED_MIME[extension].includes(file.type)) return "The file type does not match its extension.";
  if (file.size === 0) return "This file is empty. Choose a different resume.";
  if (file.size > MAX_FILE_BYTES) return `This file is larger than the ${MAX_UPLOAD_MB} MB limit.`;
  return null;
}

function formatSize(bytes: number): string {
  return bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(0)} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function ParserPage() {
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const notify = useToast();

  function chooseFile(candidate?: File) {
    if (!candidate) return;
    const validationError = validateFile(candidate);
    if (validationError) {
      setFile(null);
      setError(validationError);
      return;
    }
    setError(null);
    setProgress(0);
    setFile(candidate);
  }

  async function submit() {
    if (!file || loading) return;
    setLoading(true);
    setError(null);
    try {
      const result = await parseResume(file, setProgress);
      notify("Resume parsed successfully.");
      navigate(`/results/${result.id}`);
    } catch (requestError) {
      setError(errorMessage(requestError));
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-5 py-12 sm:px-8 sm:py-16">
      <div className="text-center">
        <p className="eyebrow justify-center"><span className="h-2 w-2 rounded-full bg-moss" /> Resume parser</p>
        <h1 className="mt-4 font-display text-4xl text-ink sm:text-5xl">Bring a resume into focus.</h1>
        <p className="mx-auto mt-4 max-w-xl leading-7 text-slate-600">Upload a PDF or DOCX and we’ll organize the details we can confidently find.</p>
      </div>

      <section className="mt-10 rounded-[2rem] border border-slate-200 bg-white p-5 shadow-card sm:p-8">
        <div
          onDragOver={(event) => { event.preventDefault(); setDragging(true); }}
          onDragLeave={(event) => { event.preventDefault(); setDragging(false); }}
          onDrop={(event) => { event.preventDefault(); setDragging(false); chooseFile(event.dataTransfer.files[0]); }}
          className={`rounded-3xl border-2 border-dashed px-5 py-12 text-center transition-colors sm:py-16 ${
            dragging ? "border-moss bg-mint/70" : "border-slate-200 bg-paper"
          }`}
        >
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-moss shadow-sm"><FileUp size={25} /></div>
          <h2 className="mt-5 text-lg font-semibold text-ink">Drop your resume here</h2>
          <p className="mt-2 text-sm text-slate-500">PDF or DOCX, up to {MAX_UPLOAD_MB} MB</p>
          <input
            ref={input}
            type="file"
            accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            className="sr-only"
            onChange={(event) => { chooseFile(event.target.files?.[0]); event.currentTarget.value = ""; }}
            disabled={loading}
          />
          <button type="button" className="button-secondary mt-6" disabled={loading} onClick={() => input.current?.click()}>Browse files</button>
        </div>

        {file && (
          <div className="mt-5 flex items-center gap-3 rounded-2xl border border-slate-200 px-4 py-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-mint text-moss"><FileText size={19} /></span>
            <div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold text-ink">{file.name}</p><p className="mt-0.5 text-xs text-slate-500">{formatSize(file.size)}</p></div>
            <button type="button" aria-label="Remove selected file" disabled={loading} className="rounded-lg p-2 text-slate-400 hover:bg-rose-50 hover:text-rose-600" onClick={() => { setFile(null); setError(null); }}><Trash2 size={17} /></button>
          </div>
        )}

        {loading && (
          <div className="mt-5" aria-live="polite">
            <div className="mb-2 flex justify-between text-xs font-medium text-slate-500"><span>Uploading resume…</span><span>{progress}%</span></div>
            <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-moss transition-all" style={{ width: `${progress}%` }} /></div>
          </div>
        )}
        {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p>}
        <div className="mt-6 flex flex-col-reverse items-center justify-between gap-4 sm:flex-row">
          <p className="inline-flex items-center gap-2 text-xs text-slate-500"><ShieldCheck size={15} className="text-moss" /> Files are processed in memory, never stored.</p>
          <button type="button" className="button-primary w-full justify-center sm:w-auto" disabled={!file || loading} onClick={submit}>
            {loading ? <><LoaderCircle className="animate-spin" size={17} /> Parsing…</> : <>Parse resume <FileUp size={17} /></>}
          </button>
        </div>
      </section>
      <p className="mx-auto mt-5 max-w-2xl text-center text-xs leading-5 text-slate-400">Resume text is parsed by the configured API. Scanned image-only PDFs require OCR and are not sent to an external service.</p>
    </main>
  );
}
