import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { Award, BriefcaseBusiness, Check, Clipboard, Download, GraduationCap, Languages, LoaderCircle, MapPin, Mail, Phone, Pencil, Sparkles, UserRound, X } from "lucide-react";
import { ContentCard } from "../components/ContentCard";
import { useToast } from "../components/ToastProvider";
import { errorMessage, getResume } from "../services/api";
import type { Education, Experience, ResumeData } from "../types/resume";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string");
}

function isEducationArray(value: unknown): value is Education[] {
  return Array.isArray(value) && value.every((item) => isRecord(item)
    && ["institution", "degree", "field", "dates", "details"].every((key) => item[key] === null || typeof item[key] === "string"));
}

function isExperienceArray(value: unknown): value is Experience[] {
  return Array.isArray(value) && value.every((item) => isRecord(item)
    && ["company", "role", "job_title", "duration", "start_date", "end_date", "description"]
      .every((key) => item[key] === null || typeof item[key] === "string")
    && isStringArray(item.skills_used));
}

function isResumeData(value: unknown): value is ResumeData {
  if (!isRecord(value)) return false;
  return isStringArray(value.skills) && isStringArray(value.technical_skills)
    && isStringArray(value.soft_skills) && isStringArray(value.projects)
    && isStringArray(value.certifications) && isStringArray(value.languages)
    && isEducationArray(value.education) && isExperienceArray(value.experiences)
    && ["name", "email", "phone", "location", "summary"].every((key) => value[key] === null || typeof value[key] === "string")
    && (value.total_experience_years === null || (typeof value.total_experience_years === "number" && Number.isFinite(value.total_experience_years)));
}

function displayValue(value: string | number | null | undefined): string {
  return value === null || value === undefined || value === "" ? "Not available" : String(value);
}

function saveFile(name: string, content: string, type: string) {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function renderEducation(item: Education, index: number) {
  return (
    <div className="border-b border-slate-100 pb-4 last:border-0 last:pb-0" key={`${item.institution}-${index}`}>
      <p className="font-semibold text-ink">{item.degree || item.field || "Education"}</p>
      <p className="mt-1 text-sm text-slate-600">{item.institution || item.details || "Details not available"}</p>
      {item.dates && <p className="mt-1 text-xs text-slate-400">{item.dates}</p>}
    </div>
  );
}

function renderExperience(item: Experience, index: number) {
  return (
    <div className="border-b border-slate-100 pb-5 last:border-0 last:pb-0" key={`${item.company}-${index}`}>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div><p className="font-semibold text-ink">{item.job_title || item.role || "Role not available"}</p><p className="mt-1 text-sm text-slate-600">{item.company || "Company not available"}</p></div>
        <span className="rounded-full bg-paper px-3 py-1 text-xs text-slate-500">{item.duration || "Dates not available"}</span>
      </div>
      {item.description && <p className="mt-3 text-sm leading-6 text-slate-600">{item.description}</p>}
      {item.skills_used.length > 0 && <div className="mt-3 flex flex-wrap gap-2">{item.skills_used.map((skill) => <span key={skill} className="tag">{skill}</span>)}</div>}
    </div>
  );
}

export function ResultsPage() {
  const { id = "" } = useParams();
  const [data, setData] = useState<ResumeData | null>(null);
  const [filename, setFilename] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState("");
  const notify = useToast();

  useEffect(() => {
    let active = true;
    getResume(id)
      .then((record) => { if (active) { setData(record.data); setFilename(record.filename); } })
      .catch((requestError: unknown) => { if (active) setError(errorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  const json = useMemo(() => data ? JSON.stringify(data, null, 2) : "", [data]);
  const unavailable = useMemo(() => !!data && !data.email && !data.phone && !data.location, [data]);

  async function copyJson() {
    if (!data) return;
    try {
      await navigator.clipboard.writeText(JSON.stringify(data, null, 2));
      notify("Parsed JSON copied to clipboard.");
    } catch {
      notify("Clipboard access is unavailable. Use Download JSON instead.", "error");
    }
  }

  function applyEdits() {
    try {
      const parsed: unknown = JSON.parse(draft);
      if (!isResumeData(parsed)) throw new Error("Please keep all expected fields and list values in the JSON.");
      setData(parsed);
      setEditing(false);
      notify("Your edits are ready to export.");
    } catch (editError) {
      notify(editError instanceof Error ? editError.message : "The JSON is invalid.", "error");
    }
  }

  if (loading) return <LoadingState />;
  if (error || !data) return <main className="mx-auto max-w-3xl px-5 py-16"><ErrorState message={error || "Resume not found."} /></main>;

  const sections = [
    { title: "Projects", icon: <BriefcaseBusiness size={18} />, items: data.projects },
    { title: "Certifications", icon: <Award size={18} />, items: data.certifications },
    { title: "Languages", icon: <Languages size={18} />, items: data.languages },
  ];

  return (
    <main className="mx-auto max-w-7xl px-5 py-10 sm:px-8 sm:py-14">
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div><p className="eyebrow">Parsed resume</p><h1 className="mt-3 font-display text-4xl text-ink sm:text-5xl">{data.name || "Candidate profile"}</h1><p className="mt-2 text-sm text-slate-500">{filename} · Review and edit before exporting.</p></div>
        <div className="flex flex-wrap gap-2">
          <button className="button-secondary" onClick={() => { setDraft(json); setEditing(!editing); }}><Pencil size={16} /> {editing ? "Close editor" : "Edit JSON"}</button>
          <button className="button-secondary" onClick={() => void copyJson()}><Clipboard size={16} /> Copy JSON</button>
          <button className="button-primary" onClick={() => saveFile("resume-parsed.json", JSON.stringify(data, null, 2), "application/json")}><Download size={16} /> Download JSON</button>
          <button className="button-secondary" onClick={() => saveFile("resume-report.txt", createReport(data), "text/plain")}><Download size={16} /> Download report</button>
        </div>
      </div>
      {unavailable && <p className="mt-5 rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-800">Contact details were not confidently found. You can add them using Edit JSON.</p>}
      {editing && (
        <section className="mt-6 rounded-3xl border border-moss/20 bg-white p-5 shadow-card sm:p-6">
          <div className="flex items-center justify-between gap-3"><div><h2 className="font-semibold text-ink">Edit extracted data</h2><p className="mt-1 text-sm text-slate-500">Update any fields in JSON, then apply your changes before exporting.</p></div><button aria-label="Close JSON editor" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" onClick={() => setEditing(false)}><X size={18} /></button></div>
          <textarea aria-label="Edit parsed resume JSON" value={draft} onChange={(event) => setDraft(event.target.value)} className="mt-4 min-h-80 w-full rounded-2xl border border-slate-200 bg-slate-950 p-4 font-mono text-xs leading-6 text-emerald-100 outline-none focus:border-moss" spellCheck={false} />
          <div className="mt-4 flex justify-end"><button className="button-primary" onClick={applyEdits}><Check size={16} /> Apply edits</button></div>
        </section>
      )}

      <div className="mt-8 grid gap-5 lg:grid-cols-[.9fr_1.1fr]">
        <div className="space-y-5">
          <ContentCard title="Contact details" icon={<UserRound size={18} />}>
            <div className="space-y-3 text-sm">
              <p className="flex items-center gap-3 text-slate-700"><UserRound size={16} className="text-moss" />{displayValue(data.name)}</p>
              <p className="flex items-center gap-3 text-slate-700"><Mail size={16} className="text-moss" />{displayValue(data.email)}</p>
              <p className="flex items-center gap-3 text-slate-700"><Phone size={16} className="text-moss" />{displayValue(data.phone)}</p>
              <p className="flex items-center gap-3 text-slate-700"><MapPin size={16} className="text-moss" />{displayValue(data.location)}</p>
            </div>
          </ContentCard>
          <ContentCard title="Skills" icon={<Sparkles size={18} />}>
            <SkillGroup title="Technical skills" items={data.technical_skills} />
            <SkillGroup title="Soft skills" items={data.soft_skills} className="mt-5" />
            {data.skills.length === 0 && <EmptyField />}
          </ContentCard>
          <ContentCard title="Education" icon={<GraduationCap size={18} />}>
            {data.education.length ? <div className="space-y-4">{data.education.map(renderEducation)}</div> : <EmptyField />}
          </ContentCard>
        </div>
        <div className="space-y-5">
          <ContentCard title="Professional summary" icon={<UserRound size={18} />}>
            <p className={`text-sm leading-7 ${data.summary ? "text-slate-600" : "text-slate-400"}`}>{data.summary || "No professional summary was identified."}</p>
            <p className="mt-4 text-xs text-slate-400">Total experience: {data.total_experience_years === null ? "Not specified" : `${data.total_experience_years} years`}</p>
          </ContentCard>
          <ContentCard title="Work experience" icon={<BriefcaseBusiness size={18} />}>
            {data.experiences.length ? <div className="space-y-5">{data.experiences.map(renderExperience)}</div> : <EmptyField />}
          </ContentCard>
          {sections.map((section) => (
            <ContentCard title={section.title} icon={section.icon} key={section.title}>
              {section.items.length ? <ul className="space-y-3">{section.items.map((item) => <li key={item} className="flex gap-2 text-sm leading-6 text-slate-600"><span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-moss" />{item}</li>)}</ul> : <EmptyField />}
            </ContentCard>
          ))}
        </div>
      </div>
    </main>
  );
}

function SkillGroup({ title, items, className = "" }: { title: string; items: string[]; className?: string }) {
  return <div className={className}><p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">{title}</p>{items.length ? <div className="flex flex-wrap gap-2">{items.map((item) => <span className="tag" key={item}>{item}</span>)}</div> : <EmptyField />}</div>;
}

function EmptyField() {
  return <p className="rounded-xl bg-paper px-4 py-3 text-sm text-slate-400">No information available.</p>;
}

function LoadingState() {
  return <div className="flex min-h-[55vh] items-center justify-center gap-3 text-slate-500"><LoaderCircle className="animate-spin text-moss" /> Loading resume…</div>;
}

function ErrorState({ message }: { message: string }) {
  return <div role="alert" className="rounded-2xl bg-rose-50 px-5 py-4 text-sm text-rose-800">{message}</div>;
}

function createReport(data: ResumeData) {
  return [
    data.name || "Candidate profile",
    `Email: ${data.email || "Not available"}`,
    `Phone: ${data.phone || "Not available"}`,
    `Location: ${data.location || "Not available"}`,
    "",
    "PROFESSIONAL SUMMARY",
    data.summary || "Not available",
    "",
    "SKILLS",
    ...((data.skills.length ? data.skills : ["Not available"]).map((skill) => `- ${skill}`)),
    "",
    "EXPERIENCE",
    ...(data.experiences.length ? data.experiences.map((job) => `- ${job.job_title || job.role || "Role not available"} | ${job.company || "Company not available"} | ${job.duration || "Dates not available"}${job.description ? `\n  ${job.description}` : ""}`) : ["Not available"]),
    "",
    "EDUCATION",
    ...(data.education.length ? data.education.map((item) => `- ${item.degree || "Degree not available"} | ${item.institution || "Institution not available"} | ${item.dates || "Dates not available"}`) : ["Not available"]),
    "",
    "PROJECTS",
    ...(data.projects.length ? data.projects.map((item) => `- ${item}`) : ["Not available"]),
    "",
    "CERTIFICATIONS",
    ...(data.certifications.length ? data.certifications.map((item) => `- ${item}`) : ["Not available"]),
  ].join("\n");
}
