import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import {
  Link2,
  Sliders,
  Download,
  Share2,
  FileCheck,
  CheckCircle2,
  ArrowRight,
  BookOpen,
} from 'lucide-react';

export const metadata = {
  title: 'How It Works — GPT to PDF',
  description:
    'Complete step-by-step guide to converting ChatGPT, Claude, and Gemini study notes into clean academic PDFs.',
};

export default function HowItWorksPage() {
  const steps = [
    {
      num: '01',
      title: 'Generate a Public Share Link',
      desc: 'Inside your AI chat (ChatGPT, Claude, Google Gemini, or Perplexity), click the "Share" button at the top right of the conversation. Make sure it is set to public/viewable with link, and copy the URL.',
      icon: Share2,
      tip: 'Tip: For ChatGPT, the link format is chatgpt.com/share/[id]. For Claude, it is claude.ai/share/[id].',
    },
    {
      num: '02',
      title: 'Paste Into GPT to PDF',
      desc: 'Paste your copied link into the converter input on our homepage and click "Convert to PDF". Our backend immediately resolves the conversation structure, message order, code blocks, and tables.',
      icon: Link2,
      tip: 'No signup, login, API key, or browser extension required.',
    },
    {
      num: '03',
      title: 'Preview & Customize Typesetting',
      desc: 'Review the extracted notes in the interactive preview. Customize Paper Size (A4 / US Letter), Font Size (Small / Medium / Large), and choose your preferred typography (Helvetica, Times New Roman, or Courier).',
      icon: Sliders,
      tip: 'You can also toggle user prompts, code snippets, dates, and title banners on or off.',
    },
    {
      num: '04',
      title: 'Instant PDF Download with Clickable Footers',
      desc: 'Click "Download PDF". A cleanly typeset academic document generates client-side within seconds. Every page is stamped with page numbers and active clickable hyperlinks to the website and author.',
      icon: Download,
      tip: 'Formatted specifically for exam study, printing, and binder storage.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-12">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] rounded-[2px] text-xs font-semibold text-blue-600 dark:text-[#3B82F6]">
            <BookOpen className="w-3.5 h-3.5" />
            <span>Workflow Guide</span>
          </div>
          <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            How AI Chat to PDF Works
          </h1>
          <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Designed for students and developers who use AI models for exam revisions, lecture summaries, and technical problem solving.
          </p>
        </div>

        {/* Steps Grid: 3 Wide Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {steps.map((s) => {
            const Icon = s.icon;
            return (
              <div
                key={s.num}
                className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-6 rounded-[2px] shadow-xs flex flex-col justify-between gap-4 hover:border-emerald-500/70 hover:bg-emerald-50/20 dark:hover:bg-emerald-950/20 hover:shadow-md transition-all duration-300"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="w-10 h-10 rounded-[2px] bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6] font-mono font-bold text-sm">
                      {s.num}
                    </div>
                    <Icon className="w-5 h-5 text-slate-400" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5]">
                    {s.title}
                  </h3>
                  <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                    {s.desc}
                  </p>
                </div>

                <div className="pt-2 border-t border-slate-100 dark:border-[#263448]/60">
                  <span className="inline-block px-2.5 py-1 bg-slate-50 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-500 dark:text-[#94A3B8] text-xs rounded-[2px]">
                    {s.tip}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Student Exam Advice Box */}
        <div className="bg-slate-50 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] p-6 rounded-[2px] space-y-3">
          <div className="flex items-center gap-2 text-blue-600 dark:text-[#3B82F6] font-semibold text-xs uppercase tracking-wider">
            <FileCheck className="w-4 h-4" />
            <span>Best Practices for Exam Notes</span>
          </div>
          <h3 className="text-sm sm:text-base font-semibold text-slate-900 dark:text-[#E8EDF5]">
            Converting 50+ Page Semester Exam Question Banks & PYQs
          </h3>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            When preparing long multi-unit question papers (e.g. Software Testing, Operating Systems, Machine Learning), prompt ChatGPT or Claude to format comparisons as markdown tables and code as code blocks. Our engine automatically preserves table column widths, syntax containers, and page breaks without cutting paragraphs across page splits.
          </p>
        </div>

        {/* Action CTA */}
        <div className="text-center pt-4">
          <Link
            href="/"
            className="h-10 px-6 bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-medium rounded-[2px] inline-flex items-center gap-2 transition-colors"
          >
            <span>Go to Converter</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </main>

      <Footer />
    </div>
  );
}
