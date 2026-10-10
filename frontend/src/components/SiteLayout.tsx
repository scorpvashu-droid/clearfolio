import { FileSearch, Github, Menu, X } from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";

const links = [
  { to: "/", label: "Home", end: true },
  { to: "/parser", label: "Resume Parser", end: false },
  { to: "/history", label: "History", end: false },
  { to: "/about", label: "About", end: false },
];

export function SiteLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-paper/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4 sm:px-8">
          <NavLink to="/" className="flex items-center gap-2.5 text-ink" onClick={() => setMenuOpen(false)}>
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-moss text-white"><FileSearch size={20} /></span>
            <span className="text-lg font-semibold tracking-tight">clearfolio<span className="text-moss">.</span></span>
          </NavLink>
          <button className="rounded-lg p-2 text-ink md:hidden" aria-label="Toggle navigation" onClick={() => setMenuOpen(!menuOpen)}>
            {menuOpen ? <X size={21} /> : <Menu size={21} />}
          </button>
          <nav className={`${menuOpen ? "absolute left-0 right-0 top-full flex border-b bg-paper px-5 py-4 shadow-card" : "hidden"} flex-col gap-1 md:static md:flex md:flex-row md:items-center md:gap-2 md:border-0 md:bg-transparent md:p-0 md:shadow-none`}>
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                onClick={() => setMenuOpen(false)}
                className={({ isActive }) => `rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                  isActive ? "bg-mint text-moss" : "text-slate-600 hover:bg-slate-100 hover:text-ink"
                }`}
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <div className="flex-1"><Outlet /></div>
      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-5 py-6 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <p>© {new Date().getFullYear()} Clearfolio. Clear, local-first resume parsing.</p>
          <NavLink to="/about" className="inline-flex items-center gap-2 hover:text-ink"><Github size={15} /> About this project</NavLink>
        </div>
      </footer>
    </div>
  );
}
