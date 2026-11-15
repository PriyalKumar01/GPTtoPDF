'use client';

import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import { APP_CONFIG } from '@/lib/config';
import {
  FileText,
  Images,
  QrCode,
  ArrowRight,
  Wand2,
  Layers,
  Minimize2,
  Music,
  Zap,
} from 'lucide-react';
import { InstagramIcon, FacebookIcon, YouTubeIcon } from '@/components/BrandIcons';

export default function ToolsHubPage() {
  const iconMap: Record<string, React.ElementType> = {
    FileText,
    Instagram: InstagramIcon,
    Facebook: FacebookIcon,
    Images,
    QrCode,
    Video: YouTubeIcon,
    YouTube: YouTubeIcon,
    Layers,
    Minimize2,
    Music,
  };

  // Feature bullets per tool matching Image 3 style
  const toolBullets: Record<string, string[]> = {
    'ai-to-pdf': ['ChatGPT, Claude & Gemini', 'Academic Layout & Diagrams', 'Zero Signup / 100% Free'],
    'instagram-downloader': ['Reels, Videos & Carousels', 'Full HD Original Quality', 'Direct Fast MP4 Download'],
    'facebook-downloader': ['Public Videos & Reels', '1080p Full HD & SD Options', 'Exact File Size Display'],
    'image-to-pdf': ['JPG, PNG, WEBP, GIF Support', 'Reorder & Rotate Pages', 'Private Client-Side Processing'],
    'youtube-downloader': ['Shorts, Long Videos & Movies', 'TikTok (No Watermark)', '1080p, 720p & MP3 Stream'],
    'pdf-merge': ['Combine Multiple PDFs', 'Drag & Drop Page Reordering', 'Zero Data Upload to Server'],
    'pdf-compress': ['Reduce Size by 50-80%', 'Smart Metadata Stripping', 'Preserves Text & Image Clarity'],
    'pdf-to-image': ['Convert PDF to JPG & PNG', 'High-Res 300 DPI Export', 'Instant ZIP or Single Download'],
    'audio-extractor': ['Extract MP3 from Any Video', 'Studio 320kbps Quality', 'Supports MP4, MOV, WebM, MKV'],
    'qr-code-generator': ['Custom URLs, WiFi & WhatsApp', 'Vector SVG & High-Res PNG', 'Instant Real-Time Preview'],
  };

  const tools = APP_CONFIG.tools;

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        {/* Compact Hero Section - Ensures Cards are visible on first screen */}
        <section className="relative overflow-hidden pt-8 pb-7 border-b border-slate-200 dark:border-[#263448] bg-white dark:bg-[#0D1524]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-[2px] text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-3">
              <Zap className="w-3.5 h-3.5" />
              <span>Qwik Save — Save in One Click</span>
            </div>

            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5] mb-2">
              Explore Dashboard — All Productivity Tools
            </h1>

            <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] max-w-2xl mx-auto leading-relaxed">
              High-speed social video downloaders (Instagram, Facebook, YouTube, TikTok), browser-native PDF utilities (Merge, Compress, Image to PDF), and AI chat note generators. 100% Free with zero sign-up required.
            </p>
          </div>
        </section>

        {/* Tools Grid Section - Cards visible immediately */}
        <section className="py-8 sm:py-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Thin Highlighted Boundary Header */}
          <div className="border-b border-slate-200 dark:border-[#263448] pb-4 mb-6 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <span className="w-1.5 h-5 bg-[#1877F2] rounded-[1px] inline-block shadow-xs" />
              <div>
                <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-[#E8EDF5]">
                  Productivity & Downloader Cards
                </h2>
                <p className="text-xs text-slate-500 dark:text-[#94A3B8]">
                  Select any card to launch the direct interactive workspace.
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold px-2.5 py-1 rounded-[2px] bg-slate-100 dark:bg-[#141D2B] text-slate-600 dark:text-[#94A3B8] border border-slate-200 dark:border-[#263448]">
              {tools.length} Available Tools
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {tools.map((tool) => {
              const Icon = iconMap[tool.iconName] || Wand2;
              const bullets = toolBullets[tool.id] || [
                '100% Free & Unlimited',
                'High Performance Execution',
                'Zero Signup / Fast Stream',
              ];

              return (
                <div
                  key={tool.id}
                  className="group relative p-6 rounded-[3px] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:border-emerald-500/50 hover:bg-emerald-50/50 dark:hover:bg-[#122428]/50 shadow-sm hover:shadow-md transition-all duration-200 flex flex-col justify-between hover:scale-[1.01]"
                >
                  <div>
                    {/* Top row: Icon box & Badge */}
                    <div className="flex items-start justify-between mb-4">
                      <div className="w-10 h-10 rounded-[3px] bg-slate-100 dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-800 dark:text-[#E8EDF5] group-hover:bg-emerald-500/10 group-hover:text-emerald-500 group-hover:border-emerald-500/30 transition-colors">
                        <Icon className="w-5 h-5" />
                      </div>

                      {tool.badge && (
                        <span
                          className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-[2px] border ${
                            tool.badgeColor || 'bg-blue-500/10 text-blue-500 border-blue-500/20'
                          }`}
                        >
                          {tool.badge}
                        </span>
                      )}
                    </div>

                    {/* Tool Name */}
                    <h3 className="font-bold text-base text-slate-900 dark:text-[#E8EDF5] group-hover:text-emerald-700 dark:group-hover:text-emerald-300 transition-colors mb-2">
                      {tool.name}
                    </h3>

                    {/* Description */}
                    <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed mb-4">
                      {tool.description}
                    </p>

                    {/* Bullet Points List */}
                    <ul className="space-y-1.5 mb-6 pt-2 border-t border-slate-100 dark:border-[#1E293B]">
                      {bullets.map((b, idx) => (
                        <li key={idx} className="flex items-center gap-2 text-xs text-slate-600 dark:text-[#94A3B8]">
                          <span className="w-1.5 h-1.5 rounded-none bg-emerald-500 shrink-0" />
                          <span>{b}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* Bottom Action Button */}
                  <Link
                    href={tool.href}
                    className="w-full py-2.5 px-4 rounded-[3px] bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1E293B] dark:hover:bg-[#28384E] text-white text-xs font-semibold flex items-center justify-between transition-colors shadow-sm group-hover:shadow group-hover:bg-[#0c1322] dark:group-hover:bg-[#162234]"
                  >
                    <span>OPEN TOOL</span>
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </Link>
                </div>
              );
            })}
          </div>
        </section>

        {/* Quick Metrics Bar - Moved to Bottom of Page as requested */}
        <section className="py-8 bg-white dark:bg-[#0D1524] border-t border-slate-200 dark:border-[#263448]">
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 text-center">
              <div className="p-3 bg-slate-50 dark:bg-[#111A29] rounded-[2px] border border-slate-200 dark:border-[#263448]">
                <p className="text-xl font-black text-slate-900 dark:text-white">100% Free</p>
                <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">No Subscriptions</p>
              </div>
              <div className="p-3 bg-slate-50 dark:bg-[#111A29] rounded-[2px] border border-slate-200 dark:border-[#263448]">
                <p className="text-xl font-black text-slate-900 dark:text-white">4.7 / 5.0 ⭐</p>
                <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">2,000+ Active Users</p>
              </div>
              <div className="p-3 bg-slate-50 dark:bg-[#111A29] rounded-[2px] border border-slate-200 dark:border-[#263448]">
                <p className="text-xl font-black text-slate-900 dark:text-white">5,000+</p>
                <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">College Students</p>
              </div>
              <div className="p-3 bg-slate-50 dark:bg-[#111A29] rounded-[2px] border border-slate-200 dark:border-[#263448]">
                <p className="text-xl font-black text-slate-900 dark:text-white">100% Private</p>
                <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">Client-Side Safe</p>
              </div>
            </div>
          </div>
        </section>

        {/* Partner promo */}
        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
