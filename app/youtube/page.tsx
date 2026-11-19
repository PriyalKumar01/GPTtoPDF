'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import { PlatformShowcase } from '@/components/PlatformShowcase';
import {
  Download,
  Clipboard,
  X,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Film,
  Music,
  ExternalLink,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { YouTubeIcon } from '@/components/BrandIcons';
import { DownloaderProgressLoader } from '@/components/DownloaderProgressLoader';

export default function UniversalDownloaderPage() {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<any | null>(null);
  const [activePlatform, setActivePlatform] = useState<string>('all');
  const [faqOpen, setFaqOpen] = useState<number | null>(null);
  const resultRef = useRef<HTMLDivElement>(null);

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setUrl(text.trim());
        setError(null);
      }
    } catch {}
  };

  const handleClear = () => {
    setUrl('');
    setError(null);
    setResult(null);
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!url.trim()) {
      setError('Please enter or paste a video link');
      return;
    }

    setLoading(true);
    setIsComplete(false);
    setError(null);
    setResult(null);

    try {
      const res = await fetch('/api/universal-download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: url.trim() }),
      });

      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Failed to fetch video. Please verify link is public.');
      }

      setIsComplete(true);
      setTimeout(() => {
        setResult(data.data);
        setLoading(false);
        setTimeout(() => {
          resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 150);
      }, 400);
    } catch (err: any) {
      setLoading(false);
      setIsComplete(false);
      setError(err?.message || 'Unable to download this video.');
    }
  };

  const getProxyUrl = (mediaUrl: string, filename: string) => {
    return `/api/proxy-download?url=${encodeURIComponent(mediaUrl)}&filename=${encodeURIComponent(
      filename
    )}`;
  };

  const faqs = [
    {
      q: 'Can I download TikTok videos without a watermark?',
      a: 'Yes! Our universal downloader automatically strips the TikTok watermark and delivers the clean original HD MP4 video.',
    },
    {
      q: 'Does it support YouTube Shorts?',
      a: 'Yes, it supports YouTube standard videos and YouTube Shorts directly.',
    },
    {
      q: 'Can I extract audio (MP3) from videos?',
      a: 'Yes, whenever an audio stream is available, a "Download Audio (MP3)" button is displayed.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        {/* Hero Section */}
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/20">
              <YouTubeIcon className="w-3.5 h-3.5 text-red-600 shrink-0" />
              <span>Universal Video Downloader</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Download YouTube Shorts, TikTok & Web Videos
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Save TikTok videos without watermark, YouTube Shorts, Twitter/X, and Pinterest videos in high quality. 100% Free with no login.
            </p>
          </div>

          {/* Quick Platform Hint Tabs */}
          <div className="flex items-center justify-center gap-2 mb-5">
            {[
              { id: 'all', label: 'All Supported' },
              { id: 'youtube', label: 'YouTube / Shorts' },
              { id: 'tiktok', label: 'TikTok (No Watermark)' },
              { id: 'twitter', label: 'Twitter / X' },
              { id: 'pinterest', label: 'Pinterest' },
            ].map((tab) => {
              const isSelected = activePlatform === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActivePlatform(tab.id)}
                  className={`px-3 py-1.5 rounded-[2px] text-xs font-medium transition-all ${
                    isSelected
                      ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30 font-semibold'
                      : 'bg-white dark:bg-[#111A29] text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] border border-slate-200 dark:border-[#263448]'
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          {/* Input Box Card */}
          <div className="bg-white dark:bg-[#141D2B] rounded-[2px] p-5 sm:p-7 shadow-sm border border-slate-200 dark:border-[#263448] max-w-4xl mx-auto">
            <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row items-center gap-2">
              <div className="relative flex-1 w-full">
                <input
                  type="url"
                  value={url}
                  onChange={(e) => {
                    setUrl(e.target.value);
                    if (error) setError(null);
                  }}
                  placeholder="Paste video link here (TikTok, YouTube Shorts, Twitter/X, Pinterest)..."
                  className="w-full h-11 pl-3.5 pr-20 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] placeholder-slate-400 dark:placeholder-[#64748B] focus:outline-none focus:border-[#3B82F6] transition-colors"
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
                className="w-full sm:w-auto h-11 px-6 bg-[#DC2626] hover:bg-[#B91C1C] active:bg-[#991B1B] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] disabled:text-slate-500 text-white font-semibold text-xs sm:text-sm rounded-[2px] flex items-center justify-center gap-2 transition shadow-xs shrink-0"
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

            {/* Professional Progress Loader with Realistic Percentage */}
            {loading && (
              <DownloaderProgressLoader
                platform="youtube"
                isComplete={isComplete}
              />
            )}

            {error && (
              <div className="mt-4 p-3 bg-red-50 dark:bg-[#EF4444]/10 border border-red-200 dark:border-[#EF4444]/30 rounded-[2px] flex items-start gap-2.5 text-xs text-red-600 dark:text-[#EF4444]">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <div className="space-y-1 text-left">
                  <p className="font-semibold">Unable to process link</p>
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
                  <span className="px-2 py-0.5 rounded-[2px] uppercase text-[10px] font-bold bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-900/50">
                    {result.platformName}
                  </span>
                  <h3 className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] line-clamp-1">
                    {result.title}
                  </h3>
                </div>

                {result.author?.name && (
                  <span className="text-xs text-slate-500 dark:text-[#94A3B8]">
                    By {result.author.name}
                  </span>
                )}
              </div>

              <div className="p-4 sm:p-6 grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
                {/* Thumbnail / Video */}
                <div className="relative rounded-[2px] overflow-hidden bg-black aspect-video max-h-[300px] mx-auto w-full flex items-center justify-center border border-slate-200 dark:border-[#263448]">
                  {result.thumbnailUrl ? (
                    <img
                      src={result.thumbnailUrl}
                      alt={result.title}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="flex items-center justify-center text-slate-400">
                      <Film className="w-10 h-10 opacity-50" />
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-semibold text-slate-500 dark:text-[#94A3B8] uppercase tracking-wider">
                      Available Downloads
                    </h4>
                    {result.duration && (
                      <span className="text-[11px] font-medium text-slate-500 dark:text-[#94A3B8]">
                        Duration: {Math.floor(result.duration / 60)}:{String(result.duration % 60).padStart(2, '0')}
                      </span>
                    )}
                  </div>

                  {/* Multi-Quality Options with Exact MB */}
                  {result.formats && result.formats.length > 0 ? (
                    <div className="space-y-2">
                      {result.formats.map((fmt: any, idx: number) => (
                        <a
                          key={idx}
                          href={getProxyUrl(fmt.url, `${result.platform}_${result.id}_${fmt.qualityLabel.replace(/\s+/g, '_')}.${fmt.ext || 'mp4'}`)}
                          download
                          className="h-10 px-4 rounded-[2px] font-semibold text-xs sm:text-sm text-white bg-[#DC2626] hover:bg-[#B91C1C] flex items-center justify-between transition shadow-xs group"
                        >
                          <div className="flex items-center gap-2">
                            <Download className="w-4 h-4 group-hover:translate-y-0.5 transition-transform" />
                            <span>Download {fmt.qualityLabel} (MP4)</span>
                          </div>
                          <span className="text-[11px] font-mono px-2 py-0.5 rounded-[2px] bg-black/25 text-white/90">
                            {fmt.filesizeMb || 'Direct HD'}
                          </span>
                        </a>
                      ))}
                    </div>
                  ) : (
                    result.downloadUrl && (
                      <a
                        href={
                          result.platform === 'tiktok'
                            ? getProxyUrl(result.downloadUrl, `tiktok_${result.id}_nowatermark.mp4`)
                            : getProxyUrl(result.downloadUrl, `video_${result.id}.mp4`)
                        }
                        download
                        className="h-10 px-4 rounded-[2px] font-semibold text-xs sm:text-sm text-white bg-[#DC2626] hover:bg-[#B91C1C] flex items-center justify-between transition shadow-xs"
                      >
                        <div className="flex items-center gap-2">
                          <Download className="w-4 h-4" />
                          <span>
                            {result.platform === 'tiktok'
                              ? 'Download Video (MP4 Without Watermark)'
                              : 'Download Video (HD MP4)'}
                          </span>
                        </div>
                        {result.filesizeMb && (
                          <span className="text-[11px] font-mono px-2 py-0.5 rounded-[2px] bg-black/25 text-white/90">
                            {result.filesizeMb}
                          </span>
                        )}
                      </a>
                    )
                  )}

                  {result.audioUrl && (
                    <a
                      href={getProxyUrl(result.audioUrl, `audio_${result.id}.mp3`)}
                      download
                      className="h-9 px-4 rounded-[2px] font-medium text-xs text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-between transition"
                    >
                      <div className="flex items-center gap-2">
                        <Music className="w-3.5 h-3.5 text-indigo-500" />
                        <span>Download Audio Only (MP3)</span>
                      </div>
                      <span className="text-[10px] font-mono text-slate-500 dark:text-slate-400">
                        High-Quality MP3
                      </span>
                    </a>
                  )}

                  {result.thumbnailUrl && (
                    <a
                      href={getProxyUrl(result.thumbnailUrl, `thumbnail_${result.id}.jpg`)}
                      download
                      className="h-9 px-4 rounded-[2px] text-xs font-medium text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-between transition"
                    >
                      <div className="flex items-center gap-1.5">
                        <Download className="w-3.5 h-3.5 text-slate-400" />
                        <span>Download High-Res Thumbnail</span>
                      </div>
                      <span className="text-[10px] text-slate-400">Cover JPG</span>
                    </a>
                  )}
                </div>
              </div>
            </div>
          </section>
        )}

        {/* Live Interactive Smartphone Workflow Demo */}
        <section className="py-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <PlatformShowcase platform="youtube" />
        </section>

        {/* FAQs */}
        <section className="py-12 border-t border-slate-200 dark:border-[#263448]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-8">
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
                Frequently Asked Questions
              </h2>
            </div>

            <div className="space-y-2">
              {faqs.map((faq, index) => {
                const isOpen = faqOpen === index;
                return (
                  <div
                    key={index}
                    className="rounded-[2px] border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#111A29] overflow-hidden"
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
