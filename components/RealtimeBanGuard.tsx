'use client';

import React, { useState, useEffect } from 'react';
import { ShieldAlert, AlertTriangle, LogOut } from 'lucide-react';

export function RealtimeBanGuard() {
  const [bannedData, setBannedData] = useState<{
    isBanned: boolean;
    reason: string;
    email: string;
  } | null>(null);

  useEffect(() => {
    const checkBanStatus = async () => {
      try {
        const storedEmail =
          localStorage.getItem('qs_user_email') ||
          localStorage.getItem('user_email') ||
          sessionStorage.getItem('qs_user_email') ||
          '';

        if (!storedEmail.trim()) return;

        // Skip master owner from ban checks
        if (storedEmail.toLowerCase().trim() === 'priyalkumar01@gmail.com') return;

        const res = await fetch(
          `/api/auth/check-ban?email=${encodeURIComponent(storedEmail.trim())}`,
          { cache: 'no-store' }
        );
        const data = await res.json();

        if (data.isBanned) {
          // Immediately wipe auth session
          localStorage.removeItem('qs_user_email');
          localStorage.removeItem('qs_user_name');
          localStorage.removeItem('user_email');
          sessionStorage.removeItem('qs_user_email');

          setBannedData({
            isBanned: true,
            reason:
              data.reason ||
              'Violation of Qwik Save Terms & Conditions (e.g. falsified account information, unauthorized automated scraping, or policy breach).',
            email: storedEmail,
          });
        }
      } catch (err) {
        // Silently catch network failures so offline browsing remains smooth
      }
    };

    // Initial check on mount
    checkBanStatus();

    // Fast polling interval (every 5 seconds) for real-time ban enforcement
    const interval = setInterval(checkBanStatus, 5000);

    // Also check on tab focus
    window.addEventListener('focus', checkBanStatus);

    return () => {
      clearInterval(interval);
      window.removeEventListener('focus', checkBanStatus);
    };
  }, []);

  if (!bannedData || !bannedData.isBanned) {
    return null;
  }

  const handleDismiss = () => {
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = '/terms';
  };

  return (
    <div className="fixed inset-0 z-[999999] bg-slate-950/95 backdrop-blur-xl flex items-center justify-center p-4">
      <div className="w-full max-w-lg bg-slate-900 border-2 border-red-500/40 rounded-3xl p-8 shadow-2xl text-center relative overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="absolute -top-20 -left-20 w-48 h-48 bg-red-500/20 rounded-full blur-3xl pointer-events-none" />

        <div className="w-16 h-16 mx-auto mb-5 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400 shadow-lg shadow-red-500/20">
          <ShieldAlert className="w-9 h-9" />
        </div>

        <span className="inline-block px-3 py-1 rounded-full bg-red-500/20 text-red-300 font-bold text-xs uppercase tracking-wider border border-red-500/30 mb-3">
          Access Suspended
        </span>

        <h2 className="text-2xl font-black text-white tracking-tight mb-2">
          Account Suspended
        </h2>

        <p className="text-xs text-slate-300 leading-relaxed mb-4">
          The account registered with{' '}
          <strong className="text-red-400 font-mono">{bannedData.email}</strong> has
          been suspended by platform administration.
        </p>

        <div className="p-4 rounded-2xl bg-red-950/40 border border-red-500/30 text-left mb-6 space-y-1.5">
          <div className="flex items-center gap-2 text-red-400 text-xs font-bold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>Reason for Suspension:</span>
          </div>
          <p className="text-xs text-red-200/90 leading-relaxed pl-6">
            {bannedData.reason}
          </p>
        </div>

        <p className="text-[11px] text-slate-400 leading-relaxed mb-6">
          In accordance with Qwik Save’s Terms of Service, all users are required to provide
          truthful profile information and adhere to acceptable use guidelines.
        </p>

        <button
          onClick={handleDismiss}
          className="w-full py-3.5 px-5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-sm shadow-lg shadow-red-600/30 transition-all flex items-center justify-center gap-2"
        >
          <LogOut className="w-4 h-4" />
          Acknowledge & Exit to Terms
        </button>
      </div>
    </div>
  );
}
