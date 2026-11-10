import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import {
  Sparkles,
  ArrowRight,
  ExternalLink,
  BookOpen,
  GraduationCap,
  Download,
  Share2,
} from 'lucide-react';
import { APP_CONFIG } from '@/lib/config';

export const metadata = {
  title: 'Supported AI Platforms — AI Chat to PDF',
  description:
    'Convert public shared links from ChatGPT, Claude, Google Gemini, Perplexity, DeepSeek, and more into clean printable academic PDFs.',
};

export default function SupportedPlatformsPage() {
  const platforms = [
    {
      name: 'ChatGPT',
      company: 'OpenAI',
      urlSnippet: 'chatgpt.com/share/...',
      badge: 'GPT-4o, o1, Canvas, GPT-3.5',
      accent: 'emerald',
      // Official OpenAI Rosette SVG
      svg: (
        <svg viewBox="0 0 24 24" className="w-12 h-12 text-[#10A37F]" fill="currentColor">
          <path d="M22.2819 9.8211a5.9847 5.9847 0 0 0-.5157-4.9108 6.0462 6.0462 0 0 0-6.5098-2.9A6.0651 6.0651 0 0 0 4.9807 4.1818a5.9847 5.9847 0 0 0-3.9977 2.9 6.0462 6.0462 0 0 0 .7427 7.0966 5.98 5.98 0 0 0 .511 4.9107 6.051 6.051 0 0 0 6.5146 2.9001A5.9847 5.9847 0 0 0 13.259 24a6.0557 6.0557 0 0 0 5.7718-4.2058 5.9894 5.9894 0 0 0 3.9977-2.9001 6.0557 6.0557 0 0 0-.7466-7.0729zm-9.022 12.6081a4.4755 4.4755 0 0 1-2.8764-1.0408l.1419-.0804 4.7783-2.7582a.7948.7948 0 0 0 .3927-.6813v-6.7369l2.02 1.1683a.071.071 0 0 1 .038.052v5.5826a4.5045 4.5045 0 0 1-4.4945 4.4947zm-9.6607-4.1254a4.4708 4.4708 0 0 1-.5346-3.0137l.142.0852 4.783 2.7582a.7712.7712 0 0 0 .7806 0l5.8428-3.3685v2.3324a.0804.0804 0 0 1-.0332.0615L9.74 19.9502a4.4992 4.4992 0 0 1-6.1408-1.6464zM2.3408 7.8956a4.485 4.485 0 0 1 2.3655-1.9728V11.6a.7664.7664 0 0 0 .3879.6765l5.8144 3.3543-2.0201 1.1683a.0757.0757 0 0 1-.071 0l-4.8303-2.7866A4.504 4.504 0 0 1 2.3408 7.8956zm16.0993 3.8558L12.5973 8.3829l2.02-1.1635a.0804.0804 0 0 1 .071 0l4.8303 2.7913a4.4944 4.4944 0 0 1-.6765 8.1042v-5.6772a.79.79 0 0 0-.402-.6863zm2.0107-3.0231l-.142-.0852-4.7735-2.7818a.7759.7759 0 0 0-.7854 0L9.409 9.2297V6.8974a.0662.0662 0 0 1 .0284-.0615l4.8303-2.7866a4.4992 4.4992 0 0 1 6.6802 4.66zM8.3065 12.863l-2.02-1.1635a.0804.0804 0 0 1-.038-.0568V6.06a4.4992 4.4992 0 0 1 7.3757-3.4537l-.142.0805L8.704 5.445a.7948.7948 0 0 0-.3927.6813l-.0048 6.7367zm1.1413-2.0744l2.5524-1.4728 2.5523 1.4728v2.9456l-2.5523 1.4728-2.5524-1.4728z" />
        </svg>
      ),
    },
    {
      name: 'Claude',
      company: 'Anthropic',
      urlSnippet: 'claude.ai/share/...',
      badge: 'Claude 3.5 Sonnet, Opus, Haiku',
      accent: 'amber',
      // Official Anthropic Starburst SVG
      svg: (
        <svg viewBox="0 0 24 24" className="w-12 h-12 text-[#CC785C]" fill="currentColor">
          <path d="M17.39 2H6.61L1 12l5.61 10h10.78L23 12 17.39 2zm-1.89 16.5H8.5L4.5 12l4-6.5h7l4 6.5-4 6.5z" />
          <polygon points="12,5.5 14,9.5 18.5,10 15,13.5 16,18 12,15.5 8,18 9,13.5 5.5,10 10,9.5" />
        </svg>
      ),
    },
    {
      name: 'Google Gemini',
      company: 'Google',
      urlSnippet: 'gemini.google.com/share/...',
      badge: 'Gemini 1.5 Pro, Flash',
      accent: 'blue',
      // Official Gemini Sparkle 4-Point Star SVG
      svg: (
        <svg viewBox="0 0 24 24" className="w-12 h-12" fill="none">
          <path
            d="M12 2C12 7.52285 7.52285 12 2 12C7.52285 12 12 16.4771 12 22C12 16.4771 16.4771 12 22 12C16.4771 12 12 7.52285 12 2Z"
            fill="url(#gemini-grad)"
          />
          <defs>
            <linearGradient id="gemini-grad" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse">
              <stop stopColor="#1E88E5" />
              <stop offset="0.5" stopColor="#7E57C2" />
              <stop offset="1" stopColor="#E91E63" />
            </linearGradient>
          </defs>
        </svg>
      ),
    },
    {
      name: 'Perplexity AI',
      company: 'Perplexity',
      urlSnippet: 'perplexity.ai/search/...',
      badge: 'Pro Search, Research Threads',
      accent: 'teal',
      // Official Perplexity Geometric Asterisk SVG
      svg: (
        <svg viewBox="0 0 24 24" className="w-12 h-12 text-[#22B8CD]" fill="currentColor">
          <path d="M12 2L9 7H15L12 2ZM4 10L9 11L7 15L3 13L4 10ZM20 10L21 13L17 15L15 11L20 10ZM7 18L10 16L12 21L8 20L7 18ZM17 18L16 20L12 21L14 16L17 18Z" />
          <circle cx="12" cy="13" r="2.5" />
        </svg>
      ),
    },
    {
      name: 'DeepSeek',
      company: 'DeepSeek AI',
      urlSnippet: 'Direct Paste / Markdown',
      badge: 'DeepSeek R1, V3, Coder',
      accent: 'indigo',
      // Official DeepSeek Whale/Fin SVG
      svg: (
        <svg viewBox="0 0 24 24" className="w-12 h-12 text-[#3B82F6]" fill="currentColor">
          <path d="M12 3C6.48 3 2 7.48 2 13c0 3.7 2.01 6.94 5 8.65v-2.19C4.59 17.9 3.5 15.6 3.5 13c0-4.69 3.81-8.5 8.5-8.5s8.5 3.81 8.5 8.5c0 2.6-1.09 4.9-3 6.46v2.19c2.99-1.71 5-4.95 5-8.65 0-5.52-4.48-10-10-10zm-1 5v4l3.5 2.1.75-1.23-2.75-1.62V8H11z" />
        </svg>
      ),
    },
    {
      name: 'Universal / Any AI',
      company: 'Copilot, Mistral, Llama',
      urlSnippet: 'Direct Text / Markdown Tab',
      badge: 'Any exported chat or transcript',
      accent: 'purple',
      // Universal Sparkles SVG
      svg: (
        <svg viewBox="0 0 24 24" className="w-12 h-12 text-[#8B5CF6]" fill="currentColor">
          <path d="M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z" />
        </svg>
      ),
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-16">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] rounded-[2px] text-xs font-semibold text-blue-600 dark:text-[#3B82F6]">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Universal AI Support</span>
          </div>
          <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Supported AI Platforms
          </h1>
          <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Convert any public share link from top AI assistants directly into clean, printable academic PDFs in seconds.
          </p>
        </div>

        {/* Big Official Logos Grid: 3 CARDS PER ROW with Green Hover Effect */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {platforms.map((p, i) => (
            <div
              key={i}
              className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] p-6 sm:p-7 shadow-xs space-y-4 hover:border-emerald-500/70 hover:bg-emerald-50/30 dark:hover:bg-emerald-950/20 hover:shadow-md transition-all duration-300 group"
            >
              <div className="flex items-center justify-between">
                {/* Big Original Brand Logo */}
                <div className="w-16 h-16 rounded-[4px] bg-slate-50 dark:bg-[#0B121E] border border-slate-200 dark:border-[#263448] p-2 flex items-center justify-center shadow-xs group-hover:scale-105 transition-transform">
                  {p.svg}
                </div>
                <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded-[2px] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-500 dark:text-[#94A3B8]">
                  {p.company}
                </span>
              </div>

              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-[#E8EDF5] group-hover:text-emerald-700 dark:group-hover:text-emerald-300 transition-colors">
                  {p.name}
                </h3>
                <span className="text-[11px] font-mono text-blue-600 dark:text-[#38BDF8] block mt-0.5">
                  {p.urlSnippet}
                </span>
                <span className="text-[10px] text-slate-500 dark:text-[#64748B] block mt-0.5">
                  {p.badge}
                </span>
              </div>

              {/* Direct Simple Rule: No lengthy steps, just share link & download! */}
              <div className="pt-2 border-t border-slate-100 dark:border-[#263448]/60">
                <p className="text-xs text-slate-700 dark:text-[#CBD5E1] font-medium leading-relaxed">
                  Bas chat ki <strong className="text-slate-900 dark:text-white">public share link</strong> ko copy karke yahan paste karo — aur clean printable PDF turant download ho jayegi!
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* Quick Convert Button */}
        <div className="text-center">
          <Link
            href="/"
            className="h-11 px-8 bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-semibold rounded-[2px] inline-flex items-center gap-2 transition-colors shadow-sm"
          >
            <span>Paste Your Link Now</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        {/* PARTNERED WITH COLLEGE STUDY (Promotion Section) */}
        <section className="bg-gradient-to-r from-blue-50 via-white to-sky-50 dark:from-[#0B1526] dark:via-[#111A29] dark:to-[#0F172A] border-2 border-blue-200 dark:border-[#1E3A5F] rounded-[3px] p-6 sm:p-10 shadow-sm">
          <div className="flex flex-col md:flex-row items-center justify-between gap-8">
            <div className="flex items-center gap-5">
              <img
                src="/images/college-study-logo.jpg"
                alt="College Study Logo"
                className="w-20 h-20 rounded-[4px] object-contain border border-blue-200 dark:border-[#263448] bg-white p-1 shadow-sm shrink-0"
              />
              <div className="space-y-1.5 text-left">
                <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-[2px] bg-blue-100 dark:bg-blue-950/80 border border-blue-300 dark:border-blue-800 text-[10px] font-bold uppercase tracking-wider text-blue-700 dark:text-blue-300">
                  Official Academic Partner
                </div>
                <h3 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#E8EDF5]">
                  Partnered with College Study – Your Study Hub
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] max-w-2xl leading-relaxed">
                  Apne semester exams ki poori taiyari karo! Get free access to university semester notes, branch-wise previous year question papers (PYQs), engineering syllabi, and curated study materials.
                </p>
              </div>
            </div>

            <a
              href={APP_CONFIG.partner.url}
              target="_blank"
              rel="noopener noreferrer"
              className="h-11 px-6 bg-blue-600 hover:bg-blue-700 text-white text-xs sm:text-sm font-semibold rounded-[2px] inline-flex items-center gap-2 whitespace-nowrap shadow-sm transition-colors"
            >
              <span>Explore College Study Hub</span>
              <ExternalLink className="w-4 h-4" />
            </a>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
