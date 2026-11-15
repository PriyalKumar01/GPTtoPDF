'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import { FacebookIcon } from '@/components/BrandIcons';
import { PlatformShowcase } from '@/components/PlatformShowcase';
import {
  Download,
  Clipboard,
  X,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Film,
  MonitorPlay,
  Zap,
  ShieldCheck,
  Smartphone,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { FacebookResult } from '@/lib/extract-facebook';

export default function FacebookDownloaderPage() {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<FacebookResult | null>(null);
  const [faqOpen, setFaqOpen] = useState<number | null>(null);
  const resultRef = useRef<HTMLDivElement>(null);

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setUrl(text.trim());
        setError(null);
      }
    } catch {
      // Clipboard permission denied or unsupported
    }
  };

  const handleClear = () => {
    setUrl('');
    setError(null);
    setResult(null);
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!url.trim()) {
      setError('Please enter or paste a Facebook video or reel link');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await fetch('/api/facebook', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: url.trim() }),
      });

      const data = await res.json();

      if (!res.ok || !data.success) {
        throw new Error(
          data.error || 'Failed to fetch Facebook video. Please verify the link is public.'
        );
      }

      setResult(data.data);
      setTimeout(() => {
        resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 150);
    } catch (err: any) {
      setError(
        err?.message ||
          'Unable to download this video. Please ensure the video is public and try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const getProxyUrl = (mediaUrl: string, filename: string) => {
    return `/api/proxy-download?url=${encodeURIComponent(mediaUrl)}&filename=${encodeURIComponent(
      filename
    )}`;
  };

  const faqs = [
    {
      q: 'Can I download Facebook Reels and Watch videos?',
      a: 'Yes! Our tool supports Facebook Reels, Watch videos, Page videos, and public user posts. Just paste the link and click Download.',
    },
    {
      q: 'What video quality is available for download?',
      a: 'When available, you can choose between High Definition (HD 720p/1080p) and Standard Definition (SD 360p) formats.',
    },
    {
      q: 'Can I download private Facebook videos or private group videos?',
      a: 'No. To respect privacy and terms of service, this tool only processes publicly accessible Facebook videos and reels.',
    },
    {
      q: 'Do I need to install any browser extension or software?',
      a: 'No software or extension is required. Everything runs directly in your web browser on mobile phones, tablets, or desktop PCs.',
    },
    {
      q: 'Is there a limit on how many videos I can download?',
      a: 'There is no limit. You can download as many public Facebook videos and reels as you need for personal study, offline viewing, or archival.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        {/* Hero Section */}
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
              <FacebookIcon className="w-3.5 h-3.5" />
              <span>Facebook Reel Save — Free Video Downloader Online</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Facebook Reel Save — Download FB Videos in <span className="text-blue-600 dark:text-blue-400">1080p Full HD</span>
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Save high quality public Facebook Reels, Watch videos, and clips in 1080p Full HD
              or SD directly to your device with exact file size display in MB. 100% free and no account needed.
            </p>
          </div>

          {/* Input Box Card */}
          <div className="bg-white dark:bg-[#141D2B] rounded-[2px] p-5 sm:p-7 shadow-sm border border-slate-200 dark:border-[#263448] max-w-4xl mx-auto">
            <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row items-center gap-2">
              <div className="relative flex-1 w-full">
                <div className="absolute left-3.5 top-1/2 -translate-y-1/2 text-blue-600">
                  <FacebookIcon className="w-4 h-4" />
                </div>
                <input
                  type="url"
                  value={url}
                  onChange={(e) => {
                    setUrl(e.target.value);
                    if (error) setError(null);
                  }}
                  placeholder="Paste Facebook video or reel link (e.g. https://fb.watch/...)"
                  className="w-full h-11 pl-10 pr-20 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] placeholder-slate-400 dark:placeholder-[#64748B] focus:outline-none focus:border-[#3B82F6] transition-colors"
                />
                <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1">
                  {url ? (
                    <button
                      type="button"
                      onClick={handleClear}
                      className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition"
                      title="Clear"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={handlePaste}
                      className="inline-flex items-center gap-1 px-2 py-1 text-xs text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-slate-200 dark:bg-[#192536] rounded-[2px] transition-colors"
                    >
                      <Clipboard className="w-3 h-3" />
                      <span>Paste</span>
                    </button>
                  )}
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full sm:w-auto h-11 px-6 bg-[#1877F2] hover:bg-[#0C63D4] active:bg-[#0A4FA8] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] disabled:text-slate-500 text-white font-semibold text-xs sm:text-sm rounded-[2px] flex items-center justify-center gap-2 transition shadow-xs shrink-0"
              >
                {loading ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>Processing...</span>
                  </>
                ) : (
                  <>
                    <Download className="w-4 h-4" />
                    <span>Download</span>
                  </>
                )}
              </button>
            </form>

            {error && (
              <div className="mt-4 p-3 bg-red-50 dark:bg-[#EF4444]/10 border border-red-200 dark:border-[#EF4444]/30 rounded-[2px] flex items-start gap-2.5 text-xs text-red-600 dark:text-[#EF4444]">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <div className="space-y-1 text-left">
                  <p className="font-semibold">Unable to process video link</p>
                  <p className="text-slate-600 dark:text-[#94A3B8]">{error}</p>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Results Card Section */}
        {result && (
          <section ref={resultRef} className="py-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-white dark:bg-[#141D2B] rounded-[2px] border border-slate-200 dark:border-[#263448] shadow-sm overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-200 dark:border-[#263448] flex items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-[2px] bg-blue-100 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center">
                    <FacebookIcon className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] line-clamp-1">
                      {result.title}
                    </h3>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] font-mono">
                      ID: {result.id}
                    </p>
                  </div>
                </div>

                <span className="px-2 py-0.5 rounded-[2px] uppercase text-[10px] font-bold bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-900/50">
                  Ready to save
                </span>
              </div>

              {/* Video Preview and Quality Selection */}
              <div className="p-4 sm:p-6 grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
                <div className="relative rounded-[2px] overflow-hidden bg-black aspect-video max-h-[280px] mx-auto w-full flex items-center justify-center border border-slate-200 dark:border-[#263448]">
                  {result.thumbnailUrl ? (
                    <img
                      src={result.thumbnailUrl}
                      alt={result.title}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center text-slate-400">
                      <MonitorPlay className="w-10 h-10 mb-1 opacity-50" />
                      <span className="text-xs">Facebook Video Preview</span>
                    </div>
                  )}
                </div>

                <div className="flex flex-col gap-3">
                  <h4 className="text-xs font-semibold text-slate-500 dark:text-[#94A3B8] uppercase tracking-wider">
                    Select Download Quality
                  </h4>

                  <div className="space-y-2">
                    {result.formats.map((fmt, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-[2px] bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-between gap-3"
                      >
                        <div className="flex items-center gap-2">
                          <span
                            className={`px-1.5 py-0.5 rounded-[2px] text-[10px] font-bold font-mono ${
                              fmt.quality === 'HD'
                                ? 'bg-blue-100 dark:bg-blue-950/70 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800'
                                : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                            }`}
                          >
                            {fmt.quality}
                          </span>
                          <div>
                            <div className="flex items-center gap-2">
                              <p className="text-xs font-bold text-slate-900 dark:text-[#E8EDF5]">
                                {fmt.label}
                              </p>
                              {fmt.filesizeMb && (
                                <span className="px-1.5 py-0.2 text-[10px] font-bold font-mono bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/60 rounded-[2px]">
                                  {fmt.filesizeMb}
                                </span>
                              )}
                            </div>
                            <p className="text-[10px] text-slate-500 dark:text-[#94A3B8]">
                              Format: MP4 • {fmt.resolution ? `${fmt.resolution} • ` : ''}High-Speed Direct Stream
                            </p>
                          </div>
                        </div>

                        <a
                          href={getProxyUrl(
                            fmt.url,
                            `facebook_${fmt.quality.toLowerCase()}_${result.id}.mp4`
                          )}
                          download
                          className="h-8 px-3 rounded-[2px] text-xs font-semibold text-white bg-[#1877F2] hover:bg-[#0866FF] flex items-center gap-1.5 transition shadow-sm hover:scale-[1.01]"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download {fmt.filesizeMb ? `(${fmt.filesizeMb})` : ''}</span>
                        </a>
                      </div>
                    ))}
                  </div>

                  {result.thumbnailUrl && (
                    <a
                      href={getProxyUrl(result.thumbnailUrl, `facebook_thumbnail_${result.id}.jpg`)}
                      download
                      className="w-full mt-1 h-8 px-3 rounded-[2px] text-xs font-medium text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-1.5 transition"
                    >
                      <Download className="w-3 h-3" />
                      <span>Download Video Cover (Thumbnail)</span>
                    </a>
                  )}
                </div>
              </div>
            </div>
          </section>
        )}

        {/* Live Interactive Smartphone Workflow Demo */}
        <section className="py-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <PlatformShowcase platform="facebook" />
        </section>

        {/* FAQs - Full Screen Widescreen Layout */}
        <section className="py-14 border-t border-slate-200 dark:border-[#263448] bg-slate-50/50 dark:bg-[#070B12]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-10">
              <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-[2px] bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                Help Center
              </span>
              <h2 className="text-xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-[#E8EDF5] mt-2">
                Frequently Asked Questions
              </h2>
              <p className="text-xs text-slate-500 dark:text-[#94A3B8] mt-1">
                Everything you need to know about downloading Facebook videos in Full HD.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {faqs.map((faq, index) => {
                const isOpen = faqOpen === index;
                return (
                  <div
                    key={index}
                    className="rounded-[2px] border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#111A29] overflow-hidden shadow-sm"
                  >
                    <button
                      onClick={() => setFaqOpen(isOpen ? null : index)}
                      className="w-full py-3.5 px-4 text-left flex items-center justify-between gap-4 font-medium text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5]"
                    >
                      <span>{faq.q}</span>
                      {isOpen ? (
                        <ChevronUp className="w-4 h-4 shrink-0 text-blue-500" />
                      ) : (
                        <ChevronDown className="w-4 h-4 shrink-0 text-slate-400" />
                      )}
                    </button>
                    {isOpen && (
                      <div className="px-4 pb-3.5 text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed border-t border-slate-100 dark:border-[#263448] pt-2.5">
                        {faq.a}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
