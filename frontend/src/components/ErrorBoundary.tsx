import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertTriangle } from "lucide-react";

interface State {
  hasError: boolean;
}

export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Application rendering failed", error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="mx-auto flex min-h-screen max-w-xl flex-col items-center justify-center px-6 text-center">
          <AlertTriangle className="mb-4 text-coral" size={36} />
          <h1 className="font-display text-3xl text-ink">That page hit a snag</h1>
          <p className="mt-3 text-slate-600">Refresh to try again. Your saved parsing history is not affected.</p>
          <button className="button-primary mt-6" onClick={() => window.location.reload()}>Refresh page</button>
        </main>
      );
    }
    return this.props.children;
  }
}
