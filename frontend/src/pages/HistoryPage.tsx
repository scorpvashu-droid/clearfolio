import { useCallback, useEffect, useState } from "react";
import { ArrowLeft, ArrowRight, Clock3, FileText, LoaderCircle, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { errorMessage, deleteResume, getHistory } from "../services/api";
import { useToast } from "../components/ToastProvider";
import type { ResumePage } from "../types/resume";

export function HistoryPage() {
  const [result, setResult] = useState<ResumePage | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const notify = useToast();

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setResult(await getHistory(page));
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { void load(); }, [load]);

  async function remove(id: string) {
    if (!window.confirm("Delete this parsing history record?")) return;
    try {
      await deleteResume(id);
      notify("History record deleted.");
      if (result?.items.length === 1 && page > 1) setPage(page - 1);
      else await load();
    } catch (requestError) {
      notify(errorMessage(requestError), "error");
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-5 py-12 sm:px-8 sm:py-16">
      <div><p className="eyebrow"><Clock3 size={14} /> Your workspace</p><h1 className="mt-4 font-display text-4xl text-ink sm:text-5xl">Parsing history</h1><p className="mt-3 text-slate-600">Open a saved profile or remove a record from your local history.</p></div>
      <section className="mt-8 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-card">
        {loading ? <div className="flex justify-center py-20 text-moss"><LoaderCircle className="animate-spin" /></div>
          : error ? <div role="alert" className="m-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-800">{error}</div>
            : result?.items.length ? <div className="divide-y divide-slate-100">
              {result.items.map((record) => (
                <div key={record.id} className="flex items-center gap-4 px-5 py-5 sm:px-7">
                  <span className="hidden h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-mint text-moss sm:flex"><FileText size={20} /></span>
                  <div className="min-w-0 flex-1"><p className="truncate font-semibold text-ink">{record.data.name || "Candidate profile"}</p><p className="mt-1 truncate text-sm text-slate-500">{record.filename} · {new Date(record.parsed_at).toLocaleString()}</p></div>
                  <div className="flex items-center gap-1">
                    <Link aria-label={`Open ${record.filename}`} className="button-quiet px-3 py-2" to={`/results/${record.id}`}><span className="hidden sm:inline">Open</span><ArrowRight size={16} /></Link>
                    <button aria-label={`Delete ${record.filename}`} className="rounded-full p-2.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600" onClick={() => void remove(record.id)}><Trash2 size={17} /></button>
                  </div>
                </div>
              ))}
            </div> : <div className="px-6 py-16 text-center"><div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-mint text-moss"><FileText size={22} /></div><h2 className="mt-4 font-semibold text-ink">No resumes parsed yet</h2><p className="mt-2 text-sm text-slate-500">Your recent profiles will appear here.</p><Link to="/parser" className="button-primary mt-5">Parse a resume <ArrowRight size={16} /></Link></div>}
        {result && result.total > result.page_size && (
          <div className="flex items-center justify-between border-t border-slate-100 px-5 py-4 sm:px-7">
            <p className="text-xs text-slate-500">Page {result.page} of {Math.ceil(result.total / result.page_size)} · {result.total} records</p>
            <div className="flex gap-2"><button className="button-secondary px-3 py-2" disabled={page === 1 || loading} onClick={() => setPage(page - 1)}><ArrowLeft size={15} /> Previous</button><button className="button-secondary px-3 py-2" disabled={page >= Math.ceil(result.total / result.page_size) || loading} onClick={() => setPage(page + 1)}>Next <ArrowRight size={15} /></button></div>
          </div>
        )}
      </section>
    </main>
  );
}
