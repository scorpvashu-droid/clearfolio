import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { CircleCheck, CircleX, X } from "lucide-react";

type ToastKind = "success" | "error";
type ToastValue = { message: string; kind: ToastKind } | null;
const ToastContext = createContext<(message: string, kind?: ToastKind) => void>(() => undefined);

export function useToast() {
  return useContext(ToastContext);
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toast, setToast] = useState<ToastValue>(null);
  const timer = useRef<number | undefined>(undefined);

  const notify = useCallback((message: string, kind: ToastKind = "success") => {
    setToast({ message, kind });
  }, []);

  useEffect(() => {
    if (!toast) return;
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setToast(null), 4200);
    return () => window.clearTimeout(timer.current);
  }, [toast]);

  return (
    <ToastContext.Provider value={notify}>
      {children}
      {toast && (
        <div
          role="status"
          className={`fixed bottom-5 right-5 z-50 flex max-w-sm items-center gap-3 rounded-2xl px-4 py-3 text-sm text-white shadow-xl ${
            toast.kind === "error" ? "bg-rose-700" : "bg-ink"
          }`}
        >
          {toast.kind === "error" ? <CircleX size={18} /> : <CircleCheck size={18} />}
          <span className="flex-1">{toast.message}</span>
          <button aria-label="Dismiss notification" onClick={() => setToast(null)}>
            <X size={16} />
          </button>
        </div>
      )}
    </ToastContext.Provider>
  );
}
