import React from 'react';
import Link from 'next/link';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import {
  FileText,
  ExternalLink,
  Code2,
  Table2,
  Lock,
  Layers,
  Smartphone,
  Sparkles,
  ArrowRight,
  Check,
  X,
  Edit3,
  Palette,
  Image as ImageIcon,
  Bot,
  Zap,
} from 'lucide-react';

export const metadata = {
  title: 'Features & Capabilities — AI Chat to PDF',
  description:
    'Explore comprehensive academic formatting features: universal multi-AI support, live document preview editing, classic shadow borders, diagram preservation, and zero-login PDF export.',
};

export default function FeaturesPage() {
  const capabilities = [
    {
      title: 'Universal Any-AI Link Support',
      desc: 'Convert public shared links from ChatGPT, Claude, Google Gemini, and Perplexity into clean documents. Also paste from DeepSeek, Copilot, or raw text directly!',
      icon: Bot,
      tag: 'Multi-Platform',
    },
    {
      title: 'Live Document Preview & Inline Editing',
      desc: 'Full freedom to edit user questions or assistant responses before PDF export. Clean AI pleasantries in one click, remove unwanted sections, or add custom study notes.',
      icon: Edit3,
      tag: 'Customizer',
    },
    {
      title: 'MS Word Classic Shadow Border',
      desc: 'Dual-line academic drop-shadow border styling just like professional MS Word research papers, complete with safe margin clearance above footers.',
      icon: Palette,
      tag: 'Typesetting',
    },
    {
      title: 'Embedded Diagrams & Visual Preservation',
      desc: 'Accurately embeds visual flowcharts, system architecture diagrams, state diagrams, and testing models directly from the chat without broken HTML or missing tags.',
      icon: ImageIcon,
      tag: 'Diagrams',
    },
    {
      title: 'Academic Typography & Font Scaling',
      desc: 'Switch between Modern Sans (Inter), Classic Academic Serif (Times), and Technical Monospace (Courier) with Compact, Standard, or Large sizing for revision.',
      icon: FileText,
      tag: 'Styling',
    },
    {
      title: 'Structured Tables & Monospaced Code',
      desc: 'Parses Markdown comparison tables into crisp bordered grid tables with zebra striping, and wraps code snippets in syntax-labeled monospaced blocks.',
      icon: Code2,
      tag: 'Code & Data',
    },
    {
      title: 'Permanent Clickable Brand Footers',
      desc: 'Every page features a professional footer branding with the official AI Chat to PDF badge, clickable website link, and Developer: Priyal Kumar LinkedIn attribution.',
      icon: ExternalLink,
      tag: 'Attribution',
    },
    {
      title: '100% Free Forever & Zero Login',
      desc: 'No credit card, no sign-up, no email verification, and no subscriptions. Open the site, paste your link, and download your revision notes in seconds.',
      icon: Sparkles,
      tag: 'Free Utility',
    },
    {
      title: 'Client-Side Privacy & High Capacity',
      desc: 'PDFs are compiled locally in your browser memory using vector jsPDF. Easily handles 60+ page semester guides without server logging or data retention.',
      icon: Lock,
      tag: 'Privacy',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 space-y-14">
        {/* Header */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] rounded-[2px] text-xs font-semibold text-blue-600 dark:text-[#3B82F6]">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Academic Excellence & Typesetting</span>
          </div>
          <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
            Engineered For Student Notes & Revision
          </h1>
          <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Everything you need to convert messy AI chats into high-scoring, printable study guides with diagrams, clean borders, and full editing freedom.
          </p>
        </div>

        {/* Comparison Table (Moved to TOP as requested!) */}
        <div className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] p-6 sm:p-8 shadow-sm space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 dark:border-[#263448] pb-4">
            <div>
              <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-[#E8EDF5]">
                Why AI Chat to PDF Beats Raw Browser Print & Screenshots
              </h2>
              <p className="text-xs text-slate-500 dark:text-[#94A3B8]">
                See how specialized academic typesetting compares against standard browser tools.
              </p>
            </div>
            <span className="w-fit text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-300 dark:border-emerald-800 px-2.5 py-1 rounded-[2px]">
              100% Free For All Students
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-slate-200 dark:border-[#263448] text-slate-500 dark:text-[#64748B]">
                  <th className="py-3 px-3 font-semibold">Capability</th>
                  <th className="py-3 px-3 font-bold text-blue-600 dark:text-[#3B82F6] bg-blue-50/50 dark:bg-[#192536]/40">AI Chat to PDF</th>
                  <th className="py-3 px-3 font-semibold">Browser Print (Ctrl+P)</th>
                  <th className="py-3 px-3 font-semibold">Screenshots / CamScanner</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-[#263448]/60 text-slate-700 dark:text-[#94A3B8]">
                <tr>
                  <td className="py-3 px-3 font-medium text-slate-900 dark:text-[#E8EDF5]">Multi-AI Support (ChatGPT, Claude, Gemini, Perplexity)</td>
                  <td className="py-3 px-3 text-emerald-600 dark:text-emerald-400 font-bold bg-blue-50/30 dark:bg-[#192536]/20"><Check className="w-4 h-4 inline mr-1" /> All in 1 Tool</td>
                  <td className="py-3 px-3 text-slate-500">Manual per site</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> Manual crop</td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-medium text-slate-900 dark:text-[#E8EDF5]">Live Inline Message Editing & Filler Cleaning</td>
                  <td className="py-3 px-3 text-emerald-600 dark:text-emerald-400 font-bold bg-blue-50/30 dark:bg-[#192536]/20"><Check className="w-4 h-4 inline mr-1" /> Full Freedom</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> Cannot edit</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> Cannot edit</td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-medium text-slate-900 dark:text-[#E8EDF5]">Embedded Diagrams & Flowcharts</td>
                  <td className="py-3 px-3 text-emerald-600 dark:text-emerald-400 font-bold bg-blue-50/30 dark:bg-[#192536]/20"><Check className="w-4 h-4 inline mr-1" /> High-Res Vector</td>
                  <td className="py-3 px-3 text-slate-500">Cluttered UI bars</td>
                  <td className="py-3 px-3 text-slate-500">Blurry pixels</td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-medium text-slate-900 dark:text-[#E8EDF5]">MS Word Classic Shadow Border</td>
                  <td className="py-3 px-3 text-emerald-600 dark:text-emerald-400 font-bold bg-blue-50/30 dark:bg-[#192536]/20"><Check className="w-4 h-4 inline mr-1" /> Yes (Customizable)</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> None</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> None</td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-medium text-slate-900 dark:text-[#E8EDF5]">Clickable Footers & Developer Attribution</td>
                  <td className="py-3 px-3 text-emerald-600 dark:text-emerald-400 font-bold bg-blue-50/30 dark:bg-[#192536]/20"><Check className="w-4 h-4 inline mr-1" /> Every Single Page</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> Browser URLs</td>
                  <td className="py-3 px-3 text-red-500"><X className="w-4 h-4 inline mr-1" /> None</td>
                </tr>
                <tr>
                  <td className="py-3 px-3 font-medium text-slate-900 dark:text-[#E8EDF5]">Account or Login Required</td>
                  <td className="py-3 px-3 text-emerald-600 dark:text-emerald-400 font-bold bg-blue-50/30 dark:bg-[#192536]/20"><Check className="w-4 h-4 inline mr-1" /> Zero Signup (100% Free)</td>
                  <td className="py-3 px-3 text-slate-500">Requires active tab</td>
                  <td className="py-3 px-3 text-slate-500">Requires apps</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Feature Cards Grid: 3 CARDS PER ROW with Professional Green Hover Effect */}
        <div className="space-y-6">
          <div className="text-center space-y-1">
            <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-[#E8EDF5]">
              Everything Included in Free Mode
            </h2>
            <p className="text-xs text-slate-500 dark:text-[#94A3B8]">
              No paid tiers or locked features. Every setting is unlocked for student productivity.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {capabilities.map((c, i) => {
              const Icon = c.icon;
              return (
                <div
                  key={i}
                  className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] p-5 sm:p-6 rounded-[2px] shadow-xs space-y-3 hover:border-emerald-500/70 hover:bg-emerald-50/30 dark:hover:bg-emerald-950/20 hover:shadow-md transition-all duration-300 group"
                >
                  <div className="flex items-center justify-between">
                    <div className="w-9 h-9 rounded-[2px] bg-blue-50 dark:bg-[#192536] border border-blue-200 dark:border-[#263448] flex items-center justify-center text-blue-600 dark:text-[#3B82F6] group-hover:text-emerald-600 dark:group-hover:text-emerald-400 group-hover:border-emerald-300 dark:group-hover:border-emerald-800 transition-colors">
                      <Icon className="w-4 h-4" strokeWidth={1.75} />
                    </div>
                    <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded-[2px] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-500 dark:text-[#94A3B8]">
                      {c.tag}
                    </span>
                  </div>
                  <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5] group-hover:text-emerald-700 dark:group-hover:text-emerald-300 transition-colors">
                    {c.title}
                  </h3>
                  <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                    {c.desc}
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* Action CTA */}
        <div className="text-center pt-4">
          <Link
            href="/"
            className="h-11 px-8 bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-semibold rounded-[2px] inline-flex items-center gap-2 transition-colors shadow-sm"
          >
            <span>Convert Your AI Notes Now</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </main>

      <Footer />
    </div>
  );
}
