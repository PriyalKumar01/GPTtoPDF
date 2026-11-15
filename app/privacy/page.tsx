import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { ShieldCheck, Lock, EyeOff, Server, HardDrive, CheckCircle2 } from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export const metadata = {
  title: 'Privacy Policy & Zero Data Storage Guarantee — Qwik Save',
  description:
    'Read Qwik Save privacy policy: 100% zero data retention, no account creation, client-side in-browser processing, and direct download privacy.',
};

export default function PrivacyPage() {
  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-12">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 dark:bg-[#141D2B] border border-emerald-200 dark:border-[#22C55E]/30 rounded-[2px] text-xs font-semibold text-emerald-600 dark:text-[#22C55E]">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Zero Data Storage Guarantee</span>
          </div>
          <h1 className="text-2xl sm:text-4xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Privacy Policy & Data Security
          </h1>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Your privacy is absolute. {APP_CONFIG.brandName} does not require an account, does not store your files or media, and does not track your personal identity.
          </p>
        </div>

        {/* 3 Core Pillars */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 rounded-[3px] space-y-2.5 shadow-xs">
            <div className="w-9 h-9 rounded-[2px] bg-slate-100 dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-800 dark:text-[#E8EDF5]">
              <EyeOff className="w-4 h-4 text-emerald-500" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5]">
              Zero User Accounts
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              We never ask for emails, usernames, phone numbers, or passwords. You can use every tool anonymously without registration.
            </p>
          </div>

          <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 rounded-[3px] space-y-2.5 shadow-xs">
            <div className="w-9 h-9 rounded-[2px] bg-slate-100 dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-800 dark:text-[#E8EDF5]">
              <HardDrive className="w-4 h-4 text-blue-500" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5]">
              No Files or Media Stored
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              We do not save your videos, photos, or PDF documents to any database or storage server. Once downloaded, everything is wiped from active memory.
            </p>
          </div>

          <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 rounded-[3px] space-y-2.5 shadow-xs">
            <div className="w-9 h-9 rounded-[2px] bg-slate-100 dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-800 dark:text-[#E8EDF5]">
              <Lock className="w-4 h-4 text-purple-500" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5]">
              Client-Side Browser Processing
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Our PDF and document manipulation tools (Merge, Compress, Image to PDF) execute 100% inside your local browser without uploading your files.
            </p>
          </div>
        </div>

        {/* Clear Policy Details */}
        <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 sm:p-8 rounded-[3px] shadow-sm space-y-6 text-xs sm:text-sm text-slate-700 dark:text-[#94A3B8] leading-relaxed">
          <div className="space-y-2">
            <h2 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5]">
              1. Information We Never Collect or Retain
            </h2>
            <p>
              When you use {APP_CONFIG.brandName}, your privacy is guaranteed by architecture:
            </p>
            <ul className="space-y-1.5 pl-2 text-xs">
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                <span>No user profiles, email lists, or sign-up forms.</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                <span>No storage of downloaded videos, reels, photos, or MP3 audio tracks.</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                <span>No storage or retention of converted PDF notes, question papers, or uploaded images.</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                <span>No third-party invasive trackers, spyware, or fingerprinting scripts.</span>
              </li>
            </ul>
          </div>

          <div className="space-y-2">
            <h2 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5]">
              2. Social Media Downloader Privacy
            </h2>
            <p>
              When you enter a public Instagram, Facebook, or YouTube URL, our tools facilitate direct streaming to your personal device. We do not maintain any cache, archive, or library of the media you download. Once your download completes, all transmission streams terminate immediately.
            </p>
          </div>

          <div className="space-y-2">
            <h2 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5]">
              3. Client-Side PDF Tools Security
            </h2>
            <p>
              When you merge multiple PDFs, compress file sizes, or convert photos to PDF, the computation is performed natively inside your browser using JavaScript and HTML5 Canvas. Your documents are never sent across the internet to an external server.
            </p>
          </div>

          <div className="space-y-2">
            <h2 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5]">
              4. External Links and Copyright
            </h2>
            <p>
              {APP_CONFIG.brandName} is designed strictly for personal study, offline archiving, and educational use. Users are responsible for complying with the copyright and intellectual property policies of the respective media owners.
            </p>
          </div>

          <div className="space-y-2 pt-2 border-t border-slate-100 dark:border-[#263448]">
            <h2 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5]">
              5. Contact Regarding Privacy
            </h2>
            <p>
              If you have any questions regarding privacy or data handling, you can reach out directly to developer Priyal Kumar via{' '}
              <a
                href={APP_CONFIG.author.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 dark:text-[#3B82F6] hover:underline font-semibold"
              >
                LinkedIn
              </a>
              .
            </p>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
