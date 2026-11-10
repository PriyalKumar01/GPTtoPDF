import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { ShieldCheck, Lock, EyeOff, Server, ArrowRight } from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export const metadata = {
  title: 'Privacy Policy & Data Security — GPT to PDF',
  description:
    'Learn how GPT to PDF protects student privacy with zero account creation, ephemeral memory processing, and client-side PDF generation.',
};

export default function PrivacyPage() {
  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-12">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 dark:bg-[#141D2B] border border-emerald-200 dark:border-[#22C55E]/30 rounded-[2px] text-xs font-semibold text-emerald-600 dark:text-[#22C55E]">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Privacy Architecture</span>
          </div>
          <h1 className="text-2xl sm:text-4xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Privacy & Security Policy
          </h1>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Your conversations are processed only for PDF conversion. We do not require an account or permanently store your notes.
          </p>
        </div>

        {/* Core Principles */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-5 rounded-[2px] space-y-2">
            <div className="w-8 h-8 rounded-[2px] bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6]">
              <EyeOff className="w-4 h-4" />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 dark:text-[#E8EDF5]">
              No Identity Tracking
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              We never collect emails, phone numbers, or passwords. There are zero accounts or tracking cookies on our platform.
            </p>
          </div>

          <div className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-5 rounded-[2px] space-y-2">
            <div className="w-8 h-8 rounded-[2px] bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6]">
              <Server className="w-4 h-4" />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 dark:text-[#E8EDF5]">
              Ephemeral Processing
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Conversations exist only in memory during the conversion lifecycle and are destroyed immediately upon response.
            </p>
          </div>

          <div className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-5 rounded-[2px] space-y-2">
            <div className="w-8 h-8 rounded-[2px] bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6]">
              <Lock className="w-4 h-4" />
            </div>
            <h3 className="text-sm font-semibold text-slate-900 dark:text-[#E8EDF5]">
              Anti-SSRF Hardened
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Backend requests are strictly validated against verified OpenAI, Anthropic, and Google domains, blocking internal addresses.
            </p>
          </div>
        </div>

        {/* Detailed Sections */}
        <div className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-6 rounded-[2px] shadow-sm space-y-6 text-xs sm:text-sm text-slate-700 dark:text-[#94A3B8] leading-relaxed">
          <div className="space-y-2">
            <h2 className="text-base font-semibold text-slate-900 dark:text-[#E8EDF5]">
              1. Information We Never Collect
            </h2>
            <p>
              When you use {APP_CONFIG.name}, you do not register an account. We do not store:
            </p>
            <ul className="list-disc list-inside space-y-1 pl-2 text-xs">
              <li>Your name, email address, or billing info.</li>
              <li>Your OpenAI, Google, or Anthropic account credentials or API keys.</li>
              <li>Your conversation history or uploaded question papers.</li>
            </ul>
          </div>

          <div className="space-y-2">
            <h2 className="text-base font-semibold text-slate-900 dark:text-[#E8EDF5]">
              2. How Public Links Are Fetched
            </h2>
            <p>
              When you paste a public share link, our server fetches the publicly exposed conversation payload. The response is parsed to generate a structured JSON document representing the user and assistant turns. This payload is transmitted directly back to your browser, which constructs and saves the PDF.
            </p>
          </div>

          <div className="space-y-2">
            <h2 className="text-base font-semibold text-slate-900 dark:text-[#E8EDF5]">
              3. Client-Side Rendering Guarantee
            </h2>
            <p>
              The actual PDF file creation (font embedding, table construction, and page numbering) happens directly inside your web browser using Vector PDF libraries. The final PDF is never uploaded to any cloud bucket or server.
            </p>
          </div>
        </div>

        {/* Action CTA */}
        <div className="text-center pt-2">
          <Link
            href="/"
            className="h-10 px-6 bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-medium rounded-[2px] inline-flex items-center gap-2 transition-colors"
          >
            <span>Back to Converter</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </main>

      <Footer />
    </div>
  );
}
