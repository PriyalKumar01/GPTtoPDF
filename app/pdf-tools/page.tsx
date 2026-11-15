'use client';

import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  FileText,
  Layers,
  Minimize2,
  Images,
  ArrowRight,
  Sparkles,
} from 'lucide-react';

export default function PdfToolsHubPage() {
  const tools = [
    {
      title: 'Merge PDF',
      description: 'Combine multiple PDF documents into a single organized file in your custom order.',
      href: '/pdf-tools/merge',
      icon: Layers,
      badge: 'Popular',
    },
    {
      title: 'Compress PDF',
      description: 'Reduce the file size of your PDF documents with custom compression levels.',
      href: '/pdf-tools/compress',
      icon: Minimize2,
      badge: 'Fast',
    },
    {
      title: 'PDF to Image',
      description: 'Convert and extract PDF document pages into high-resolution JPG or PNG pictures.',
      href: '/pdf-tools/pdf-to-image',
      icon: Images,
      badge: 'HD',
    },
    {
      title: 'Image to PDF',
      description: 'Merge multiple JPG, PNG, WEBP, or GIF photos into one clean PDF document.',
      href: '/image-to-pdf',
      icon: Images,
      badge: 'Free',
    },
    {
      title: 'AI Chat to PDF',
      description: 'Convert ChatGPT, Claude, Gemini, and Perplexity study conversations into formatted PDFs.',
      href: '/',
      icon: FileText,
      badge: 'Core',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
              <Sparkles className="w-3.5 h-3.5" />
              <span>PDF & Document Suite</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Free Online PDF Tools
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Merge, compress, convert, and manage your PDF documents directly in the browser with 100% privacy.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {tools.map((t, idx) => {
              const Icon = t.icon;
              return (
                <Link
                  key={idx}
                  href={t.href}
                  className="group p-5 rounded-[2px] bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] hover:border-[#3B82F6] transition shadow-sm flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <div className="w-9 h-9 rounded-[2px] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-700 dark:text-[#E8EDF5] group-hover:text-blue-600 dark:group-hover:text-blue-400 transition">
                        <Icon className="w-4 h-4" />
                      </div>
                      <span className="text-[10px] font-mono font-bold uppercase px-2 py-0.5 rounded-[2px] bg-slate-100 dark:bg-[#111A29] text-slate-600 dark:text-[#94A3B8] border border-slate-200 dark:border-[#263448]">
                        {t.badge}
                      </span>
                    </div>

                    <h3 className="font-bold text-sm text-slate-900 dark:text-[#E8EDF5] group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors mb-1">
                      {t.title}
                    </h3>
                    <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                      {t.description}
                    </p>
                  </div>

                  <div className="flex items-center gap-1 text-xs font-medium text-blue-600 dark:text-[#3B82F6] mt-4 pt-3 border-t border-slate-100 dark:border-[#263448]">
                    <span>Use Tool</span>
                    <ArrowRight className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
                  </div>
                </Link>
              );
            })}
          </div>
        </section>

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
