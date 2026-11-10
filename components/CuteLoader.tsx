'use client';

import React from 'react';
import { Sparkles, FileText, Bot, Layers, ArrowRight } from 'lucide-react';
import { ExtractionProgress } from '@/lib/types';

interface CuteLoaderProps {
  progress: ExtractionProgress;
}

export function CuteLoader({ progress }: CuteLoaderProps) {
  return (
    <div className="mt-6 p-6 sm:p-7 rounded-[4px] bg-gradient-to-b from-blue-50/80 via-white to-sky-50/50 dark:from-[#0B1526] dark:via-[#111A29] dark:to-[#0F172A] border-2 border-blue-200/80 dark:border-[#1E3A5F] shadow-md transition-all duration-300 relative overflow-hidden">
      {/* Decorative background glow */}
      <div className="absolute -top-12 -right-12 w-32 h-32 bg-blue-400/10 dark:bg-blue-500/10 rounded-full blur-2xl pointer-events-none" />
      <div className="absolute -bottom-12 -left-12 w-32 h-32 bg-emerald-400/10 dark:bg-emerald-500/10 rounded-full blur-2xl pointer-events-none" />

      <div className="flex flex-col items-center justify-center text-center space-y-4 relative z-10">
        {/* Cute Animated AI to PDF Morphing Character Box */}
        <div className="relative">
          {/* Pulsing Aura Ring */}
          <div className="absolute inset-0 rounded-2xl bg-gradient-to-tr from-blue-500 via-sky-400 to-emerald-400 opacity-25 blur-md animate-pulse" />

          {/* Center Mascot Container */}
          <div className="relative w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-white dark:bg-[#141D2B] border-2 border-blue-400/40 dark:border-[#38BDF8]/40 shadow-lg flex items-center justify-center">
            {/* Cute bouncing Document Icon with eyes */}
            <div className="flex flex-col items-center justify-center animate-bounce">
              <div className="relative">
                <FileText className="w-8 h-8 sm:w-10 sm:h-10 text-blue-600 dark:text-[#38BDF8]" strokeWidth={2} />
                {/* Cute friendly blink dots */}
                <div className="absolute top-[13px] left-[10px] flex items-center gap-[6px]">
                  <span className="w-1.5 h-1.5 rounded-full bg-slate-900 dark:bg-white animate-ping" />
                  <span className="w-1.5 h-1.5 rounded-full bg-slate-900 dark:bg-white" />
                </div>
              </div>
            </div>

            {/* Orbiting Sparkles */}
            <Sparkles className="w-4 h-4 text-amber-400 absolute -top-1.5 -right-1.5 animate-spin" />
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 absolute -bottom-1 -left-1 animate-pulse" />
          </div>
        </div>

        {/* Status Message & Percent Badge */}
        <div className="space-y-1.5 max-w-md">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-100/80 dark:bg-[#192536] border border-blue-300 dark:border-blue-800 text-[11px] font-bold text-blue-700 dark:text-blue-300">
            <span className="w-2 h-2 rounded-full bg-blue-600 dark:bg-blue-400 animate-ping" />
            <span>Converting Your Notes</span>
            <span className="font-mono px-1.5 py-0.2 bg-white dark:bg-[#0F1623] rounded-sm text-blue-600 dark:text-blue-400 shadow-xs">
              {progress.percent}%
            </span>
          </div>

          <h4 className="text-sm sm:text-base font-bold text-slate-900 dark:text-[#F1F5F9] animate-pulse">
            {progress.message || 'Extracting conversation and rendering diagrams...'}
          </h4>

          <p className="text-[11px] sm:text-xs text-slate-500 dark:text-[#94A3B8]">
            Exam notes typeset ho rahe hain — diagrams, questions, aur code blocks format ho rahe hain...
          </p>
        </div>

        {/* Shimmering Animated Progress Bar */}
        <div className="w-full max-w-md space-y-1.5">
          <div className="w-full h-2.5 bg-slate-200 dark:bg-[#0F1623] rounded-full overflow-hidden p-0.5 border border-slate-300 dark:border-[#22334A]">
            <div
              className="h-full rounded-full bg-gradient-to-r from-blue-500 via-sky-400 to-emerald-400 transition-all duration-300 ease-out shadow-xs"
              style={{ width: `${Math.max(8, progress.percent)}%` }}
            />
          </div>
          <div className="flex items-center justify-between text-[10px] text-slate-400 dark:text-[#64748B] font-mono px-1">
            <span>Reading Share Link</span>
            <span>Parsing Diagrams</span>
            <span>Typesetting PDF</span>
          </div>
        </div>
      </div>
    </div>
  );
}
