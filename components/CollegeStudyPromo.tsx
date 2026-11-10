import React from 'react';
import { ExternalLink, BookOpen, GraduationCap, FileCheck } from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export function CollegeStudyPromo() {
  return (
    <section className="py-12 border-t border-slate-200 dark:border-[#263448] bg-gradient-to-b from-transparent via-blue-50/40 dark:via-[#0D1829]/30 to-transparent">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white dark:bg-[#111A29] border border-blue-200 dark:border-[#1E3A5F] rounded-[3px] p-6 sm:p-9 shadow-sm">
          <div className="flex flex-col lg:flex-row items-center justify-between gap-8">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-left">
              <img
                src="/images/college-study-logo.jpg"
                alt="College Study Logo"
                className="w-20 h-20 sm:w-24 sm:h-24 rounded-[3px] object-contain border border-slate-200 dark:border-[#263448] bg-white p-1 shadow-xs shrink-0"
              />
              <div className="space-y-2">
                <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-[2px] bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] text-[10px] font-bold uppercase tracking-wider text-blue-600 dark:text-[#3B82F6]">
                  Academic Hub Partnership
                </div>
                <h3 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#E8EDF5]">
                  Partnered with College Study – Your Study Hub
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] max-w-2xl leading-relaxed">
                  Looking for semester notes, branch-wise previous year questions (PYQs), and syllabus resources? Access free university study materials directly through our academic partner.
                </p>

                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 pt-1 text-xs text-slate-500 dark:text-[#94A3B8]">
                  <span className="flex items-center gap-1">
                    <BookOpen className="w-3.5 h-3.5 text-blue-500" /> Free Semester Notes
                  </span>
                  <span className="flex items-center gap-1">
                    <FileCheck className="w-3.5 h-3.5 text-emerald-500" /> Solved PYQ Papers
                  </span>
                  <span className="flex items-center gap-1">
                    <GraduationCap className="w-3.5 h-3.5 text-purple-500" /> Engineering Syllabus
                  </span>
                </div>
              </div>
            </div>

            <a
              href={APP_CONFIG.partner.url}
              target="_blank"
              rel="noopener noreferrer"
              className="h-11 px-6 bg-blue-600 hover:bg-blue-700 text-white text-xs sm:text-sm font-semibold rounded-[2px] inline-flex items-center gap-2 whitespace-nowrap shadow-sm transition-colors"
            >
              <span>Visit College Study</span>
              <ExternalLink className="w-4 h-4" />
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
