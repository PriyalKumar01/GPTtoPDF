'use client';

import React from 'react';
import { Sparkles, FileText, CheckCircle2, Cpu, Layers, ArrowUpRight } from 'lucide-react';
import { ExtractionProgress } from '@/lib/types';

interface ProfessionalLoaderProps {
  progress: ExtractionProgress;
}

export function CuteLoader({ progress }: ProfessionalLoaderProps) {
  const percent = Math.min(100, Math.max(0, progress.percent || 12));

  // Determine active stage based on percentage
  const stages = [
    {
      id: 1,
      title: 'Validating Link',
      desc: 'Authenticating AI conversation thread',
      isDone: percent >= 30,
      isActive: percent < 30,
    },
    {
      id: 2,
      title: 'Synthesizing Content',
      desc: 'Parsing Markdown, Code & LaTeX Math',
      isDone: percent >= 75,
      isActive: percent >= 30 && percent < 75,
    },
    {
      id: 3,
      title: 'Typesetting PDF',
      desc: 'Compiling academic print-ready layout',
      isDone: percent >= 100,
      isActive: percent >= 75 && percent < 100,
    },
  ];

  return (
    <div className="mt-6 p-6 sm:p-8 rounded-xl bg-white/90 dark:bg-[#0A101D]/90 border border-slate-200/90 dark:border-[#1E2E48] shadow-xl backdrop-blur-xl transition-all duration-300 relative overflow-hidden">
      {/* Subtle ambient lighting effects */}
      <div className="absolute top-0 right-1/4 w-72 h-44 bg-blue-500/10 dark:bg-blue-600/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-72 h-44 bg-indigo-500/10 dark:bg-indigo-600/15 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col items-center justify-center text-center space-y-5 relative z-10">
        {/* Modern Glowing AI Synthesis Hub */}
        <div className="relative flex items-center justify-center">
          {/* Outer rotating pulse aura */}
          <div className="absolute -inset-2 rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-500 to-cyan-400 opacity-20 blur-md animate-pulse" />

          {/* Central high-tech card */}
          <div className="relative w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-slate-50 dark:bg-[#101A2C] border border-blue-400/30 dark:border-[#2A4368] shadow-inner flex items-center justify-center">
            <div className="relative flex items-center justify-center">
              <FileText className="w-8 h-8 sm:w-9 sm:h-9 text-blue-600 dark:text-[#38BDF8] transition-transform duration-300 scale-105" strokeWidth={1.75} />
              <Sparkles className="w-4 h-4 text-amber-500 absolute -top-1 -right-2 animate-bounce" />
            </div>
            {/* Spinning micro scan indicator */}
            <div className="absolute inset-0 rounded-2xl border-2 border-transparent border-t-blue-500/60 dark:border-t-cyan-400/80 animate-spin pointer-events-none" />
          </div>
        </div>

        {/* Dynamic Status Headline & Real-time Percentage */}
        <div className="space-y-2 max-w-lg">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 dark:bg-[#14233A] border border-blue-200/80 dark:border-blue-900/60 text-xs font-semibold text-blue-700 dark:text-blue-300">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-600 dark:bg-blue-400" />
            </span>
            <span>Document Synthesis Engine</span>
            <span className="font-mono px-2 py-0.5 bg-white dark:bg-[#0B1424] rounded-md text-blue-600 dark:text-cyan-400 border border-blue-100 dark:border-blue-900 font-bold shadow-xs">
              {percent}%
            </span>
          </div>

          <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-[#F8FAFC]">
            {progress.message || 'Extracting conversation and generating print-ready PDF...'}
          </h3>

          <p className="text-xs text-slate-500 dark:text-[#94A3B8] max-w-md mx-auto">
            Formatting headings, syntax-highlighted code blocks, KaTeX mathematical formulas, and tables into an academic vector document.
          </p>
        </div>

        {/* Premium Linear Progress Bar with Shimmer */}
        <div className="w-full max-w-md space-y-3">
          <div className="w-full h-2.5 bg-slate-100 dark:bg-[#131F33] rounded-full overflow-hidden p-0.5 border border-slate-200 dark:border-[#223552] shadow-inner relative">
            <div
              className="h-full rounded-full bg-gradient-to-r from-blue-600 via-indigo-500 to-cyan-400 transition-all duration-300 ease-out shadow-sm relative overflow-hidden"
              style={{ width: `${Math.max(6, percent)}%` }}
            >
              <div className="absolute inset-0 bg-white/20 -skew-x-12 animate-[shimmer_2s_infinite]" />
            </div>
          </div>

          {/* Step Timeline Pills */}
          <div className="grid grid-cols-3 gap-2 pt-1 text-left">
            {stages.map((stage) => (
              <div
                key={stage.id}
                className={`p-2 rounded-lg border text-[11px] transition-colors ${
                  stage.isDone
                    ? 'bg-emerald-50/70 dark:bg-emerald-950/20 border-emerald-300/80 dark:border-emerald-800/40 text-emerald-800 dark:text-emerald-300'
                    : stage.isActive
                    ? 'bg-blue-50/70 dark:bg-blue-950/30 border-blue-300 dark:border-blue-700/60 text-blue-800 dark:text-blue-200'
                    : 'bg-slate-50/50 dark:bg-[#0E1726]/40 border-slate-200/60 dark:border-slate-800/60 text-slate-400 dark:text-slate-500'
                }`}
              >
                <div className="flex items-center gap-1.5 font-bold mb-0.5">
                  {stage.isDone ? (
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0" />
                  ) : stage.isActive ? (
                    <div className="w-3.5 h-3.5 border-2 border-blue-600 dark:border-cyan-400 border-t-transparent rounded-full animate-spin shrink-0" />
                  ) : (
                    <div className="w-3.5 h-3.5 rounded-full border border-slate-300 dark:border-slate-700 shrink-0" />
                  )}
                  <span className="truncate">{stage.title}</span>
                </div>
                <p className="text-[10px] opacity-75 truncate hidden sm:block">{stage.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
