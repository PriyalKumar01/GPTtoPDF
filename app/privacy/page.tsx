import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { ShieldCheck, Lock, EyeOff, Server, HardDrive, CheckCircle2, UserCheck, Trash2, Mail } from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export const metadata = {
  title: 'Privacy Policy — Qwik Save',
  description:
    'Read the official Privacy Policy for Qwik Save. Learn how we handle OAuth authentication (Google, Facebook, GitHub), protect user data, and uphold zero file storage standards.',
};

export default function PrivacyPage() {
  const lastUpdated = 'November 21, 2026';

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-12">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 dark:bg-[#141D2B] border border-emerald-200 dark:border-[#22C55E]/30 rounded-full text-xs font-semibold text-emerald-600 dark:text-[#22C55E]">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>GDPR, CCPA & Meta Platform Compliant</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Privacy Policy
          </h1>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            At {APP_CONFIG.brandName}, your privacy and digital security are our highest priority. This policy outlines how information is collected, used, and protected across our web application and mobile app.
          </p>
          <p className="text-[11px] text-slate-500 dark:text-slate-500 font-mono">
            Effective Date: {lastUpdated}
          </p>
        </div>

        {/* 3 Core Security Pillars */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 rounded-xl space-y-2.5 shadow-sm">
            <div className="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
              <UserCheck className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5]">
              Minimal Identity Data
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              We only request essential public profile information (Name, Email) through verified OAuth providers (Google, Facebook, GitHub) to secure your account.
            </p>
          </div>

          <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 rounded-xl space-y-2.5 shadow-sm">
            <div className="w-10 h-10 rounded-lg bg-sky-50 dark:bg-sky-500/10 border border-sky-200 dark:border-sky-500/20 flex items-center justify-center text-sky-600 dark:text-sky-400">
              <HardDrive className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5]">
              Zero Media Storage
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              We do not store your downloaded videos, reels, audio files, or generated PDF documents. Media streams directly from source servers to your device.
            </p>
          </div>

          <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 rounded-xl space-y-2.5 shadow-sm">
            <div className="w-10 h-10 rounded-lg bg-indigo-50 dark:bg-indigo-500/10 border border-indigo-200 dark:border-indigo-500/20 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
              <Lock className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5]">
              No Advertising or Selling Data
            </h3>
            <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              We strictly never sell, rent, monetize, or transfer your personal credentials to third-party ad networks, marketing brokers, or data analytics firms.
            </p>
          </div>
        </div>

        {/* Detailed Policy Sections */}
        <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 sm:p-10 rounded-2xl shadow-sm space-y-8 text-xs sm:text-sm text-slate-700 dark:text-[#94A3B8] leading-relaxed">
          
          {/* Section 1 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">1</span>
              Information We Collect
            </h2>
            <p>
              When you interact with {APP_CONFIG.brandName} via web or our official mobile application, we may collect the following limited categories of information:
            </p>
            <ul className="space-y-2 pl-3">
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">OAuth Profile Information:</strong> When you sign in using <strong>Google</strong>, <strong>Facebook</strong>, or <strong>GitHub</strong>, we receive your verified full name, email address, and profile avatar URL provided by that identity platform. We never have access to your passwords.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">Technical Usage Data:</strong> Basic standard diagnostics such as device operating system version, browser user-agent, and anonymized error reports to diagnose system crashes and improve performance.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                <span>
                  <strong className="text-slate-900 dark:text-white">Local Device Preferences:</strong> UI theme settings (dark/light), recent download history cache, and bookmark links stored solely on your local device storage.
                </span>
              </li>
            </ul>
          </div>

          {/* Section 2 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">2</span>
              How We Use Your Information
            </h2>
            <p>
              Any data collected is utilized exclusively for the following specified purposes:
            </p>
            <ul className="space-y-1.5 pl-3">
              <li>• Authenticating your identity and preventing unauthorized bot abuse.</li>
              <li>• Personalizing your user greeting (e.g., &quot;Hello, Name&quot;) and avatar display in the app header.</li>
              <li>• Maintaining active session continuity so you do not need to sign in on every launch.</li>
              <li>• Ensuring compliance with terms of service and preventing abusive automated traffic.</li>
            </ul>
          </div>

          {/* Section 3 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">3</span>
              Third-Party OAuth Services (Google, Facebook & GitHub)
            </h2>
            <p>
              {APP_CONFIG.brandName} integrates with third-party authentication APIs:
            </p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50">
                <p className="font-semibold text-slate-900 dark:text-white">Google OAuth 2.0</p>
                <p className="text-xs text-slate-500 mt-1">
                  Adheres strictly to the Google API Services User Data Policy, including Limited Use requirements.
                </p>
              </div>
              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50">
                <p className="font-semibold text-slate-900 dark:text-white">Meta / Facebook Login</p>
                <p className="text-xs text-slate-500 mt-1">
                  Compliant with Meta Platform Terms. Accesses only public profile and email permissions.
                </p>
              </div>
              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50">
                <p className="font-semibold text-slate-900 dark:text-white">GitHub OAuth</p>
                <p className="text-xs text-slate-500 mt-1">
                  Uses standard read:user scope to verify developer credentials with encrypted token exchange.
                </p>
              </div>
            </div>
          </div>

          {/* Section 4 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">4</span>
              Zero Data Retention on Media & Files
            </h2>
            <p>
              When you download videos, reels, or music via {APP_CONFIG.brandName}:
            </p>
            <ul className="space-y-1.5 pl-3">
              <li>• We act solely as a real-time protocol bridge and media pipeline.</li>
              <li>• No video, audio, or image files are saved or cached permanently on our servers.</li>
              <li>• PDF conversions (Merge, Compress, Image-to-PDF) execute completely client-side in your device memory.</li>
            </ul>
          </div>

          {/* Section 5 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">5</span>
              User Rights & Facebook Data Deletion
            </h2>
            <p>
              You maintain total control over your personal data at all times. Under GDPR, CCPA, and Meta platform rules, you have the right to request full erasure of any authentication records:
            </p>
            <div className="p-4 rounded-xl border border-sky-200 dark:border-sky-900/50 bg-sky-50/50 dark:bg-sky-950/20 space-y-2">
              <div className="flex items-center gap-2 text-sky-600 dark:text-sky-400 font-bold text-sm">
                <Trash2 className="w-4 h-4" />
                <span>How to Delete Your Data</span>
              </div>
              <p className="text-xs leading-relaxed">
                You can revoke {APP_CONFIG.brandName}&apos;s access directly from your Facebook Settings &gt; Apps and Websites, or Google Account Permissions, or visit our dedicated instruction page:
              </p>
              <div className="pt-1">
                <Link
                  href="/data-deletion"
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-sky-600 dark:text-sky-400 hover:underline"
                >
                  View Facebook Data Deletion Instructions &rarr;
                </Link>
              </div>
            </div>
          </div>

          {/* Section 6 */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">6</span>
              Contact Us
            </h2>
            <p>
              If you have any questions or data protection requests concerning this Privacy Policy, please contact our administrative team:
            </p>
            <div className="pt-1 flex items-center gap-2 font-mono text-xs text-slate-800 dark:text-slate-200">
              <Mail className="w-4 h-4 text-sky-500" />
              <span>Email: support@qwiksave.vercel.app / priyalkumar01@gmail.com</span>
            </div>
          </div>

        </div>

        {/* Bottom Navigation Links */}
        <div className="text-center pt-4">
          <div className="inline-flex items-center gap-4 text-xs font-medium text-slate-500">
            <Link href="/terms" className="hover:text-sky-500 transition-colors">
              Terms of Service
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
