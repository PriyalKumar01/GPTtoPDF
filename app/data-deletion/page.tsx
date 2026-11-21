import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { Trash2, ShieldCheck, CheckCircle2, Mail, ArrowRight, ExternalLink } from 'lucide-react';
import { FacebookIcon } from '@/components/BrandIcons';
import { APP_CONFIG } from '@/lib/config';

export const metadata = {
  title: 'User Data Deletion Instructions — Qwik Save',
  description:
    'Step-by-step instructions for Facebook (Meta) and Google users to remove and delete their account data associated with Qwik Save.',
};

export default function DataDeletionPage() {
  const lastUpdated = 'November 21, 2026';

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-10">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-rose-50 dark:bg-[#141D2B] border border-rose-200 dark:border-rose-500/30 rounded-full text-xs font-semibold text-rose-600 dark:text-rose-400">
            <Trash2 className="w-3.5 h-3.5" />
            <span>Facebook & Google Compliance</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            User Data Deletion Instructions
          </h1>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            According to the Meta Platform rules for Facebook Login and Google API policies, you have complete ownership of your personal data. Below are instructions on how to delete all your activities and data associated with {APP_CONFIG.brandName}.
          </p>
          <p className="text-[11px] text-slate-500 dark:text-slate-500 font-mono">
            Effective Date: {lastUpdated}
          </p>
        </div>

        {/* Informative Status Card */}
        <div className="bg-emerald-500/10 border border-emerald-500/20 p-5 rounded-2xl flex items-start gap-4">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div className="space-y-1 text-xs sm:text-sm">
            <h3 className="font-bold text-slate-900 dark:text-white">Zero Server Retention Guarantee</h3>
            <p className="text-slate-600 dark:text-slate-400 leading-relaxed">
              {APP_CONFIG.brandName} does not store personal passwords, downloaded media, or payment details on persistent servers. Your OAuth credentials (Name &amp; Email) are kept exclusively on your local device to verify your session. When you remove permissions or clear data, it is permanently erased.
            </p>
          </div>
        </div>

        {/* Step-by-Step Instructions */}
        <div className="bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] p-6 sm:p-10 rounded-2xl shadow-sm space-y-8 text-xs sm:text-sm text-slate-700 dark:text-[#94A3B8] leading-relaxed">
          
          {/* Method 1: Facebook App Settings */}
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-[#1877F2]/10 text-[#1877F2] flex items-center justify-center font-bold">
                <FacebookIcon className="w-4 h-4" />
              </div>
              <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-[#E8EDF5]">
                Method 1: Remove via Facebook Account Settings (Recommended)
              </h2>
            </div>
            <p>
              To remove {APP_CONFIG.brandName} activities and revoke authentication permissions from your Facebook account:
            </p>
            <ol className="space-y-3 pl-2">
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">1</span>
                <span>Log into your Facebook account and navigate to <strong>Settings &amp; Privacy &gt; Settings</strong>.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">2</span>
                <span>Scroll down to <strong>Permissions</strong> and click on <strong>Apps and Websites</strong>.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">3</span>
                <span>Find and select <strong>Qwik Save</strong> from your connected applications list.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">4</span>
                <span>Click the <strong>Remove</strong> button. You may also check the box to delete any remaining posts or media permissions.</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                <span>Once removed, Facebook notifies our systems and all active auth sessions are instantly terminated.</span>
              </li>
            </ol>
          </div>

          <hr className="border-slate-200 dark:border-slate-800" />

          {/* Method 2: In-App Mobile Removal */}
          <div className="space-y-4">
            <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">2</span>
              Method 2: One-Click Data Deletion inside Qwik Save Mobile App
            </h2>
            <p>
              If you have the Qwik Save Android APK installed:
            </p>
            <ol className="space-y-2.5 pl-2">
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">1</span>
                <span>Open the <strong>Qwik Save App</strong>.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">2</span>
                <span>Tap the top-right menu (three dots) &gt; <strong>Account &amp; Settings</strong>.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[11px] font-bold shrink-0 mt-0.5">3</span>
                <span>Tap <strong>&quot;Sign Out &amp; Clear All Local Data&quot;</strong>. All cached profile info and download tokens are wiped instantly.</span>
              </li>
            </ol>
          </div>

          <hr className="border-slate-200 dark:border-slate-800" />

          {/* Method 3: Direct Email Request */}
          <div className="space-y-4">
            <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-sky-500/10 text-sky-500 flex items-center justify-center text-xs">3</span>
              Method 3: Submit a Manual Deletion Request
            </h2>
            <p>
              You can also request manual data verification or deletion by emailing our support team. We will process your request and confirm data erasure within <strong>24 to 48 hours</strong>:
            </p>
            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50 space-y-2">
              <div className="flex items-center gap-2 text-slate-900 dark:text-white font-semibold">
                <Mail className="w-4 h-4 text-sky-500" />
                <span>Email Subject: User Data Deletion Request</span>
              </div>
              <p className="text-xs text-slate-500">
                Send to: <code className="text-sky-500 font-mono">support@qwiksave.vercel.app</code> or <code className="text-sky-500 font-mono">priyalkumar01@gmail.com</code>
              </p>
              <p className="text-xs text-slate-500">
                Include the email address associated with your Facebook or Google profile.
              </p>
            </div>
          </div>

        </div>

        {/* Footer links */}
        <div className="text-center pt-4">
          <div className="inline-flex items-center gap-4 text-xs font-medium text-slate-500">
            <Link href="/privacy" className="hover:text-sky-500 transition-colors">
              Privacy Policy
            </Link>
            <span>•</span>
            <Link href="/terms" className="hover:text-sky-500 transition-colors">
              Terms of Service
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
