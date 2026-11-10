import React from 'react';
import {
  UserX,
  FileCheck2,
  ExternalLink,
  Code2,
  Table2,
  Smartphone,
  Lock,
  Sparkles,
  Bot,
  Edit3,
  Palette,
  Image as ImageIcon,
} from 'lucide-react';

export function Features() {
  const featureList = [
    {
      title: 'Universal AI Link Support',
      description:
        'Supports ChatGPT (o1, 4o, canvas), Claude, Google Gemini, and Perplexity public links, plus direct text paste for DeepSeek & Copilot.',
      icon: Bot,
      tag: 'Any AI',
    },
    {
      title: 'Live Preview & Inline Editing',
      description:
        'Edit user questions or AI answers directly, clean AI pleasantries in one click, remove unwanted filler, or add custom study notes before export.',
      icon: Edit3,
      tag: 'Full Freedom',
    },
    {
      title: 'MS Word Classic Shadow Border',
      description:
        'Dual-layer academic drop-shadow border styling that frames your study notes cleanly with comfortable margin clearance.',
      icon: Palette,
      tag: 'Typesetting',
    },
    {
      title: 'Visual Diagram & Formula Preservation',
      description:
        'Preserves embedded flowcharts, architecture diagrams, and mathematical formulas cleanly without broken tags or missing images.',
      icon: ImageIcon,
      tag: 'Diagrams',
    },
    {
      title: 'Code & Table Formatting',
      description:
        'Syntax-labeled monospaced code containers with language headers and structured comparison tables with auto-width columns.',
      icon: Code2,
      tag: 'Formatting',
    },
    {
      title: 'Permanent Clickable PDF Footer',
      description:
        'Every page features official AI Chat to PDF branding, clickable website links, and Developer: Priyal Kumar attribution.',
      icon: ExternalLink,
      tag: 'Attribution',
    },
    {
      title: 'Zero Login or Subscription',
      description:
        '100% free and frictionless. Never asks for credit cards, email signups, passwords, or personal credentials.',
      icon: Sparkles,
      tag: '100% Free',
    },
    {
      title: 'Client-Side Ephemeral Privacy',
      description:
        'Your PDF is compiled directly on your machine. Conversations are never stored in a database or tracked.',
      icon: Lock,
      tag: 'Private',
    },
    {
      title: 'Heavy Semester Notes Ready',
      description:
        'Handles 60+ page multi-unit question papers, hundred-message study guides, and multi-turn discussions seamlessly.',
      icon: FileCheck2,
      tag: '60+ Pages',
    },
  ];

  return (
    <section id="features" className="py-14 border-t border-slate-200 dark:border-[#263448]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center space-y-2 mb-10">
          <span className="text-xs font-semibold text-blue-600 dark:text-[#3B82F6] uppercase tracking-wider">
            Engineered For Student Revision
          </span>
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-[#E8EDF5]">
            Everything Included in Free Mode
          </h2>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8]">
            Designed specifically for clean academic output, visual diagrams, and instant PDF download.
          </p>
        </div>

        {/* 3 CARDS PER ROW with Professional Green Hover Effect */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {featureList.map((feature, i) => {
            const Icon = feature.icon;
            return (
              <div
                key={i}
                className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-5 sm:p-6 rounded-[2px] space-y-3 hover:border-emerald-500/70 hover:bg-emerald-50/30 dark:hover:bg-emerald-950/20 hover:shadow-md transition-all duration-300 group shadow-xs"
              >
                <div className="flex items-center justify-between">
                  <div className="w-8 h-8 rounded-[2px] bg-slate-100 dark:bg-[#192536] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6] group-hover:text-emerald-600 dark:group-hover:text-emerald-400 group-hover:border-emerald-300 dark:group-hover:border-emerald-800 transition-colors">
                    <Icon className="w-4 h-4" strokeWidth={1.75} />
                  </div>
                  <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded-[2px] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-500 dark:text-[#94A3B8]">
                    {feature.tag}
                  </span>
                </div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5] group-hover:text-emerald-700 dark:group-hover:text-emerald-300 transition-colors">
                  {feature.title}
                </h3>
                <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                  {feature.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
