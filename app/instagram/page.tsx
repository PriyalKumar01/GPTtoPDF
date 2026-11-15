'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import { InstagramIcon } from '@/components/BrandIcons';
import { PlatformShowcase } from '@/components/PlatformShowcase';
import {
  Download,
  Clipboard,
  X,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Film,
  Image as ImageIcon,
  Layers,
  ShieldCheck,
  Zap,
  Smartphone,
  ChevronDown,
  ChevronUp,
  Heart,
  Eye,
  ExternalLink,
  Info,
} from 'lucide-react';
import { InstagramResult } from '@/lib/extract-instagram';

export default function InstagramDownloaderPage() {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<InstagramResult | null>(null);
  const [activeTab, setActiveTab] = useState<'all' | 'reels' | 'posts' | 'stories'>('all');
  const [showFullCaption, setShowFullCaption] = useState(false);
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
      setError('Please enter or paste an Instagram link');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await fetch('/api/instagram', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: url.trim() }),
      });

      const data = await res.json();

      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Failed to fetch Instagram media. Please try again.');
      }

      setResult(data.data);
      setTimeout(() => {
        resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 150);
    } catch (err: any) {
      setError(
        err?.message ||
          'Unable to download this media. Please verify that the post is public and try again.'
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
      q: 'Is this Instagram downloader free to use?',
      a: 'Yes, 100% free with unlimited downloads. There are no subscriptions, hidden fees, or account registration required.',
    },
    {
      q: 'Will downloaded Instagram Reels have a watermark?',
      a: 'No. Our tool extracts the raw original video stream directly from Instagram servers, so your downloaded reel or video has zero watermarks.',
    },
    {
      q: 'Can I download private Instagram posts or stories?',
      a: 'Due to Instagram privacy restrictions, our tool only supports public accounts and posts. Private posts require user authentication and cannot be accessed publicly.',
    },
    {
      q: 'Does it work on iPhone, Android, and PC?',
      a: 'Yes! The downloader works seamlessly on any modern web browser across iOS (Safari/Chrome), Android, Windows, Mac, and Linux.',
    },
    {
      q: 'Where are downloaded Instagram videos saved?',
      a: 'Videos and photos are saved directly into your device’s default "Downloads" folder or camera roll gallery.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        {/* Hero Section */}
        <section className="relative overflow-hidden pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448] bg-gradient-to-b from-pink-500/5 via-rose-500/5 to-transparent dark:from-[#2d0f1e]/30 dark:via-[#1a0c18]/20 dark:to-transparent">
          <div className="text-center space-y-3 mb-8">
            {/* Badge */}
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-pink-500/10 text-pink-600 dark:text-pink-400 border border-pink-500/20">
              <InstagramIcon className="w-3.5 h-3.5 text-pink-500" />
              <span>Insta Save — Free Instagram Reel & Video Downloader</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Insta Save — Download Instagram Reels & Posts in <span className="text-pink-600 dark:text-pink-400">1080p HD</span>
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Save high-quality Instagram Reels, Videos, Carousel photos, and Stories directly to
              your phone, tablet, or PC. Fast, free, no watermark, and zero login required.
            </p>
          </div>

          {/* Quick Filter Tabs */}
          <div className="flex items-center justify-center gap-2 mb-5">
            {[
              { id: 'all', label: 'All Links', icon: Sparkles },
              { id: 'reels', label: 'Reels', icon: Film },
              { id: 'posts', label: 'Photos / Carousel', icon: Layers },
              { id: 'stories', label: 'Stories', icon: ImageIcon },
            ].map((tab) => {
              const Icon = tab.icon;
              const isSelected = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-[2px] text-xs font-medium transition-all ${
                    isSelected
                      ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30 font-semibold'
                      : 'bg-white dark:bg-[#111A29] text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] border border-slate-200 dark:border-[#263448]'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>

          {/* Input Box Card */}
          <div className="bg-white dark:bg-[#141D2B] rounded-[2px] p-5 sm:p-7 shadow-sm border border-slate-200 dark:border-[#263448] max-w-4xl mx-auto">
            <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row items-center gap-2">
              <div className="relative flex-1 w-full">
                <div className="absolute left-3.5 top-1/2 -translate-y-1/2 text-pink-500">
                  <InstagramIcon className="w-4 h-4" />
                </div>
                <input
                  type="url"
                  value={url}
                  onChange={(e) => {
                    setUrl(e.target.value);
                    if (error) setError(null);
                  }}
                  placeholder="Paste Instagram link here (e.g. https://www.instagram.com/reel/...)"
                  className="w-full h-11 pl-10 pr-20 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] placeholder-slate-400 dark:placeholder-[#64748B] focus:outline-none focus:border-[#3B82F6] transition-colors"
                />
                {/* Action buttons inside input */}
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
                className="w-full sm:w-auto h-11 px-6 bg-gradient-to-r from-[#D82D7E] to-[#C13584] hover:from-[#C13584] hover:to-[#A82D72] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] disabled:text-slate-500 text-white font-semibold text-xs sm:text-sm rounded-[2px] flex items-center justify-center gap-2 transition shadow-xs shrink-0"
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

            {/* Error Message */}
            {error && (
              <div className="mt-4 p-3 bg-red-50 dark:bg-[#EF4444]/10 border border-red-200 dark:border-[#EF4444]/30 rounded-[2px] flex items-start gap-2.5 text-xs text-red-600 dark:text-[#EF4444]">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <div className="space-y-1 text-left">
                  <p className="font-semibold">Unable to process link</p>
                  <p className="text-slate-600 dark:text-[#94A3B8]">{error}</p>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 pt-1">
                    Tip: If Instagram has applied temporary bot protection to this reel, you can try our{' '}
                    <a href="/youtube" className="text-blue-600 dark:text-blue-400 underline">
                      Universal Downloader
                    </a>{' '}
                    or verify the post is public.
                  </p>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Results Card Section */}
        {result && (
          <section ref={resultRef} className="py-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-white dark:bg-[#141D2B] rounded-[2px] border border-slate-200 dark:border-[#263448] shadow-sm overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-200 dark:border-[#263448] flex items-center justify-between flex-wrap gap-3">
                {/* Author Info */}
                <div className="flex items-center gap-3">
                  {result.author.avatarUrl ? (
                    <img
                      src={result.author.avatarUrl}
                      alt={result.author.username}
                      className="w-9 h-9 rounded-[2px] object-cover border border-slate-200 dark:border-[#263448]"
                    />
                  ) : (
                    <div className="w-9 h-9 rounded-[2px] bg-slate-100 dark:bg-[#1E293B] flex items-center justify-center text-slate-800 dark:text-[#E8EDF5] font-bold text-xs">
                      {result.author.username[0]?.toUpperCase() || 'I'}
                    </div>
                  )}
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5]">
                        @{result.author.username}
                      </span>
                      {result.author.isVerified && (
                        <CheckCircle2 className="w-3.5 h-3.5 text-blue-500" />
                      )}
                    </div>
                    {result.author.fullName && (
                      <p className="text-[11px] text-slate-500 dark:text-[#94A3B8]">
                        {result.author.fullName}
                      </p>
                    )}
                  </div>
                </div>

                {/* Media Type Badge & Metrics */}
                <div className="flex items-center gap-3 text-xs">
                  {result.metrics?.likes !== undefined && (
                    <span className="inline-flex items-center gap-1 text-slate-500 dark:text-[#94A3B8]">
                      <Heart className="w-3 h-3 text-rose-500" />
                      {result.metrics.likes.toLocaleString()}
                    </span>
                  )}
                  {result.metrics?.views !== undefined && (
                    <span className="inline-flex items-center gap-1 text-slate-500 dark:text-[#94A3B8]">
                      <Eye className="w-3 h-3 text-blue-500" />
                      {result.metrics.views.toLocaleString()}
                    </span>
                  )}
                  <span className="px-2 py-0.5 rounded-[2px] uppercase text-[10px] font-bold bg-pink-50 dark:bg-pink-950/60 text-pink-600 dark:text-pink-400 border border-pink-200 dark:border-pink-900/50">
                    {result.type}
                  </span>
                </div>
              </div>

              {/* Informative notice if rate limited */}
              {result.notice && (
                <div className="mx-4 sm:mx-6 mt-4 p-3 rounded-[2px] bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/40 text-amber-700 dark:text-amber-300 text-xs flex items-start gap-2">
                  <Info className="w-4 h-4 shrink-0 mt-0.5 text-amber-500" />
                  <div className="space-y-1">
                    <p className="font-semibold">Notice</p>
                    <p>{result.notice}</p>
                    <a
                      href={`https://www.instagram.com/reel/${result.shortcode}/`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 font-semibold text-blue-600 dark:text-blue-400 underline mt-1"
                    >
                      <span>Open Reel in Instagram</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                </div>
              )}

              {/* Caption */}
              {result.caption && (
                <div className="px-4 sm:px-6 pt-4 pb-2 text-xs text-slate-700 dark:text-[#94A3B8]">
                  <p className={showFullCaption ? '' : 'line-clamp-2'}>
                    {result.caption}
                  </p>
                  {result.caption.length > 120 && (
                    <button
                      onClick={() => setShowFullCaption(!showFullCaption)}
                      className="text-blue-600 dark:text-blue-400 font-medium text-xs mt-1 hover:underline flex items-center gap-0.5"
                    >
                      {showFullCaption ? (
                        <>
                          Show less <ChevronUp className="w-3 h-3" />
                        </>
                      ) : (
                        <>
                          Show more <ChevronDown className="w-3 h-3" />
                        </>
                      )}
                    </button>
                  )}
                </div>
              )}

              {/* Media Preview & Download Area */}
              <div className="p-4 sm:p-6">
                {result.mediaItems.length === 1 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
                    <div className="relative rounded-[2px] overflow-hidden bg-black aspect-[9/16] max-h-[380px] mx-auto w-full flex items-center justify-center border border-slate-200 dark:border-[#263448]">
                      {result.mediaItems[0].type === 'video' ? (
                        <video
                          src={result.mediaItems[0].url}
                          poster={result.thumbnailUrl}
                          controls
                          playsInline
                          className="w-full h-full object-contain"
                        />
                      ) : (
                        <img
                          src={result.mediaItems[0].url}
                          alt="Instagram Post"
                          className="w-full h-full object-contain"
                        />
                      )}
                    </div>

                    <div className="flex flex-col gap-3">
                      <div className="p-4 rounded-[2px] bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448]">
                        <h4 className="text-xs font-semibold text-slate-500 dark:text-[#94A3B8] uppercase tracking-wider mb-2">
                          Available Download
                        </h4>

                        {/* Direct Download Button */}
                        <a
                          href={getProxyUrl(
                            result.mediaItems[0].url,
                            `instagram_${result.type}_${result.shortcode}.${
                              result.mediaItems[0].type === 'video' ? 'mp4' : 'jpg'
                            }`
                          )}
                          download
                          className="w-full h-11 px-4 rounded-[2px] font-semibold text-xs sm:text-sm text-white bg-gradient-to-r from-[#E1306C] via-[#D82D7E] to-[#C13584] hover:opacity-95 shadow-md flex items-center justify-center gap-2 transition hover:scale-[1.01]"
                        >
                          <Download className="w-4 h-4" />
                          <span>
                            Download {result.mediaItems[0].type === 'video' ? 'Video (HD MP4)' : 'Photo (HD)'}
                          </span>
                        </a>

                        {/* Cover Thumbnail */}
                        {result.thumbnailUrl && (
                          <a
                            href={getProxyUrl(
                              result.thumbnailUrl,
                              `instagram_cover_${result.shortcode}.jpg`
                            )}
                            download
                            className="w-full mt-2 h-9 px-3 rounded-[2px] text-xs font-medium text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-1.5 transition"
                          >
                            <ImageIcon className="w-3.5 h-3.5" />
                            <span>Download Cover Thumbnail</span>
                          </a>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div>
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="font-semibold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
                        <Layers className="w-4 h-4 text-pink-500" />
                        <span>Carousel Media ({result.mediaItems.length} items)</span>
                      </h4>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                      {result.mediaItems.map((item, index) => (
                        <div
                          key={index}
                          className="rounded-[2px] border border-slate-200 dark:border-[#263448] overflow-hidden bg-slate-50 dark:bg-[#111A29] flex flex-col"
                        >
                          <div className="relative aspect-square bg-black flex items-center justify-center overflow-hidden">
                            {item.type === 'video' ? (
                              <video
                                src={item.url}
                                controls
                                playsInline
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <img
                                src={item.url}
                                alt={`Item ${index + 1}`}
                                className="w-full h-full object-cover"
                              />
                            )}
                            <span className="absolute top-2 left-2 px-1.5 py-0.5 rounded-[2px] text-[10px] font-bold bg-black/70 text-white">
                              #{index + 1} • {item.type.toUpperCase()}
                            </span>
                          </div>

                          <div className="p-3 mt-auto">
                            <a
                              href={getProxyUrl(
                                item.url,
                                `instagram_${result.shortcode}_part${index + 1}.${
                                  item.type === 'video' ? 'mp4' : 'jpg'
                                }`
                              )}
                              download
                              className="w-full h-9 px-3 rounded-[2px] text-xs font-semibold text-white bg-[#C13584] hover:bg-[#A82D72] flex items-center justify-center gap-1.5 transition"
                            >
                              <Download className="w-3.5 h-3.5" />
                              <span>Download {item.type === 'video' ? 'MP4' : 'JPG'}</span>
                            </a>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </section>
        )}

        {/* How It Works Guide Section */}
        <section className="py-12 border-t border-slate-200 dark:border-[#263448] bg-white dark:bg-[#090D14]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-8">
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
                How to Download Instagram Reels in 3 Simple Steps
              </h2>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                {
                  step: '01',
                  title: 'Copy the Link',
                  desc: 'Open Instagram, find the Reel or Post you want to save, tap Share and select "Copy Link".',
                  icon: InstagramIcon,
                },
                {
                  step: '02',
                  title: 'Paste URL Here',
                  desc: 'Paste the copied Instagram link into the input box above and click the "Download" button.',
                  icon: Clipboard,
                },
                {
                  step: '03',
                  title: 'Save to Device',
                  desc: 'Preview the video or carousel images and click "Download MP4" to save it directly with zero watermark.',
                  icon: Download,
                },
              ].map((step, idx) => {
                const Icon = step.icon;
                return (
                  <div
                    key={idx}
                    className="p-5 rounded-[2px] bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] relative"
                  >
                    <div className="text-2xl font-black text-slate-300 dark:text-[#263448] mb-2 font-mono">
                      {step.step}
                    </div>
                    <h3 className="font-bold text-sm text-slate-900 dark:text-[#E8EDF5] mb-1">
                      {step.title}
                    </h3>
                    <p className="text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed">
                      {step.desc}
                    </p>
                  </div>
                );
              })}
            </div>

            {/* Live Interactive Smartphone Workflow Demo */}
            <PlatformShowcase platform="instagram" />
          </div>
        </section>

        {/* FAQs - Full Screen Widescreen Layout */}
        <section className="py-14 border-t border-slate-200 dark:border-[#263448] bg-slate-50/50 dark:bg-[#070B12]">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-10">
              <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-[2px] bg-pink-500/10 text-pink-600 dark:text-pink-400 border border-pink-500/20">
                Help & Support
              </span>
              <h2 className="text-xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-[#E8EDF5] mt-2">
                Frequently Asked Questions
              </h2>
              <p className="text-xs text-slate-500 dark:text-[#94A3B8] mt-1">
                Everything you need to know about downloading Instagram Reels, Videos, and Photos.
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
                        <ChevronUp className="w-4 h-4 shrink-0 text-pink-500" />
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

            {/* Live Interactive Smartphone Workflow Demo */}
            <PlatformShowcase platform="instagram" />
          </div>
        </section>

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
