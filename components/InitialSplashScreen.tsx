'use client';

import React, { useEffect, useState } from 'react';

export function InitialSplashScreen() {
  const [visible, setVisible] = useState(true);
  const [fading, setFading] = useState(false);
  const [activeDot, setActiveDot] = useState(0);

  useEffect(() => {
    // Check if splash was already shown in this session
    try {
      const alreadyShown = window.sessionStorage.getItem('qwik_splash_viewed');
      if (alreadyShown === 'true') {
        setVisible(false);
        return;
      }
    } catch {}

    // Dot cycling wave animation (travels left to right smoothly)
    const dotInterval = setInterval(() => {
      setActiveDot((prev) => (prev + 1) % 7);
    }, 160);

    // Snappy professional timing: visible for ~1.6s, then clean fade-out
    const fadeTimer = setTimeout(() => {
      setFading(true);
    }, 1500);

    const hideTimer = setTimeout(() => {
      setVisible(false);
      try {
        window.sessionStorage.setItem('qwik_splash_viewed', 'true');
      } catch {}
    }, 1900);

    return () => {
      clearInterval(dotInterval);
      clearTimeout(fadeTimer);
      clearTimeout(hideTimer);
    };
  }, []);

  if (!visible) return null;

  return (
    <div
      className={`fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-white dark:bg-[#070B12] transition-opacity duration-300 ease-in-out ${
        fading ? 'opacity-0 pointer-events-none' : 'opacity-100'
      }`}
      aria-label="Loading Qwik Save Suite"
    >
      <div className="flex flex-col items-center max-w-sm px-6 text-center animate-in fade-in zoom-in-95 duration-250">
        {/* Seamless Brand Banner - Pure White Blend (No rectangular border or edge artifact) */}
        <div className="relative mb-6 flex items-center justify-center">
          <img
            src="/images/qwik-save-banner.png"
            alt="Qwik Save - Save in One Click"
            className="w-56 sm:w-64 h-auto object-contain select-none mix-blend-multiply dark:mix-blend-normal"
          />
        </div>

        {/* Professional 7-Dot Traveling Color Wave Loader */}
        <div className="flex items-center justify-center gap-2.5 my-3">
          {[0, 1, 2, 3, 4, 5, 6].map((i) => {
            const isActive = activeDot === i;
            const isNear = (activeDot + 6) % 7 === i;
            return (
              <span
                key={i}
                className={`w-2.5 h-2.5 rounded-full transition-all duration-200 ease-out ${
                  isActive
                    ? 'bg-[#1877F2] scale-125 shadow-[0_0_12px_rgba(24,119,242,0.9)]'
                    : isNear
                    ? 'bg-[#00B4D8] scale-110 opacity-75'
                    : 'bg-slate-200 dark:bg-slate-800 scale-90 opacity-40'
                }`}
              />
            );
          })}
        </div>

        {/* Minimalist Subtext */}
        <p className="text-[11px] font-medium tracking-wider text-slate-400 dark:text-[#64748B] mt-1 font-mono">
          Save in One Click
        </p>
      </div>
    </div>
  );
}
