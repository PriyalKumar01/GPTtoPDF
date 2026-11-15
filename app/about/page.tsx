'use client';

import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import { APP_CONFIG } from '@/lib/config';
import { LinkedInIcon, InstagramIcon, FacebookIcon, YouTubeIcon } from '@/components/BrandIcons';
import {
  Check,
  X,
  Star,
  Users,
  ShieldCheck,
  Zap,
  ArrowRight,
  ArrowUpRight,
  Globe,
  Mail,
  GraduationCap,
  MapPin,
  Sparkles,
  Heart,
  FileText,
  Download,
  Images,
  Layers,
  Minimize2,
  Music,
} from 'lucide-react';

export default function AboutPage() {
  const comparisonData = [
    {
      feature: 'Unified All-In-One Workspace (No Scattered Sites)',
      qwikSave: true,
      competitors: false,
      detail: 'Everything from video downloaders to PDF tools and AI notes in one clean platform instead of searching dozens of scattered websites.',
    },
    {
      feature: 'No Account or Sign-Up Required',
      qwikSave: true,
      competitors: false,
      detail: 'Instant 1-click access without sharing personal email or passwords.',
    },
    {
      feature: 'Exact File Size (MB) Shown Before Download',
      qwikSave: true,
      competitors: false,
      detail: 'See file sizes (e.g. 1.20 MB, 3.58 MB, 6.72 MB) before initiating download.',
    },
    {
      feature: 'Direct 1080p Full HD & Original Quality Streams',
      qwikSave: true,
      competitors: false,
      detail: 'Zero watermarks and original progressive audio+video MP4 encoding.',
    },
    {
      feature: 'Client-Side PDF Processing & Zero Server Retention',
      qwikSave: true,
      competitors: false,
      detail: 'Your documents, exam notes, and photos never leave your device.',
    },
    {
      feature: 'Complete All-in-One Utility Suite',
      qwikSave: true,
      competitors: false,
      detail: 'AI Chat to PDF + Social Media + PDF Tools + Audio Extractor in one tab.',
    },
    {
      feature: 'Enterprise Minimalist SaaS Design',
      qwikSave: true,
      competitors: false,
      detail: 'Clean, distraction-free interface built to modern developer standards.',
    },
    {
      feature: 'Completely Free Unlimited Usage',
      qwikSave: true,
      competitors: false,
      detail: 'No daily download caps or surprise premium paywalls.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        {/* Hero Section */}
        <section className="relative overflow-hidden pt-12 pb-16 border-b border-slate-200 dark:border-[#263448] bg-white dark:bg-[#0D1524]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
              {/* Left Column: Heading and Mission */}
              <div className="space-y-5">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-[2px] text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>About {APP_CONFIG.name}</span>
                </div>

                <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5] leading-tight">
                  Save in{' '}
                  <span className="text-[#1877F2]">One Click</span>.
                </h1>

                <p className="text-sm sm:text-base text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                  <strong>Qwik Save</strong> is an enterprise-grade digital suite engineered to solve the everyday frustrations students, researchers, and creators face. From converting AI study sessions into formatted academic PDFs to downloading public Instagram, Facebook, and YouTube videos, all under one unified, high-speed dashboard.
                </p>

                {/* Metrics Badges */}
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2">
                  <div className="p-3 bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] rounded-[2px]">
                    <div className="flex items-center gap-1.5 text-amber-500 mb-1">
                      <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                      <span className="font-bold text-sm text-slate-900 dark:text-white">4.7 / 5.0</span>
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8]">
                      Rated by 2,000+ users
                    </p>
                  </div>

                  <div className="p-3 bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] rounded-[2px]">
                    <div className="flex items-center gap-1.5 text-blue-500 mb-1">
                      <Users className="w-4 h-4 text-blue-500" />
                      <span className="font-bold text-sm text-slate-900 dark:text-white">5,000+</span>
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8]">
                      College students empowered
                    </p>
                  </div>

                  <div className="p-3 bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] rounded-[2px] col-span-2 sm:col-span-1">
                    <div className="flex items-center gap-1.5 text-emerald-500 mb-1">
                      <ShieldCheck className="w-4 h-4 text-emerald-500" />
                      <span className="font-bold text-sm text-slate-900 dark:text-white">100% Free</span>
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8]">
                      All-In-One • Zero Tracking
                    </p>
                  </div>
                </div>

                <div className="pt-2 flex flex-wrap gap-3">
                  <Link
                    href="/tools"
                    className="h-10 px-5 rounded-[2px] bg-[#1877F2] hover:bg-[#0866FF] text-white font-medium text-xs sm:text-sm flex items-center gap-2 transition shadow-sm"
                  >
                    <span>Explore All Features</span>
                    <ArrowRight className="w-4 h-4" />
                  </Link>

                  <a
                    href={APP_CONFIG.author.linkedinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="h-10 px-4 rounded-[2px] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] text-slate-800 dark:text-[#E8EDF5] hover:text-blue-600 dark:hover:text-[#3B82F6] font-medium text-xs sm:text-sm flex items-center gap-1.5 transition"
                  >
                    <span>Developer LinkedIn</span>
                    <ArrowUpRight className="w-3.5 h-3.5" />
                  </a>
                </div>
              </div>

              {/* Right Column: AI Generated Hero Illustration */}
              <div className="relative">
                <div className="rounded-[3px] overflow-hidden border border-slate-200 dark:border-[#263448] shadow-xl bg-white dark:bg-[#111A29] p-2">
                  <img
                    src="/images/about-hero-illustration.jpg"
                    alt="University students and professionals collaborating with Qwik Save tools"
                    className="w-full h-auto object-cover rounded-[2px]"
                  />
                  <div className="p-3 text-center border-t border-slate-100 dark:border-[#1E293B] mt-2">
                    <p className="text-[11px] font-medium text-slate-500 dark:text-[#94A3B8]">
                      Engineered for academic success, lightning-fast digital media extraction, and 100% private document management.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Feature Comparison Table (Tick vs Cross) */}
        <section className="py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto mb-12">
            <span className="text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-[2px] bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
              Why We Are Different
            </span>
            <h2 className="text-2xl sm:text-4xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5] mt-2 mb-3">
              Qwik Save vs. Traditional Downloaders
            </h2>
            <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8]">
              Most online converters and downloaders bombard you with deceptive popups, malware redirects, and slow speeds. See how Qwik Save sets a new standard.
            </p>
          </div>

          <div className="border border-slate-200 dark:border-[#263448] rounded-[2px] bg-white dark:bg-[#111A29] overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs sm:text-sm">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-[#263448] bg-slate-50 dark:bg-[#0D1524]">
                    <th className="py-3.5 px-4 font-bold text-slate-900 dark:text-[#E8EDF5] w-1/2">
                      Platform Capability
                    </th>
                    <th className="py-3.5 px-4 font-bold text-[#1877F2] text-center w-1/4 bg-blue-50/50 dark:bg-[#14233A]/40 border-x border-slate-200 dark:border-[#263448]">
                      <div className="flex items-center justify-center gap-1.5">
                        <Sparkles className="w-3.5 h-3.5 text-blue-500" />
                        <span>Qwik Save</span>
                      </div>
                    </th>
                    <th className="py-3.5 px-4 font-bold text-slate-500 dark:text-slate-400 text-center w-1/4">
                      Other Downloaders & Tools
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-[#1E293B]">
                  {comparisonData.map((row, idx) => (
                    <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-[#141D2B]/50 transition-colors">
                      <td className="py-3 px-4">
                        <p className="font-semibold text-slate-900 dark:text-[#E8EDF5]">
                          {row.feature}
                        </p>
                        <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">
                          {row.detail}
                        </p>
                      </td>
                      <td className="py-3 px-4 text-center bg-blue-50/30 dark:bg-[#14233A]/20 border-x border-slate-200 dark:border-[#263448]">
                        <div className="inline-flex items-center justify-center w-6 h-6 rounded-[2px] bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
                          <Check className="w-4 h-4" strokeWidth={2.5} />
                        </div>
                      </td>
                      <td className="py-3 px-4 text-center">
                        <div className="inline-flex items-center justify-center w-6 h-6 rounded-[2px] bg-red-500/10 text-red-500 border border-red-500/20">
                          <X className="w-4 h-4" strokeWidth={2.5} />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>

        {/* Master Directory & Direct Action Table with Row Hover Highlighting */}
        <section className="py-14 bg-white dark:bg-[#0D1524] border-t border-slate-200 dark:border-[#263448]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="mb-6">
              <span className="text-[10px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-[2px] bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                Unified Ecosystem
              </span>
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#E8EDF5] mt-1.5">
                Complete Feature Directory & Access Table
              </h2>
              <p className="text-xs text-slate-500 dark:text-[#94A3B8]">
                Quick reference guide of all capabilities, supported inputs, and direct access links. Hover over any row to highlight details.
              </p>
            </div>

            <div className="overflow-x-auto border border-slate-200 dark:border-[#263448] rounded-[3px] bg-slate-50 dark:bg-[#111A29]">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-100 dark:bg-[#141D2B] text-slate-700 dark:text-[#94A3B8] uppercase text-[10px] tracking-wider font-bold border-b border-slate-200 dark:border-[#263448]">
                  <tr>
                    <th className="py-3 px-4">Feature Name</th>
                    <th className="py-3 px-4">Category</th>
                    <th className="py-3 px-4">Supported Inputs / URLs</th>
                    <th className="py-3 px-4">Output / Formats</th>
                    <th className="py-3 px-4">Privacy & Security</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-[#1E293B] text-slate-700 dark:text-[#94A3B8]">
                  <tr className="hover:bg-blue-50/70 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                      <FileText className="w-4 h-4 text-red-500 shrink-0" />
                      <span>AI Chat to PDF</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-red-500/10 text-red-600 dark:text-red-400 font-semibold text-[10px]">AI Notes</span></td>
                    <td className="py-3.5 px-4">ChatGPT, Claude, Gemini, Perplexity share links</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">Academic PDF with Mermaid & KaTeX</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">100% Client-Side Render</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-pink-50/60 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-pink-600 dark:group-hover:text-pink-400 transition-colors">
                      <InstagramIcon className="w-4 h-4 text-pink-500 shrink-0" />
                      <span>Instagram Downloader</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-pink-500/10 text-pink-600 dark:text-pink-400 font-semibold text-[10px]">Social</span></td>
                    <td className="py-3.5 px-4">instagram.com/reel, /p/, /stories/</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">Original HD MP4 & JPG Cover</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">Zero Logged Data</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/instagram" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-blue-50/70 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                      <FacebookIcon className="w-4 h-4 text-blue-500 shrink-0" />
                      <span>Facebook Downloader</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-blue-500/10 text-blue-600 dark:text-blue-400 font-semibold text-[10px]">Social</span></td>
                    <td className="py-3.5 px-4">facebook.com/reel, /watch, /videos/</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">1080p Full HD & SD with MB Size</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">Safe Stream Extraction</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/facebook" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-red-50/60 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-red-600 dark:group-hover:text-red-400 transition-colors">
                      <YouTubeIcon className="w-4 h-4 text-red-600 shrink-0" />
                      <span>YouTube & Universal</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-red-500/10 text-red-600 dark:text-red-400 font-semibold text-[10px]">Social</span></td>
                    <td className="py-3.5 px-4">YouTube Shorts, Long Videos, TikTok (No Watermark), X</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">1080p, 720p HD MP4 & MP3</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">Direct Proxy Stream</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/youtube" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-emerald-50/60 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">
                      <Images className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Image to PDF</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-semibold text-[10px]">PDF Suite</span></td>
                    <td className="py-3.5 px-4">JPG, JPEG, PNG, WEBP, GIF, BMP</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">Consolidated A4 / Letter PDF</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">100% In-Browser (No Upload)</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/image-to-pdf" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-blue-50/70 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                      <Layers className="w-4 h-4 text-blue-500 shrink-0" />
                      <span>Merge PDF</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-blue-500/10 text-blue-600 dark:text-blue-400 font-semibold text-[10px]">PDF Suite</span></td>
                    <td className="py-3.5 px-4">2+ PDF files (Unlimited pages)</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">Merged Unified PDF</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">100% Client-Side Private</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/pdf-tools/merge" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-emerald-50/60 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">
                      <Minimize2 className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Compress PDF</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-semibold text-[10px]">PDF Suite</span></td>
                    <td className="py-3.5 px-4">Heavy PDFs, scanned documents</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">Optimized Reduced PDF</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">Lossless Metadata Pruning</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/pdf-tools/compress" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>

                  <tr className="hover:bg-indigo-50/60 dark:hover:bg-[#1E293B] transition-colors cursor-pointer group">
                    <td className="py-3.5 px-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                      <Music className="w-4 h-4 text-indigo-500 shrink-0" />
                      <span>Video to Audio MP3</span>
                    </td>
                    <td className="py-3.5 px-4"><span className="px-2 py-0.5 rounded-[2px] bg-indigo-500/10 text-indigo-500 font-semibold text-[10px]">Utility</span></td>
                    <td className="py-3.5 px-4">Instagram, Facebook, YouTube URLs or local video files</td>
                    <td className="py-3.5 px-4 font-mono text-[11px]">Crystal Clear MP3 (320kbps)</td>
                    <td className="py-3.5 px-4 text-emerald-600 dark:text-emerald-400 font-medium">Lossless Audio Extraction</td>
                    <td className="py-3.5 px-4 text-right">
                      <Link href="/audio-extractor" className="inline-flex items-center gap-1 px-3 py-1 bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1877F2] dark:hover:bg-blue-600 text-white rounded-[2px] font-semibold text-xs transition">
                        <span>Use This</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        {/* Developer & Founder Section (Priyal Kumar - Modelled directly after Image 4) */}
        <section className="py-16 border-t border-slate-200 dark:border-[#263448] bg-slate-50 dark:bg-[#090D14]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-white dark:bg-[#111A29] rounded-[3px] border border-slate-200 dark:border-[#263448] p-6 sm:p-10 shadow-md">
              <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-center">
                {/* Developer Photo */}
                <div className="md:col-span-4 flex justify-center">
                  <div className="relative group block rounded-[4px] p-3 bg-gradient-to-b from-blue-100/80 via-slate-50 to-slate-100 dark:from-blue-950/40 dark:via-[#111A29] dark:to-[#0D1524] border border-slate-200 dark:border-[#263448] shadow-sm hover:shadow-lg transition-all duration-300">
                    <div className="overflow-hidden rounded-[3px] bg-gradient-to-t from-slate-200/50 to-transparent dark:from-slate-800/30 flex items-end justify-center pt-4">
                      <img
                        src="/images/priyal-kumar.png"
                        alt="Priyal Kumar - Founder & Lead Developer"
                        className="w-56 sm:w-64 h-auto object-contain max-h-80 drop-shadow-md group-hover:scale-[1.02] transition-transform duration-300"
                      />
                    </div>
                    <div className="mt-2.5 text-center">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400 inline-flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                        Active Founder & Creator
                      </span>
                    </div>
                  </div>
                </div>

                {/* Developer Bio & Achievements */}
                <div className="md:col-span-8 space-y-4">
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
                      FOUNDER & LEAD DEVELOPER
                    </span>
                    <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-[#E8EDF5]">
                      Priyal Kumar
                    </h3>
                    <div className="flex items-center gap-2 text-xs font-medium text-slate-500 dark:text-[#94A3B8]">
                      <GraduationCap className="w-4 h-4 text-blue-500" />
                      <span>CSE&apos;27 @ Harcourt Butler Technical University (HBTU Kanpur)</span>
                    </div>
                  </div>

                  <div className="space-y-3 text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                    <p>
                      <strong>College Study</strong> was founded by <strong>Priyal Kumar</strong> with a vision to simplify access to academic resources for college students. Having experienced firsthand the challenges students face while searching for notes, previous year papers, practical files, and study materials scattered across multiple platforms, he decided to build a centralized solution that makes learning resources accessible, organized, and reliable.
                    </p>
                    <p>
                      Under his leadership, <strong>College Study</strong> has grown into a student-driven academic platform trusted by thousands of learners (<strong>5,000+ active students</strong>).
                    </p>
                    <p>
                      His goal is to leverage technology to create practical solutions that help students save time, learn efficiently, and achieve academic success. Having seen how learners and creators struggled searching across dozens of separate, confusing websites for video downloads, PDF conversions, and notes formatting, <strong>Qwik Save</strong> was created to bring every essential tool together in one organized, lightning-fast suite in a single click.
                    </p>
                  </div>

                  {/* Connect With Me Links */}
                  <div className="pt-2 border-t border-slate-100 dark:border-[#263448]">
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2.5">
                      CONNECT WITH PRIYAL KUMAR
                    </p>
                    <div className="flex flex-wrap items-center gap-2.5">
                      <a
                        href={APP_CONFIG.author.portfolioUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-[2px] text-xs font-semibold text-white bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1E293B] dark:hover:bg-[#334155] border border-slate-700 transition shadow-xs"
                      >
                        <Globe className="w-3.5 h-3.5 text-blue-400" />
                        <span>Portfolio Website</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </a>

                      <a
                        href={APP_CONFIG.author.linkedinUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-[2px] text-xs font-semibold text-white bg-[#0077B5] hover:bg-[#005E93] transition shadow-xs"
                      >
                        <LinkedInIcon className="w-3.5 h-3.5" />
                        <span>LinkedIn</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </a>

                      <a
                        href={APP_CONFIG.partner.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-[2px] text-xs font-medium text-slate-700 dark:text-[#E8EDF5] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] hover:border-blue-500 transition"
                      >
                        <GraduationCap className="w-3.5 h-3.5 text-emerald-500" />
                        <span>College Study (5k+ Students)</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </a>

                      <div className="inline-flex items-center gap-1 px-3 py-1.5 text-xs text-slate-500 dark:text-[#94A3B8]">
                        <MapPin className="w-3.5 h-3.5 text-rose-500" />
                        <span>Kanpur, Uttar Pradesh, India</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Partner Promo */}
        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
