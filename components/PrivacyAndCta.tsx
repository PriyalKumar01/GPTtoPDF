'use client';

import React from 'react';
import Link from 'next/link';
import { ShieldCheck, ArrowUp, ArrowRight } from 'lucide-react';

export function PrivacyAndCta({ onScrollToTop }: { onScrollToTop: () => void }) {
  return (
    <>
      {/* Privacy Section */}
      <section id="privacy" className="py-14 border-t border-slate-200 dark:border-[#263448]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 dark:bg-[#141D2B] border border-emerald-200 dark:border-[#22C55E]/30 rounded-[2px] text-xs text-emerald-600 dark:text-[#22C55E]">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Strict Privacy Standards</span>
          </div>

          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#E8EDF5]">
            Private, Client-Side, No Storage
          </h2>

          <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] max-w-2xl mx-auto leading-relaxed">
            Your conversations are processed only for PDF conversion. We do not require an account, store personal information, or permanently save your study notes on any database.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-5 pt-4 text-left">
            <div className="p-5 bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] shadow-xs hover:border-emerald-500/70 hover:bg-emerald-50/20 dark:hover:bg-emerald-950/20 transition-all duration-300">
              <h4 className="text-xs sm:text-sm font-bold text-slate-900 dark:text-[#E8EDF5] mb-1.5">
                No Account Tracking
              </h4>
              <p className="text-xs text-slate-500 dark:text-[#64748B] leading-relaxed">
                No user identities, cookies, or telemetry tied to your conversation prompts.
              </p>
            </div>
            <div className="p-5 bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] shadow-xs hover:border-emerald-500/70 hover:bg-emerald-50/20 dark:hover:bg-emerald-950/20 transition-all duration-300">
              <h4 className="text-xs sm:text-sm font-bold text-slate-900 dark:text-[#E8EDF5] mb-1.5">
                Zero Permanent Logs
              </h4>
              <p className="text-xs text-slate-500 dark:text-[#64748B] leading-relaxed">
                Payloads exist only in memory during the conversion lifecycle and are discarded.
              </p>
            </div>
            <div className="p-5 bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] shadow-xs hover:border-emerald-500/70 hover:bg-emerald-50/20 dark:hover:bg-emerald-950/20 transition-all duration-300">
              <h4 className="text-xs sm:text-sm font-bold text-slate-900 dark:text-[#E8EDF5] mb-1.5">
                Safe SSRF Prevention
              </h4>
              <p className="text-xs text-slate-500 dark:text-[#64748B] leading-relaxed">
                Outbound requests are strictly constrained to verified AI share domains.
              </p>
            </div>
          </div>

          <div className="pt-2">
            <Link
              href="/privacy"
              className="text-xs text-blue-600 dark:text-[#3B82F6] hover:underline inline-flex items-center gap-1 font-medium"
            >
              <span>Read the full Privacy Policy</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </section>

      {/* Final Call to Action */}
      <section className="py-12 border-t border-slate-200 dark:border-[#263448] bg-slate-50 dark:bg-[#0F1623]/50">
        <div className="max-w-2xl mx-auto px-4 text-center space-y-4">
          <h2 className="text-xl sm:text-2xl font-semibold text-slate-900 dark:text-[#E8EDF5]">
            Your AI Study Notes, Ready to Download.
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8]">
            Turn any ChatGPT, Claude, or Gemini study discussion into a clean, printable PDF in seconds.
          </p>
          <div className="pt-2">
            <button
              onClick={onScrollToTop}
              className="h-10 px-6 bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs font-medium rounded-[2px] inline-flex items-center gap-2 transition-colors"
            >
              <span>Paste Your Link Now</span>
              <ArrowUp className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </section>
    </>
  );
}
