import React from 'react';
import { Link2, Eye, Download } from 'lucide-react';

export function HowItWorks() {
  const steps = [
    {
      step: '01',
      title: 'Paste Public AI Share Link',
      description:
        'Copy your public shared chat link from ChatGPT, Claude, Gemini, or Perplexity and paste it into the input. No login or signup required.',
      icon: Link2,
    },
    {
      step: '02',
      title: 'Preview & Customize Typesetting',
      description:
        'Review the extracted study notes, exam question papers, tables, and code blocks. Customize paper size (A4 / Letter) and typography styling.',
      icon: Eye,
    },
    {
      step: '03',
      title: 'Download Formatted PDF',
      description:
        'Click Download. An academic-typeset PDF with clean margins and clickable footers downloads directly to your device within seconds.',
      icon: Download,
    },
  ];

  return (
    <section id="how-it-works" className="py-14 border-t border-slate-200 dark:border-[#263448]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center space-y-2 mb-10">
          <span className="text-xs font-semibold text-blue-600 dark:text-[#3B82F6] uppercase tracking-wider">
            Simple 3-Step Process
          </span>
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-[#E8EDF5]">
            How It Works
          </h2>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8]">
            Three straightforward steps to convert exam notes from AI chats into print-ready documents.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {steps.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.step}
                className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-6 rounded-[2px] space-y-3.5 relative group hover:border-emerald-500/70 hover:bg-emerald-50/20 dark:hover:bg-emerald-950/20 hover:shadow-md transition-all duration-300 shadow-xs"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-bold text-slate-400 dark:text-[#64748B]">
                    {item.step}
                  </span>
                  <div className="w-8 h-8 rounded-[2px] bg-slate-100 dark:bg-[#192536] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6]">
                    <Icon className="w-4 h-4" strokeWidth={1.75} />
                  </div>
                </div>

                <h3 className="text-sm font-semibold text-slate-900 dark:text-[#E8EDF5]">
                  {item.title}
                </h3>

                <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                  {item.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
