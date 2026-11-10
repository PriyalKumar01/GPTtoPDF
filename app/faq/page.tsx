'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { HelpCircle, ChevronDown, ChevronUp, Search, ArrowRight } from 'lucide-react';

export default function FaqPage() {
  const [search, setSearch] = useState('');
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqData = [
    {
      category: 'General',
      q: 'How do I convert a ChatGPT or AI conversation into a PDF?',
      a: 'In your AI tool (ChatGPT, Claude, Gemini, etc.), click the "Share" button to create a public link. Copy that URL, paste it into our converter box on the homepage, and click "Convert to PDF". The PDF is generated in seconds without creating an account.',
    },
    {
      category: 'General',
      q: 'Do I need to sign up, log in, or pay for a subscription?',
      a: 'No. GPT to PDF is completely free and account-free. You do not need to register, verify an email, or install any software.',
    },
    {
      category: 'General',
      q: 'Do I need to enter my ChatGPT password, session cookies, or API key?',
      a: 'Never. You should NEVER share your OpenAI, Google, or Anthropic login credentials with any third-party app. We only read publicly accessible links or text that you explicitly paste.',
    },
    {
      category: 'Platforms',
      q: 'Which AI platforms are supported?',
      a: 'We support ChatGPT (chatgpt.com/share/...), Claude (claude.ai/share/...), Google Gemini (gemini.google.com/share/...), and Perplexity AI. Additionally, our "Direct Text / Markdown" tab works for ANY AI including DeepSeek, Copilot, Llama, and Mistral.',
    },
    {
      category: 'Academic & Notes',
      q: 'Can I convert long exam revision notes (50+ pages)?',
      a: 'Yes! Our PDF engine is tested and optimized for heavy study guides, including 60+ page multi-unit question papers with hundreds of questions, answers, formulas, and diagrams.',
    },
    {
      category: 'Formatting',
      q: 'Will code blocks and comparison tables be preserved?',
      a: 'Yes. Programming code blocks are formatted in monospaced Courier with framed borders and language labels. Markdown comparison tables are converted into clean structured grid tables with bold column headers.',
    },
    {
      category: 'Formatting',
      q: 'How does the clickable PDF footer work?',
      a: 'Every page of your PDF features a discreet, minimal footer: "GPT to PDF ↗" (which clicks and opens the website) and "Made by Priyal" (which clicks and opens Priyal\'s LinkedIn profile), along with clean page numbering (e.g. Page 01 of 12).',
    },
    {
      category: 'Mobile',
      q: 'Can I use this on my Android or iPhone?',
      a: 'Yes. The website is fully responsive. You can paste links from Chrome or Safari on mobile, preview the notes, and download the PDF directly into your phone files or downloads folder.',
    },
    {
      category: 'Troubleshooting',
      q: 'What should I do if OpenAI bot protection blocks an automated fetch?',
      a: 'If OpenAI serves a Cloudflare verification challenge for your IP or link, simply click the "Direct Text / Markdown" tab on the homepage, copy the text from your ChatGPT window, and paste it. Our engine will format it into the exact same academic PDF!',
    },
  ];

  const filtered = faqData.filter(
    (item) =>
      item.q.toLowerCase().includes(search.toLowerCase()) ||
      item.a.toLowerCase().includes(search.toLowerCase()) ||
      item.category.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-10">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] rounded-[2px] text-xs font-semibold text-blue-600 dark:text-[#3B82F6]">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Academic Knowledge Base</span>
          </div>
          <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Frequently Asked Questions
          </h1>
          <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Everything you need to know about converting ChatGPT, Claude, Gemini, and AI study notes into clean academic PDFs.
          </p>

          {/* Search Input */}
          <div className="pt-4 max-w-xl mx-auto relative">
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search questions (e.g. exam notes, code blocks, diagrams, formatting, mobile)..."
              className="w-full h-11 pl-10 pr-4 text-xs sm:text-sm bg-white dark:bg-[#141D2B] border border-slate-300 dark:border-[#263448] rounded-[2px] text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 transition-colors shadow-xs"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          </div>
        </div>

        {/* FAQs 2-Column Wide Grid for Balanced Full-Width Screen Utilization */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {filtered.map((faq, i) => {
            const isOpen = openIndex === i;
            return (
              <div
                key={i}
                className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] overflow-hidden hover:border-emerald-500/60 transition-all duration-200 shadow-xs h-fit"
              >
                <button
                  type="button"
                  onClick={() => setOpenIndex(isOpen ? null : i)}
                  className="w-full p-4 text-left flex items-start justify-between gap-3 hover:bg-slate-50 dark:hover:bg-[#192536]/30 transition-colors"
                >
                  <div className="flex flex-col gap-1.5">
                    <span className="w-fit px-2 py-0.5 bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-[10px] font-mono font-medium text-slate-500 dark:text-[#94A3B8] rounded-[2px]">
                      {faq.category}
                    </span>
                    <span className="text-xs sm:text-sm font-semibold text-slate-900 dark:text-[#E8EDF5]">
                      {faq.q}
                    </span>
                  </div>
                  {isOpen ? (
                    <ChevronUp className="w-4 h-4 text-blue-600 dark:text-[#3B82F6] shrink-0 mt-1" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-slate-400 shrink-0 mt-1" />
                  )}
                </button>

                {isOpen && (
                  <div className="px-4 pb-4 pt-2 text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed border-t border-slate-100 dark:border-[#263448]/60 bg-slate-50/50 dark:bg-[#0B121E]/40">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Action CTA */}
        <div className="text-center pt-4">
          <Link
            href="/"
            className="h-10 px-6 bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-medium rounded-[2px] inline-flex items-center gap-2 transition-colors"
          >
            <span>Start Converting</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </main>

      <Footer />
    </div>
  );
}
