import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { FileText, ShieldAlert, CheckCircle, Scale, AlertTriangle, Mail } from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export const metadata = {
  title: 'Terms of Service — Qwik Save',
  description:
    'Read the official Terms of Service for Qwik Save. Understand guidelines regarding acceptable use, intellectual property, personal use compliance, and account authentication.',
};

export default function TermsPage() {
  const lastUpdated = 'November 21, 2026';

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-12">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-sky-50 dark:bg-[#141D2B] border border-sky-200 dark:border-sky-500/30 rounded-full text-xs font-semibold text-sky-600 dark:text-sky-400">
            <Scale className="w-3.5 h-3.5" />
            <span>Legal Agreement</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Terms of Service
          </h1>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Please review these Terms carefully before using {APP_CONFIG.brandName}. By accessing our web application or mobile app, you agree to be bound by these provisions.
          </p>
          <p className="text-[11px] text-slate-500 dark:text-slate-500 font-mono">
            Effective Date: {lastUpdated}
          </p>
        </div>

        {/* Highlight Banner */}
        <div className="bg-sky-500/10 border border-sky-500/20 p-5 rounded-2xl flex items-start gap-4">
          <div className="w-10 h-10 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center shrink-0">
            <FileText className="w-5 h-5" />
          </div>
          <div className="space-y-1 text-xs sm:text-sm">
            <h3 className="font-bold text-slate-900 dark:text-white">Summary of Terms</h3>
            <p className="text-slate-600 dark:text-slate-400 leading-relaxed">
              {APP_CONFIG.brandName} is provided for personal, lawful, and educational purposes. You agree to use media downloads strictly for personal offline viewing and to respect the copyright and intellectual property of original content creators.
            </p>
          </div>
        </div>

        {/* Detailed Terms Container */}
        <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 sm:p-10 rounded-2xl shadow-sm space-y-8 text-xs sm:text-sm text-slate-700 dark:text-[#94A3B8] leading-relaxed">
          
          {/* Section 1 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">1</span>
              Acceptance of Agreement
            </h2>
            <p>
              By installing, downloading, browsing, or using {APP_CONFIG.brandName} (including web tools at <code>qwiksave.vercel.app</code> and our Android application), you represent that you are at least 13 years of age and agree to comply with these Terms of Service, along with our <Link href="/privacy" className="text-sky-500 hover:underline">Privacy Policy</Link>. If you do not agree to these terms, you must discontinue use immediately.
            </p>
          </div>

          {/* Section 2 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">2</span>
              Description of Services
            </h2>
            <p>
              {APP_CONFIG.brandName} provides online productivity, educational, and media utilities, including:
            </p>
            <ul className="space-y-1.5 pl-3">
              <li>• Direct video and audio extraction from publicly accessible URLs (YouTube, Facebook, Instagram).</li>
              <li>• In-app background music streaming and offline playback caching.</li>
              <li>• Client-side PDF tools (AI Chat to PDF, document compression, page merge, format conversions).</li>
              <li>• Utility tools such as QR code generation, text scanning, and study notes organizing.</li>
            </ul>
          </div>

          {/* Section 3 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">3</span>
              Intellectual Property & Fair Use Guidelines
            </h2>
            <p>
              We respect copyright and expect all users to do the same:
            </p>
            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/60 space-y-2">
              <ul className="space-y-2 text-xs">
                <li className="flex items-start gap-2">
                  <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                  <span>
                    <strong>Personal, Non-Commercial Use Only:</strong> You may only download and store content for personal archiving, study, or offline playback.
                  </span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                  <span>
                    <strong>No Redistribution:</strong> You agree not to resell, broadcast, re-upload, or commercially exploit any third-party media acquired via {APP_CONFIG.brandName}.
                  </span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                  <span>
                    <strong>Third-Party Trademarks:</strong> YouTube, Facebook, Meta, Instagram, and GitHub are registered trademarks of their respective owners. {APP_CONFIG.brandName} is an independent utility and is not affiliated with or endorsed by these platforms.
                  </span>
                </li>
              </ul>
            </div>
          </div>

          {/* Section 4 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">4</span>
              User Authentication, Truthful Profiles & Zero Tolerance Policy
            </h2>
            <p>
              To safeguard service integrity and protect infrastructure from abusive bots:
            </p>
            <ul className="space-y-1.5 pl-3">
              <li>• You must authenticate using verified OAuth identity providers (Google, Facebook, or GitHub).</li>
              <li>• <strong>Truthful Profile Details:</strong> You agree to provide accurate, non-fictitious personal information (real name and age).</li>
              <li>• <strong>Zero Tolerance for Falsified Accounts:</strong> Providing fraudulent details, disposable/temporary email addresses, or creating bot accounts constitutes a material violation.</li>
              <li>• <strong>Immediate Real-Time Suspension:</strong> We reserve the full right to instantly terminate and permanently ban any account found in violation of these Terms across all web and mobile platforms without prior notice.</li>
            </ul>
          </div>

          {/* Section 5 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">5</span>
              Prohibited Activities
            </h2>
            <p>
              Users are strictly prohibited from engaging in the following actions:
            </p>
            <ul className="space-y-1.5 pl-3 text-xs text-rose-600 dark:text-rose-400">
              <li>• Launching DDoS attacks, scrapers, or automated bots against {APP_CONFIG.brandName} APIs.</li>
              <li>• Attempting to reverse engineer, decompile, or extract source code from protected modules.</li>
              <li>• Utilizing the service to acquire, store, or distribute illegal or harmful materials.</li>
              <li>• Bypassing geo-restrictions or digital rights management (DRM) technologies.</li>
            </ul>
          </div>

          {/* Section 6 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">6</span>
              Disclaimer of Warranties & Limitation of Liability
            </h2>
            <div className="p-4 rounded-xl border border-amber-200 dark:border-amber-900/50 bg-amber-50/50 dark:bg-amber-950/20 text-xs leading-relaxed space-y-2">
              <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-bold">
                <AlertTriangle className="w-4 h-4" />
                <span>&quot;AS IS&quot; and &quot;AS AVAILABLE&quot; Provision</span>
              </div>
              <p>
                {APP_CONFIG.brandName} and its developer team provide all tools on an &quot;AS IS&quot; basis without warranties of any kind. We do not warrant that service will be completely uninterrupted, error-free, or compatible with future third-party platform algorithm revisions. In no event shall {APP_CONFIG.brandName} be liable for any indirect, punitive, or consequential damages.
              </p>
            </div>
          </div>

          {/* Section 7 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">7</span>
              Modifications to Terms
            </h2>
            <p>
              We may revise these Terms of Service periodically to reflect product updates or legal developments. Continued usage of our services after any modification constitutes acceptance of the revised Terms.
            </p>
          </div>

          {/* Section 8 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">8</span>
              Contact Information
            </h2>
            <p>
              For legal inquiries, copyright claims, or questions regarding these Terms:
            </p>
            <div className="pt-1 flex items-center gap-2 font-mono text-xs text-slate-800 dark:text-slate-200">
              <Mail className="w-4 h-4 text-sky-500" />
              <span>Email: legal@qwiksave.vercel.app / priyalkumar01@gmail.com</span>
            </div>
          </div>

        </div>

        {/* Bottom Navigation Links */}
        <div className="text-center pt-4">
          <div className="inline-flex items-center gap-4 text-xs font-medium text-slate-500">
            <Link href="/privacy" className="hover:text-sky-500 transition-colors">
              Privacy Policy
            </Link>
            <span>•</span>
            <Link href="/data-deletion" className="hover:text-sky-500 transition-colors">
              Data Deletion
            </Link>
            <span>•</span>
            <Link href="/" className="hover:text-sky-500 transition-colors">
              Home
            </Link>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
