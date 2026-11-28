'use client';

import React, { useEffect, useState } from 'react';
import { validateUserEmail } from '@/lib/email-validator';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

export function InitialSplashScreen() {
  const [visible, setVisible] = useState(true);
  const [fading, setFading] = useState(false);
  const [activeDot, setActiveDot] = useState(0);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

  // Form states for non-authenticated users
  const [loadingProvider, setLoadingProvider] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [showEmailForm, setShowEmailForm] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [emailFeedback, setEmailFeedback] = useState<{
    valid: boolean;
    reason?: string;
    isEducational?: boolean;
  } | null>(null);

  useEffect(() => {
    // Check if user is already authenticated
    const storedEmail =
      (typeof window !== 'undefined'
        ? localStorage.getItem('qs_user_email') ||
          localStorage.getItem('user_email')
        : null) || '';

    if (storedEmail.trim()) {
      setIsAuthenticated(true);
      // Auto fade-out after 1.5s for logged-in users
      const fadeTimer = setTimeout(() => {
        setFading(true);
      }, 1500);

      const hideTimer = setTimeout(() => {
        setVisible(false);
      }, 1900);

      return () => {
        clearTimeout(fadeTimer);
        clearTimeout(hideTimer);
      };
    } else {
      setIsAuthenticated(false);
    }
  }, []);

  useEffect(() => {
    // Dot cycling wave animation (travels left to right smoothly)
    const dotInterval = setInterval(() => {
      setActiveDot((prev) => (prev + 1) % 7);
    }, 160);

    return () => clearInterval(dotInterval);
  }, []);

  const initiateOAuth = async (provider: 'google' | 'github' | 'twitter') => {
    try {
      setLoadingProvider(provider);
      setErrorMessage('');

      if (typeof window !== 'undefined') {
        sessionStorage.setItem('qs_auth_redirect', window.location.pathname + window.location.search);
      }

      const res = await fetch(`/api/auth?provider=${provider}`);
      const data = await res.json();

      if (data.success && data.authUrl) {
        window.location.href = data.authUrl;
      } else {
        setErrorMessage(data.error || `Failed to initiate ${provider} sign-in.`);
        setLoadingProvider(null);
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Network error connecting to auth server.');
      setLoadingProvider(null);
    }
  };

  const handleEmailChange = (val: string) => {
    setEmail(val);
    setErrorMessage('');
    if (!val.trim()) {
      setEmailFeedback(null);
      return;
    }
    if (val.includes('@') && val.includes('.')) {
      const result = validateUserEmail(val);
      setEmailFeedback(result);
    } else {
      setEmailFeedback(null);
    }
  };

  const handleDirectEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (!name.trim()) {
      setErrorMessage('Please enter your full name.');
      return;
    }
    if (!email.trim()) {
      setErrorMessage('Please enter your email address.');
      return;
    }

    const check = validateUserEmail(email);
    if (!check.valid) {
      setErrorMessage(check.reason || 'Invalid email address.');
      return;
    }

    try {
      setLoadingProvider('email');
      const res = await fetch('/api/auth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          provider: 'email',
          name: name.trim(),
          email: email.trim().toLowerCase(),
        }),
      });

      const data = await res.json();

      if (data.success && data.user) {
        localStorage.setItem('qs_user_id', data.user.id);
        localStorage.setItem('qs_user_name', data.user.name);
        localStorage.setItem('qs_user_email', data.user.email);
        localStorage.setItem('qs_user_provider', 'email');
        localStorage.setItem('user_email', data.user.email);
        localStorage.setItem('user_name', data.user.name);

        window.dispatchEvent(new Event('qs_auth_changed'));
        setFading(true);
        setTimeout(() => setVisible(false), 400);
      } else {
        setErrorMessage(data.error || 'Authentication failed. Please verify credentials.');
        setLoadingProvider(null);
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Network error during sign-in.');
      setLoadingProvider(null);
    }
  };

  if (!visible) return null;

  return (
    <div
      className={`fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-white dark:bg-[#070B12] transition-opacity duration-300 ease-in-out p-4 overflow-y-auto ${
        fading ? 'opacity-0 pointer-events-none' : 'opacity-100'
      }`}
      aria-label="Qwik Save Suite Authentication"
    >
      <div className="flex flex-col items-center w-full max-w-sm px-4 text-center my-auto animate-in fade-in zoom-in-95 duration-250">
        {/* Seamless Brand Banner */}
        <div className="relative mb-4 flex flex-col items-center justify-center">
          <img
            src="/images/qwik-save-banner.png"
            alt="Qwik Save - Save in One Click"
            className="w-56 sm:w-64 h-auto object-contain select-none mix-blend-multiply dark:mix-blend-normal"
          />
        </div>

        {/* Professional 7-Dot Traveling Color Wave Loader */}
        <div className="flex items-center justify-center gap-2.5 my-2">
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
        <p className="text-[11px] font-medium tracking-wider text-slate-400 dark:text-[#64748B] mb-5 font-mono">
          Save in One Click
        </p>

        {/* If user is not yet logged in: Show the clean 3 OAuth buttons & verified email entry */}
        {isAuthenticated === false && (
          <div className="w-full bg-slate-50 dark:bg-[#0B111E] border border-slate-200 dark:border-[#1E293B] rounded-2xl p-5 shadow-xl text-left animate-in fade-in duration-300">
            <div className="text-center mb-4">
              <h2 className="text-sm font-bold text-slate-800 dark:text-slate-100 tracking-tight">
                Sign In to Enter
              </h2>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                100% verified access. Select your login method:
              </p>
            </div>

            {errorMessage && (
              <div className="mb-3 p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-400 text-xs flex items-start gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="leading-snug">{errorMessage}</span>
              </div>
            )}

            {/* 3 OAuth Buttons */}
            <div className="space-y-2 mb-3">
              {/* Google */}
              <button
                type="button"
                onClick={() => initiateOAuth('google')}
                disabled={loadingProvider !== null}
                className="w-full py-2.5 px-3 rounded-xl border border-slate-300 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 bg-white dark:bg-slate-800/90 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-100 font-semibold text-xs shadow-sm transition-all flex items-center justify-center gap-2.5 disabled:opacity-50"
              >
                {loadingProvider === 'google' ? (
                  <div className="w-4 h-4 border-2 border-sky-500 border-t-transparent rounded-full animate-spin" />
                ) : (
                  <svg className="w-4 h-4" viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                )}
                <span>Continue with Google</span>
              </button>

              {/* GitHub */}
              <button
                type="button"
                onClick={() => initiateOAuth('github')}
                disabled={loadingProvider !== null}
                className="w-full py-2.5 px-3 rounded-xl border border-slate-300 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 bg-white dark:bg-slate-800/90 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-100 font-semibold text-xs shadow-sm transition-all flex items-center justify-center gap-2.5 disabled:opacity-50"
              >
                {loadingProvider === 'github' ? (
                  <div className="w-4 h-4 border-2 border-slate-500 border-t-transparent rounded-full animate-spin" />
                ) : (
                  <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                    <path
                      fillRule="evenodd"
                      clipRule="evenodd"
                      d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                    />
                  </svg>
                )}
                <span>Continue with GitHub</span>
              </button>

              {/* X */}
              <button
                type="button"
                onClick={() => initiateOAuth('twitter')}
                disabled={loadingProvider !== null}
                className="w-full py-2.5 px-3 rounded-xl border border-slate-300 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 bg-white dark:bg-slate-800/90 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-100 font-semibold text-xs shadow-sm transition-all flex items-center justify-center gap-2.5 disabled:opacity-50"
              >
                {loadingProvider === 'twitter' ? (
                  <div className="w-4 h-4 border-2 border-slate-500 border-t-transparent rounded-full animate-spin" />
                ) : (
                  <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                    <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                  </svg>
                )}
                <span>Continue with X</span>
              </button>
            </div>

            {/* Toggle Direct Email Verification Form */}
            <div className="text-center pt-2 border-t border-slate-200 dark:border-slate-800">
              {!showEmailForm ? (
                <button
                  type="button"
                  onClick={() => setShowEmailForm(true)}
                  className="text-[11px] text-sky-600 dark:text-sky-400 hover:underline font-medium"
                >
                  Or enter with verified email / College ID
                </button>
              ) : (
                <form onSubmit={handleDirectEmailSubmit} className="space-y-2 mt-2 text-left">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase">
                      Full Name
                    </label>
                    <input
                      type="text"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="e.g. Priyal Kumar"
                      required
                      className="w-full px-2.5 py-1.5 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-sky-500"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase">
                      Email Address
                    </label>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => handleEmailChange(e.target.value)}
                      placeholder="e.g. name@gmail.com or student@college.edu"
                      required
                      className="w-full px-2.5 py-1.5 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-sky-500"
                    />
                    {emailFeedback && (
                      <div className="mt-1 text-[10px] flex items-center gap-1 text-emerald-600 dark:text-emerald-400">
                        <CheckCircle2 className="w-3 h-3" />
                        <span>Verified {emailFeedback.isEducational ? 'College / University' : 'Email'}</span>
                      </div>
                    )}
                  </div>
                  <button
                    type="submit"
                    disabled={loadingProvider !== null}
                    className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition-all disabled:opacity-50"
                  >
                    {loadingProvider === 'email' ? 'Verifying...' : 'Verify & Enter'}
                  </button>
                </form>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
