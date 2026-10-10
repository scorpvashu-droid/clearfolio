import { FileSearch, LockKeyhole, ScanText, ShieldCheck } from "lucide-react";

const steps = [
  { icon: FileSearch, title: "Upload a document", copy: "Choose a PDF or DOCX file under 10 MB. The app validates the type and size before parsing." },
  { icon: ScanText, title: "Review the extraction", copy: "A local parser reads document text and looks for common resume sections. Uncertain details are left blank." },
  { icon: ShieldCheck, title: "Keep or remove history", copy: "Structured results and timestamps are saved in a local SQLite database. Uploaded files are not saved." },
];

export function AboutPage() {
  return (
    <main className="mx-auto max-w-5xl px-5 py-12 sm:px-8 sm:py-16">
      <p className="eyebrow"><FileSearch size={14} /> About Clearfolio</p>
      <h1 className="mt-4 max-w-3xl font-display text-4xl leading-tight text-ink sm:text-6xl">A useful first pass, not a black box.</h1>
      <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">Clearfolio turns resume text into a structured profile that you can review, edit and export. It is designed to help organize information—not make decisions about people.</p>
      <div className="mt-10 grid gap-4 md:grid-cols-3">
        {steps.map(({ icon: Icon, title, copy }, index) => <section key={title} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-card"><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-mint text-moss"><Icon size={20} /></span><p className="mt-6 text-xs font-bold tracking-widest text-coral">STEP 0{index + 1}</p><h2 className="mt-2 font-semibold text-ink">{title}</h2><p className="mt-2 text-sm leading-6 text-slate-600">{copy}</p></section>)}
      </div>
      <section className="mt-5 rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
        <div className="flex items-start gap-4"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-mint text-moss"><LockKeyhole size={19} /></span><div><h2 className="font-semibold text-ink">What happens to your resume?</h2><p className="mt-2 text-sm leading-7 text-slate-600">The backend extracts text in memory and stores only the parsed result, source filename and timestamp in SQLite. It does not retain the uploaded document or send resume data to external AI services. Delete history records from the History page when they are no longer needed. This project does not claim regulatory compliance.</p></div></div>
      </section>
    </main>
  );
}
