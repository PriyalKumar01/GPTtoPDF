'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { ChevronDown, ChevronUp, ArrowRight } from 'lucide-react';

export function FaqSection() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqs = [
    {
      q: 'How do I convert a ChatGPT conversation into a PDF?',
      a: 'In ChatGPT, click the "Share" button at the top of your chat to generate a public share link. Copy that link, paste it into GPT to PDF, and click "Convert to PDF". Your formatted document will be ready in seconds.',
    },
    {
      q: 'Do I need to sign up or create an account?',
      a: 'No. GPT to PDF is completely account-free and frictionless. You never need to sign up, log in, enter an email, or install browser extensions.',
    },
    {
      q: 'Which other AI platforms can I convert?',
      a: 'In addition to ChatGPT, we support public share links from Claude (claude.ai), Google Gemini (gemini.google.com), and Perplexity AI. You can also paste text from any AI directly using the Direct Text tab.',
    },
    {
      q: 'Can I convert long exam notes with lots of questions and answers?',
      a: 'Yes. Our converter handles multi-turn dialogues, comprehensive lecture revisions, and long topic summaries. The PDF generator automatically manages pagination, page breaks, and margin bounds.',
    },
    {
      q: 'Are code blocks and comparison tables preserved?',
      a: 'Yes. Programming code blocks are typeset with monospaced Courier typography and clean container borders. Markdown comparison tables are parsed into structured grid tables with clear column headers.',
    },
    {
      q: 'Are my notes or conversations stored on your servers?',
      a: 'No. Conversational text is processed ephemerally solely to construct the PDF document structure. We do not maintain a permanent database of user conversations or store student notes.',
    },
  ];

  return (
    <section id="faq" className="py-14 border-t border-slate-200 dark:border-[#263448]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center space-y-2 mb-10">
          <span className="text-xs font-semibold text-blue-600 dark:text-[#3B82F6] uppercase tracking-wider">
            Answers & Guidance
          </span>
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-[#E8EDF5]">
            Frequently Asked Questions
          </h2>
          <p className="max-w-2xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8]">
            Quick answers about converting your study material.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {faqs.map((faq, i) => {
            const isOpen = openIndex === i;
            return (
              <div
                key={i}
                className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] overflow-hidden shadow-xs hover:border-emerald-500/60 transition-all duration-200 h-fit"
              >
                <button
                  type="button"
                  onClick={() => setOpenIndex(isOpen ? null : i)}
                  className="w-full p-4 text-left flex items-start justify-between gap-4 hover:bg-slate-50 dark:hover:bg-[#192536]/30 transition-colors"
                >
                  <span className="text-xs sm:text-sm font-semibold text-slate-900 dark:text-[#E8EDF5]">
                    {faq.q}
                  </span>
                  {isOpen ? (
                    <ChevronUp className="w-4 h-4 text-blue-600 dark:text-[#3B82F6] shrink-0 mt-0.5" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                  )}
                </button>

                {isOpen && (
                  <div className="px-4 pb-4 pt-1 text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed border-t border-slate-100 dark:border-[#263448]/50 bg-slate-50/50 dark:bg-[#0B121E]/30">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="text-center pt-6">
          <Link
            href="/faq"
            className="text-xs text-blue-600 dark:text-[#3B82F6] hover:underline inline-flex items-center gap-1 font-medium"
          >
            <span>View all questions in the Knowledge Base</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </section>
  );
}
