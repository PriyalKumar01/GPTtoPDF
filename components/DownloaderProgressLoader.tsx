'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Sparkles, CheckCircle2, ShieldCheck, Zap, Video } from 'lucide-react';

interface DownloaderProgressLoaderProps {
  platform: 'instagram' | 'facebook' | 'youtube' | 'audio' | 'universal';
  isComplete: boolean;
  onFinish?: () => void;
}

export function DownloaderProgressLoader({
  platform,
  isComplete,
  onFinish,
}: DownloaderProgressLoaderProps) {
  const [percent, setPercent] = useState(12);
  const [statusText, setStatusText] = useState('Connecting to media servers...');
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Platform styling
  const platformConfig = {
    instagram: {
      name: 'Instagram Reel & Video Engine',
      barGradient: 'from-[#833AB4] via-[#FD1D1D] to-[#FCAF45]',
      borderGlow: 'border-pink-500/30 dark:border-pink-500/20',
      badgeBg: 'bg-pink-500/10 text-pink-600 dark:text-pink-400 border-pink-500/20',
      accentColor: 'text-pink-600 dark:text-pink-400',
    },
    facebook: {
      name: 'Facebook Full HD Stream Engine',
      barGradient: 'from-[#1877F2] via-[#0C63D4] to-[#38BDF8]',
      borderGlow: 'border-blue-500/30 dark:border-blue-500/20',
      badgeBg: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
      accentColor: 'text-blue-600 dark:text-blue-400',
    },
    youtube: {
      name: 'YouTube & Universal Media Engine',
      barGradient: 'from-[#DC2626] via-[#EF4444] to-[#F97316]',
      borderGlow: 'border-red-500/30 dark:border-red-500/20',
      badgeBg: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20',
      accentColor: 'text-red-600 dark:text-red-400',
    },
    audio: {
      name: 'Audio Extractor Studio 320kbps',
      barGradient: 'from-[#6366F1] via-[#8B5CF6] to-[#EC4899]',
      borderGlow: 'border-indigo-500/30 dark:border-indigo-500/20',
      badgeBg: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border-indigo-500/20',
      accentColor: 'text-indigo-600 dark:text-indigo-400',
    },
    universal: {
      name: 'Universal Media Downloader',
      barGradient: 'from-blue-600 via-indigo-600 to-emerald-500',
      borderGlow: 'border-blue-500/30 dark:border-blue-500/20',
      badgeBg: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
      accentColor: 'text-blue-600 dark:text-blue-400',
    },
  }[platform];

  // Smart Realistic Progress Simulation:
  // Climbs quickly to 80-90% as requested by user, then inches 92% -> 95% -> 97% -> 98% -> 99%,
  // and upon completion immediately hits 100% and triggers finish.
  useEffect(() => {
    let current = 12;

    const interval = setInterval(() => {
      if (isComplete) return;

      if (current < 45) {
        current += Math.floor(Math.random() * 8) + 6; // Quick jump to 45%
      } else if (current < 85) {
        current += Math.floor(Math.random() * 6) + 4; // Swift climb to 85%
      } else if (current < 92) {
        current += 2; // Approaching 92%
      } else if (current < 95) {
        current += 1;
      } else if (current < 98) {
        // Slow tick between 95-98%
        if (Math.random() > 0.4) current += 1;
      } else if (current === 98) {
        // 99% cap until actual completion
        current = 99;
      }

      const capped = Math.min(99, current);
      setPercent(capped);

      // Status text updates based on progress
      if (capped < 40) {
        setStatusText('Analyzing URL & connecting to high-speed CDN...');
      } else if (capped < 75) {
        setStatusText('Resolving 1080p Full HD video streams & bitrate...');
      } else if (capped < 92) {
        setStatusText('Extracting audio track, tags & high-res thumbnail...');
      } else {
        setStatusText('Finalizing download manifest & packaging streams...');
      }
    }, 180);

    return () => clearInterval(interval);
  }, [isComplete]);

  // When fetch completes, jump to 100% and gracefully transition out
  useEffect(() => {
    if (isComplete) {
      setPercent(100);
      setStatusText('Media extracted successfully! Rendering download links...');
      const timer = setTimeout(() => {
        if (onFinish) onFinish();
      }, 350);
      return () => clearTimeout(timer);
    }
  }, [isComplete, onFinish]);

  return (
    <div
      className={`w-full my-6 p-6 sm:p-7 rounded-xl bg-white/95 dark:bg-[#0C1424]/95 border ${platformConfig.borderGlow} shadow-xl backdrop-blur-xl relative overflow-hidden transition-all duration-300 animate-in fade-in zoom-in-95`}
    >
      {/* Decorative ambient background glow */}
      <div className="absolute top-0 right-1/4 w-60 h-32 bg-blue-500/10 dark:bg-blue-600/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-60 h-32 bg-purple-500/10 dark:bg-purple-600/15 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col items-center justify-center text-center space-y-4 relative z-10">
        {/* Top Header Pill */}
        <div className="flex items-center gap-2">
          <div
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${platformConfig.badgeBg}`}
          >
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-current opacity-75" />
              <span className="relative inline-flex rounded-full h-2 w-2 bg-current" />
            </span>
            <span>{platformConfig.name}</span>
          </div>
        </div>

        {/* Large Prominent Percentage Counter */}
        <div className="flex items-baseline justify-center gap-1">
          <span className="text-4xl sm:text-5xl font-black tracking-tight text-slate-900 dark:text-white font-mono">
            {percent}
          </span>
          <span className={`text-2xl sm:text-3xl font-extrabold ${platformConfig.accentColor}`}>
            %
          </span>
        </div>

        {/* Dynamic Status Text */}
        <div className="space-y-1">
          <p className="text-sm sm:text-base font-bold text-slate-800 dark:text-[#F1F5F9] transition-all duration-200">
            {statusText}
          </p>
          <p className="text-[11px] sm:text-xs text-slate-500 dark:text-[#94A3B8]">
            Original quality preserved • No compression • 100% Free
          </p>
        </div>

        {/* Shimmering Animated Progress Bar */}
        <div className="w-full max-w-md space-y-2">
          <div className="w-full h-3 bg-slate-100 dark:bg-[#141D2E] rounded-full overflow-hidden p-0.5 border border-slate-200 dark:border-[#223552] shadow-inner relative">
            <div
              className={`h-full rounded-full bg-gradient-to-r ${platformConfig.barGradient} transition-all duration-200 ease-out shadow-sm relative overflow-hidden`}
              style={{ width: `${percent}%` }}
            >
              {/* Animated specular highlight scanline */}
              <div className="absolute inset-0 bg-white/25 -skew-x-12 animate-[shimmer_1.5s_infinite]" />
            </div>
          </div>

          {/* Stepped progress indicators */}
          <div className="flex items-center justify-between text-[10px] text-slate-400 dark:text-slate-500 font-mono px-1">
            <span className={percent >= 25 ? 'text-slate-800 dark:text-slate-200 font-semibold' : ''}>
              Connect
            </span>
            <span className={percent >= 60 ? 'text-slate-800 dark:text-slate-200 font-semibold' : ''}>
              Resolve 1080p
            </span>
            <span className={percent >= 90 ? 'text-slate-800 dark:text-slate-200 font-semibold' : ''}>
              Extract Audio
            </span>
            <span className={percent === 100 ? 'text-emerald-600 dark:text-emerald-400 font-bold' : ''}>
              Ready ✓
            </span>
          </div>
        </div>

        {/* Trust Badges */}
        <div className="pt-2 flex flex-wrap items-center justify-center gap-3 text-[11px] text-slate-500 dark:text-[#94A3B8]">
          <span className="inline-flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
            Zero Watermark
          </span>
          <span className="inline-flex items-center gap-1">
            <Zap className="w-3.5 h-3.5 text-amber-500" />
            Fast Stream Fetch
          </span>
          <span className="inline-flex items-center gap-1">
            <Video className="w-3.5 h-3.5 text-blue-500" />
            Up to 4K Ultra HD
          </span>
        </div>
      </div>
    </div>
  );
}
