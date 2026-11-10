import React from 'react';
import Link from 'next/link';
import { FileText, ArrowUpRight } from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export function Footer() {
  return (
    <footer className="border-t border-slate-200 dark:border-[#263448] bg-white dark:bg-[#090D14] text-xs text-slate-500 dark:text-[#64748B] py-10 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand & Attribution */}
          <div className="space-y-2 md:col-span-1">
            <Link href="/" className="flex items-center gap-2.5 text-slate-900 dark:text-[#E8EDF5]">
              <img
                src="/images/app-logo.jpg"
                alt="AI Chat to PDF Logo"
                className="w-7 h-7 rounded-[3px] object-contain shadow-sm border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#111A29] p-0.5"
              />
              <div className="flex items-center gap-1.5 leading-none">
                <span className="font-bold text-sm tracking-tight text-slate-900 dark:text-[#E8EDF5]">
                  AI Chat to PDF
                </span>
                <span className="text-[9px] font-bold uppercase px-1 py-0.2 rounded-sm bg-red-100 dark:bg-red-950/80 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/60 leading-tight">
                  PDF
                </span>
              </div>
            </Link>
            <p className="text-[11px] text-slate-500 dark:text-[#64748B] leading-relaxed">
              Academic document converter transforming AI study conversations into clean, printable PDFs.
            </p>
            <div className="pt-2 flex flex-col gap-2">
              <a
                href={APP_CONFIG.author.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1 bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] text-xs text-slate-700 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-[#3B82F6] transition-colors"
              >
                <span>Developer: Priyal Kumar</span>
                <ArrowUpRight className="w-3.5 h-3.5" />
              </a>

              <a
                href={APP_CONFIG.partner.url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-2.5 py-1.5 bg-slate-50 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] rounded-[2px] text-[11px] text-slate-600 dark:text-[#94A3B8] hover:border-blue-400 transition-colors"
              >
                <img
                  src="/images/college-study-logo.jpg"
                  alt="College Study"
                  className="w-5 h-5 rounded-sm object-contain"
                />
                <span>Partnered with <strong>College Study</strong></span>
              </a>
            </div>
          </div>

          {/* Navigation */}
          <div>
            <h4 className="font-semibold text-slate-900 dark:text-[#E8EDF5] text-xs uppercase tracking-wider mb-3">
              Application
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link href="/" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  AI Link Converter
                </Link>
              </li>
              <li>
                <Link href="/how-it-works" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  How It Works
                </Link>
              </li>
              <li>
                <Link href="/features" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  Features & Formatting
                </Link>
              </li>
            </ul>
          </div>

          {/* Supported AI */}
          <div>
            <h4 className="font-semibold text-slate-900 dark:text-[#E8EDF5] text-xs uppercase tracking-wider mb-3">
              Supported Platforms
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link href="/supported-platforms" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  ChatGPT Shared Chats
                </Link>
              </li>
              <li>
                <Link href="/supported-platforms" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  Claude AI Discussions
                </Link>
              </li>
              <li>
                <Link href="/supported-platforms" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  Google Gemini Threads
                </Link>
              </li>
              <li>
                <Link href="/supported-platforms" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  Perplexity AI Searches
                </Link>
              </li>
            </ul>
          </div>

          {/* Legal & Privacy */}
          <div>
            <h4 className="font-semibold text-slate-900 dark:text-[#E8EDF5] text-xs uppercase tracking-wider mb-3">
              Security & Info
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link href="/privacy" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <Link href="/faq" className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors">
                  Frequently Asked Questions
                </Link>
              </li>
              <li>
                <a
                  href={APP_CONFIG.author.linkedinUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-blue-600 dark:hover:text-[#E8EDF5] transition-colors flex items-center gap-1"
                >
                  <span>Connect with Author</span>
                  <ArrowUpRight className="w-3 h-3" />
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Legal Disclaimer */}
        <div className="pt-6 border-t border-slate-200 dark:border-[#263448] flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px]">
          <p>
            Your conversations are processed only for PDF conversion. We do not require an account or permanently store your notes.
          </p>
          <p className="text-slate-400 dark:text-[#64748B] shrink-0">
            © {new Date().getFullYear()} {APP_CONFIG.name}. Developed by Priyal Kumar.
          </p>
        </div>
      </div>
    </footer>
  );
}
