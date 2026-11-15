'use client';

import React, { useState, useEffect } from 'react';
import {
  Download,
  Share2,
  CheckCircle2,
  Sparkles,
  ArrowRight,
  Clipboard,
  Play,
  Film,
  Zap,
} from 'lucide-react';
import {
  InstagramIcon,
  FacebookIcon,
  YouTubeIcon,
} from '@/components/BrandIcons';

interface PlatformShowcaseProps {
  platform: 'instagram' | 'facebook' | 'youtube';
}

export function PlatformShowcase({ platform }: PlatformShowcaseProps) {
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3>(1);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentStep((prev) => (prev === 3 ? 1 : ((prev + 1) as 1 | 2 | 3)));
    }, 3200);

    return () => clearInterval(timer);
  }, []);

  const config = {
    instagram: {
      brand: 'Instagram',
      title: 'Instagram Reels & Stories',
      icon: InstagramIcon,
      accentColor: 'text-pink-500',
      badgeBg: 'bg-pink-500/10 text-pink-600 dark:text-pink-400 border-pink-500/20',
      btnGradient: 'from-[#E1306C] via-[#D82D7E] to-[#C13584]',
      glowColor: 'rgba(225, 48, 108, 0.15)',
      sampleUser: '@explore_viral',
      sampleCaption: 'Sunset vibes in 4K HDR ✨ #reels #viral',
      fileSize: '3.58 MB',
      quality: '1080p Full HD',
      sampleUrl: 'https://instagram.com/reel/DftdqGjSKxM',
    },
    facebook: {
      brand: 'Facebook',
      title: 'Facebook Reels & Watch Clips',
      icon: FacebookIcon,
      accentColor: 'text-blue-600',
      badgeBg: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
      btnGradient: 'from-[#1877F2] to-[#0866FF]',
      glowColor: 'rgba(24, 119, 242, 0.15)',
      sampleUser: 'Viral Media Daily',
      sampleCaption: 'Incredible tech breakthrough in 2026! 🚀',
      fileSize: '5.20 MB',
      quality: '1080p Full HD',
      sampleUrl: 'https://facebook.com/watch/?v=892341092',
    },
    youtube: {
      brand: 'YouTube & Shorts',
      title: 'YouTube Shorts & TikTok',
      icon: YouTubeIcon,
      accentColor: 'text-red-600',
      badgeBg: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20',
      btnGradient: 'from-[#DC2626] to-[#B91C1C]',
      glowColor: 'rgba(220, 38, 38, 0.15)',
      sampleUser: 'NextGen Creator',
      sampleCaption: 'Top 5 Productivity Hacks (No Watermark)',
      fileSize: '4.10 MB',
      quality: 'Original HD MP4',
      sampleUrl: 'https://youtube.com/shorts/k92a01Xz',
    },
  }[platform];

  const Icon = config.icon;

  return (
    <div className="my-12 p-6 sm:p-10 rounded-[4px] bg-white dark:bg-[#0D1524] border border-slate-200 dark:border-[#263448] shadow-sm overflow-hidden">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
        {/* Left Column: Big Brand Emblem & Step-by-Step Interactive Guide */}
        <div className="lg:col-span-7 space-y-6">
          <div className="flex items-center gap-3">
            <div className="w-14 h-14 rounded-[3px] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] flex items-center justify-center shadow-xs">
              <Icon className="w-8 h-8 text-slate-800 dark:text-white" />
            </div>
            <div>
              <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-[2px] border ${config.badgeBg}`}>
                Live Workflow Demo
              </span>
              <h3 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-[#E8EDF5] mt-1">
                How Qwik Save Downloads in Seconds
              </h3>
            </div>
          </div>

          <p className="text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
            Watch the live simulation on the right. With zero installation, our stream extractor resolves any public {config.brand} link directly into an original high-definition MP4.
          </p>

          {/* Interactive Steps List */}
          <div className="space-y-3">
            {[
              {
                num: 1,
                title: 'Copy the Reel / Video Link',
                desc: `Open ${config.brand}, tap Share, and tap "Copy Link".`,
                icon: Share2,
              },
              {
                num: 2,
                title: 'Instant Extraction on Qwik Save',
                desc: 'Paste the link above; our engine detects resolution, streams, and file sizes.',
                icon: Zap,
              },
              {
                num: 3,
                title: 'One-Click Direct HD Download',
                desc: `Download ${config.quality} with exact MB calculation (${config.fileSize}) and zero watermark.`,
                icon: Download,
              },
            ].map((step) => {
              const StepIcon = step.icon;
              const isActive = currentStep === step.num;
              return (
                <div
                  key={step.num}
                  onClick={() => setCurrentStep(step.num as any)}
                  className={`p-3.5 rounded-[3px] border transition-all cursor-pointer flex items-start gap-3.5 ${
                    isActive
                      ? 'bg-slate-50 dark:bg-[#141D2B] border-[#1877F2] dark:border-[#1877F2] shadow-xs'
                      : 'bg-transparent border-transparent hover:border-slate-200 dark:hover:border-[#263448]'
                  }`}
                >
                  <div
                    className={`w-7 h-7 rounded-[2px] flex items-center justify-center text-xs font-bold shrink-0 transition-colors ${
                      isActive
                        ? 'bg-[#1877F2] text-white shadow-xs'
                        : 'bg-slate-200 dark:bg-[#1E293B] text-slate-600 dark:text-[#94A3B8]'
                    }`}
                  >
                    {step.num}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h4
                        className={`text-xs sm:text-sm font-bold ${
                          isActive
                            ? 'text-slate-900 dark:text-white'
                            : 'text-slate-700 dark:text-[#94A3B8]'
                        }`}
                      >
                        {step.title}
                      </h4>
                      {isActive && (
                        <span className="text-[10px] font-bold text-[#1877F2] flex items-center gap-1">
                          <span>Active</span>
                          <span className="w-1.5 h-1.5 rounded-full bg-[#1877F2] animate-ping" />
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">
                      {step.desc}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="flex items-center gap-4 text-xs font-medium text-slate-500 dark:text-[#94A3B8] pt-2">
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              100% Free
            </span>
            <span>•</span>
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              No Watermarks
            </span>
            <span>•</span>
            <span className="flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              Direct Gallery Save
            </span>
          </div>
        </div>

        {/* Right Column: Realistic Animated Smartphone Mockup */}
        <div className="lg:col-span-5 flex justify-center">
          <div className="relative w-72 sm:w-80 rounded-[36px] p-3 bg-slate-900 border-4 border-slate-800 shadow-2xl ring-1 ring-white/10">
            {/* Phone Speaker Notch / Dynamic Island */}
            <div className="w-24 h-4 bg-black rounded-full mx-auto mb-2 flex items-center justify-center">
              <div className="w-2 h-2 rounded-full bg-slate-800" />
            </div>

            {/* Phone Screen Display */}
            <div className="w-full h-[460px] rounded-[24px] bg-[#070B12] border border-slate-800 overflow-hidden flex flex-col justify-between text-white relative">
              {/* Top Status Bar */}
              <div className="px-5 pt-3 pb-2 flex items-center justify-between text-[11px] font-semibold text-slate-400 border-b border-slate-800/80">
                <span>9:41</span>
                <span className="font-mono text-[9px] uppercase tracking-wider text-slate-500">
                  {config.brand}
                </span>
                <div className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  <span className="text-[9px]">5G</span>
                </div>
              </div>

              {/* Step 1 Screen: App Feed & Link Copied */}
              {currentStep === 1 && (
                <div className="flex-1 p-4 flex flex-col justify-between animate-in fade-in duration-300">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-amber-500 via-rose-500 to-purple-600 p-0.5">
                        <div className="w-full h-full rounded-full bg-black flex items-center justify-center text-[10px] font-bold">
                          Q
                        </div>
                      </div>
                      <div>
                        <p className="text-xs font-bold leading-tight">{config.sampleUser}</p>
                        <p className="text-[9px] text-slate-400">Sponsored Reel</p>
                      </div>
                    </div>
                    <span className="text-xs text-slate-400">•••</span>
                  </div>

                  {/* Mock Video Playing Card */}
                  <div className="relative aspect-[9/12] rounded-[16px] bg-gradient-to-b from-slate-800 to-slate-900 border border-slate-700/60 overflow-hidden flex items-center justify-center shadow-lg my-2">
                    <div className="absolute inset-0 bg-radial from-white/10 to-transparent" />
                    <Film className="w-12 h-12 text-slate-500 opacity-60" />

                    {/* Floating Overlay: Copy Link Toast */}
                    <div className="absolute bottom-4 inset-x-4 bg-emerald-600/95 backdrop-blur-md text-white p-2.5 rounded-[8px] flex items-center justify-center gap-2 text-xs font-bold shadow-xl animate-bounce">
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Link Copied to Clipboard!</span>
                    </div>
                  </div>

                  <p className="text-[11px] text-slate-300 line-clamp-1">{config.sampleCaption}</p>
                </div>
              )}

              {/* Step 2 Screen: Qwik Save Input & Auto Paste */}
              {currentStep === 2 && (
                <div className="flex-1 p-4 flex flex-col justify-center space-y-4 animate-in fade-in duration-300">
                  <div className="text-center space-y-1">
                    <div className="w-10 h-10 rounded-[4px] bg-white text-slate-900 mx-auto flex items-center justify-center font-black text-sm shadow-md">
                      Q
                    </div>
                    <h4 className="font-bold text-sm">Qwik Save Downloader</h4>
                    <p className="text-[10px] text-slate-400">Save in One Click</p>
                  </div>

                  <div className="p-3 rounded-[8px] bg-slate-900 border border-slate-700 space-y-2">
                    <div className="flex items-center justify-between text-[10px] text-slate-400">
                      <span>Detected URL</span>
                      <span className="text-emerald-400 font-semibold">Pasted ✓</span>
                    </div>
                    <div className="p-2 rounded-[4px] bg-black/60 font-mono text-[10px] text-blue-400 truncate border border-slate-800">
                      {config.sampleUrl}
                    </div>
                    <button className="w-full py-2 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-[4px] text-xs font-bold flex items-center justify-center gap-1.5 shadow">
                      <Zap className="w-3.5 h-3.5 animate-spin" />
                      <span>Fetching Best Stream...</span>
                    </button>
                  </div>
                </div>
              )}

              {/* Step 3 Screen: Available Formats & Direct Download */}
              {currentStep === 3 && (
                <div className="flex-1 p-4 flex flex-col justify-between animate-in fade-in duration-300">
                  <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                    <span className="text-[10px] uppercase font-bold text-emerald-400">Ready to Save</span>
                    <span className="text-[10px] text-slate-400">{config.fileSize}</span>
                  </div>

                  <div className="p-3 rounded-[12px] bg-slate-900/90 border border-slate-700/80 space-y-3 shadow-xl">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-14 rounded-[6px] bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-400 shrink-0">
                        <Play className="w-5 h-5 fill-white text-white" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-bold truncate text-white">{config.sampleUser}</p>
                        <p className="text-[10px] text-emerald-400 font-semibold mt-0.5">{config.quality}</p>
                        <p className="text-[9px] text-slate-400">Original Bitrate • No Logo</p>
                      </div>
                    </div>

                    <a
                      href="#download"
                      className={`w-full py-2.5 rounded-[6px] bg-gradient-to-r ${config.btnGradient} text-white font-bold text-xs flex items-center justify-center gap-2 shadow-lg active:scale-95 transition`}
                    >
                      <Download className="w-4 h-4" />
                      <span>Download Video ({config.fileSize})</span>
                    </a>
                  </div>

                  <div className="p-2.5 rounded-[8px] bg-emerald-950/40 border border-emerald-500/30 text-center">
                    <p className="text-[10px] font-semibold text-emerald-300 flex items-center justify-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      Saved directly to Camera Roll!
                    </p>
                  </div>
                </div>
              )}

              {/* Bottom Phone Bar */}
              <div className="p-3 text-center border-t border-slate-800/80">
                <div className="flex items-center justify-center gap-1.5">
                  {[1, 2, 3].map((step) => (
                    <span
                      key={step}
                      className={`h-1.5 rounded-full transition-all duration-300 ${
                        currentStep === step ? 'w-6 bg-white' : 'w-1.5 bg-slate-700'
                      }`}
                    />
                  ))}
                </div>
                <div className="w-28 h-1 bg-slate-600 rounded-full mx-auto mt-2" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
