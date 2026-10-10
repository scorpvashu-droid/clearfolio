export interface Education {
  institution: string | null;
  degree: string | null;
  field: string | null;
  dates: string | null;
  details: string | null;
}

export interface Experience {
  company: string | null;
  role: string | null;
  job_title: string | null;
  duration: string | null;
  start_date: string | null;
  end_date: string | null;
  description: string | null;
  skills_used: string[];
}

export interface ResumeData {
  name: string | null;
  email: string | null;
  phone: string | null;
  location: string | null;
  summary: string | null;
  total_experience_years: number | null;
  skills: string[];
  technical_skills: string[];
  soft_skills: string[];
  education: Education[];
  experiences: Experience[];
  projects: string[];
  certifications: string[];
  languages: string[];
}

export interface ResumeRecord {
  id: string;
  filename: string;
  parsed_at: string;
  data: ResumeData;
}

export interface ResumePage {
  items: ResumeRecord[];
  total: number;
  page: number;
  page_size: number;
}
