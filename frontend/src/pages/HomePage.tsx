import { ArrowDownRight, ArrowRight, BriefcaseBusiness, FileCheck2, LockKeyhole, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";

const features = [
  { icon: FileCheck2, title: "Structured at a glance", text: "Bring contact details, skills, experience and education into one clean, readable view." },
  { icon: Sparkles, title: "Thoughtful extraction", text: "A section-aware parser handles common resume layouts without inventing missing information." },
  { icon: LockKeyhole, title: "Local-first by design", text: "Your document is processed by this app and never sent to an external AI service." },
];

export function HomePage() {
  return (
    <main>
      <section className="relative overflow-hidden">
        <div className="absolute -right-32 -top-36 h-[30rem] w-[30rem] rounded-full bg-mint/70 blur-3xl" />
        <div className="relative mx-auto grid max-w-7xl items-center gap-12 px-5 py-16 sm:px-8 sm:py-24 lg:grid-cols-[1.1fr_.9fr] lg:py-28">
          <div className="animate-rise">
            <span className="eyebrow"><span className="h-2 w-2 rounded-full bg-moss" /> A clearer way to read resumes</span>
            <h1 className="mt-7 max-w-2xl font-display text-5xl leading-[1.08] tracking-tight text-ink sm:text-6xl lg:text-7xl">
              Good people deserve to be <span className="italic text-moss">easy to find.</span>
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-slate-600">
              Turn a resume into organized, editable information in seconds. No black box. Just a clearer starting point.
            </p>
            <div className="mt-9 flex flex-wrap items-center gap-4">
              <Link to="/parser" className="button-primary">Start parsing <ArrowRight size={17} /></Link>
              <Link to="/about" className="button-quiet">See how it works <ArrowDownRight size={17} /></Link>
            </div>
            <p className="mt-8 text-xs font-medium uppercase tracking-[.16em] text-slate-400">PDF and DOCX · up to 10 MB · no OCR or external AI</p>
          </div>
          <div className="relative mx-auto w-full max-w-lg animate-rise [animation-delay:120ms]">
            <div className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-card sm:p-7">
              <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                <div><p className="text-xs font-semibold uppercase tracking-widest text-moss">Candidate snapshot</p><p className="mt-1 text-2xl font-semibold text-ink">Alex Morgan</p></div>
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-mint text-moss"><BriefcaseBusiness size={21} /></div>
              </div>
              <div className="mt-5 grid grid-cols-2 gap-3">
                {[["EXPERIENCE", "5 years"], ["LOCATION", "Portland, OR"], ["EDUCATION", "B.S. Computer Science"], ["SKILLS", "React · Python · AWS"]].map(([label, value]) => (
                  <div className="rounded-2xl bg-paper p-4" key={label}><p className="text-[10px] font-bold tracking-[.15em] text-slate-400">{label}</p><p className="mt-2 text-sm font-medium leading-5 text-ink">{value}</p></div>
                ))}
              </div>
              <div className="mt-4 flex items-center gap-2 rounded-2xl bg-moss px-4 py-3 text-sm font-medium text-white">
                <FileCheck2 size={16} /> Parsed into a clear, editable profile
              </div>
            </div>
            <div className="absolute -bottom-6 -left-5 hidden rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm shadow-card sm:block"><span className="mr-2 text-coral">✳</span>Resume details, in focus</div>
          </div>
        </div>
      </section>
      <section className="bg-white py-16 sm:py-20">
        <div className="mx-auto max-w-7xl px-5 sm:px-8">
          <div className="max-w-xl"><p className="eyebrow">Designed for clarity</p><h2 className="mt-4 font-display text-4xl text-ink sm:text-5xl">Less scanning. More understanding.</h2></div>
          <div className="mt-10 grid gap-5 md:grid-cols-3">
            {features.map(({ icon: Icon, title, text }, index) => (
              <article key={title} className="rounded-3xl border border-slate-200 bg-paper p-6 transition-transform duration-300 hover:-translate-y-1">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-moss shadow-sm"><Icon size={21} /></div>
                <p className="mt-6 text-xs font-bold tracking-[.15em] text-coral">0{index + 1}</p>
                <h3 className="mt-2 text-lg font-semibold text-ink">{title}</h3>
                <p className="mt-2 leading-7 text-slate-600">{text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>
      <section className="mx-auto max-w-7xl px-5 py-16 sm:px-8 sm:py-20">
        <div className="flex flex-col justify-between gap-6 rounded-[2rem] bg-ink p-8 text-white sm:flex-row sm:items-center sm:p-12">
          <div><p className="text-sm font-semibold text-emerald-300">Your next clear view</p><h2 className="mt-2 font-display text-3xl sm:text-4xl">Start with one resume.</h2></div>
          <Link to="/parser" className="inline-flex items-center justify-center gap-2 rounded-full bg-white px-6 py-3 font-semibold text-ink transition hover:bg-mint">Choose a file <ArrowRight size={17} /></Link>
        </div>
      </section>
    </main>
  );
}
