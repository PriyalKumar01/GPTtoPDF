'use client';

import React, { useState } from 'react';
import {
  X,
  ShieldCheck,
  Mail,
  User,
  AlertCircle,
  GraduationCap,
  CheckCircle2,
  Lock,
} from 'lucide-react';
import { validateUserEmail } from '@/lib/email-validator';
import { UserSession } from '@/lib/use-auth';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (user: UserSession) => void;
  title?: string;
  subtitle?: string;
}

export function AuthModal({
  isOpen,
  onClose,
  onSuccess,
  title = 'Sign In to Qwik Save',
  subtitle = 'Authentication is required to use Qwik Save tools and download content.',
}: AuthModalProps) {
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [age, setAge] = useState('');
  const [loadingProvider, setLoadingProvider] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [emailFeedback, setEmailFeedback] = useState<{
    valid: boolean;
    reason?: string;
    isEducational?: boolean;
  } | null>(null);

  if (!isOpen) return null;

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

  const initiateOAuth = async (provider: 'google' | 'github' | 'twitter') => {
    try {
      setLoadingProvider(provider);
      setErrorMessage('');

      // Remember where the user was
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
          age: age ? parseInt(age, 10) : undefined,
        }),
      });

      const data = await res.json();

      if (data.success && data.user) {
        // Save session in browser
        if (typeof window !== 'undefined') {
          localStorage.setItem('qs_user_id', data.user.id || '');
          localStorage.setItem('qs_user_name', data.user.name);
          localStorage.setItem('qs_user_email', data.user.email);
          localStorage.setItem('qs_user_provider', data.provider || 'email');
          if (data.user.age) {
            localStorage.setItem('qs_user_age', String(data.user.age));
          }
          window.dispatchEvent(new Event('qs_auth_changed'));
        }

        if (onSuccess) {
          onSuccess({
            id: data.user.id,
            name: data.user.name,
            email: data.user.email,
            provider: data.provider || 'email',
            age: data.user.age,
          });
        }
        onClose();
      } else {
        setErrorMessage(data.error || 'Failed to authenticate.');
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Connection error. Please try again.');
    } finally {
      setLoadingProvider(null);
    }
  };

  return (
    <div className="fixed inset-0 z-[99999] bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
      <div className="w-full max-w-md bg-white dark:bg-[#0B111E] border border-slate-200 dark:border-[#1E293B] rounded-2xl p-6 sm:p-7 shadow-2xl relative text-slate-900 dark:text-white animate-in fade-in zoom-in-95 duration-200 my-8">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          aria-label="Close modal"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Brand Header */}
        <div className="flex items-center gap-3 mb-4">
          <img
            src="/app-logo.png"
            alt="Qwik Save"
            className="w-11 h-11 rounded-xl shadow-md object-contain border border-slate-200 dark:border-slate-800"
          />
          <div>
            <h2 className="text-lg font-black tracking-tight text-slate-900 dark:text-white">
              {title}
            </h2>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              {subtitle}
            </p>
          </div>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-600 dark:text-rose-400 text-xs flex items-start gap-2 animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <div className="leading-relaxed">{errorMessage}</div>
          </div>
        )}

        {/* 3 OAuth Providers */}
        <div className="space-y-2.5 mb-5">
          {/* Google */}
          <button
            type="button"
            onClick={() => initiateOAuth('google')}
            disabled={loadingProvider !== null}
            className="w-full py-2.5 px-4 rounded-xl border border-slate-300 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 bg-white dark:bg-slate-800/80 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-100 font-semibold text-xs shadow-sm transition-all flex items-center justify-center gap-3 disabled:opacity-50"
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
            className="w-full py-2.5 px-4 rounded-xl border border-slate-300 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 bg-white dark:bg-slate-800/80 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-100 font-semibold text-xs shadow-sm transition-all flex items-center justify-center gap-3 disabled:opacity-50"
          >
            {loadingProvider === 'github' ? (
              <div className="w-4 h-4 border-2 border-slate-500 border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
              </svg>
            )}
            <span>Continue with GitHub</span>
          </button>

          {/* X (Twitter) */}
          <button
            type="button"
            onClick={() => initiateOAuth('twitter')}
            disabled={loadingProvider !== null}
            className="w-full py-2.5 px-4 rounded-xl border border-slate-300 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 bg-white dark:bg-slate-800/80 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-100 font-semibold text-xs shadow-sm transition-all flex items-center justify-center gap-3 disabled:opacity-50"
          >
            {loadingProvider === 'twitter' ? (
              <div className="w-4 h-4 border-2 border-slate-500 border-t-transparent rounded-full animate-spin" />
            ) : (
              <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
              </svg>
            )}
            <span>Continue with X (Twitter)</span>
          </button>
        </div>

        {/* Divider */}
        <div className="relative flex items-center justify-center mb-5">
          <div className="border-t border-slate-200 dark:border-slate-800 w-full" />
          <span className="bg-white dark:bg-[#0B111E] px-2.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 shrink-0">
            Or Verified / College Email
          </span>
          <div className="border-t border-slate-200 dark:border-slate-800 w-full" />
        </div>

        {/* Direct Email Form */}
        <form onSubmit={handleDirectEmailSubmit} className="space-y-3">
          <div>
            <label className="block text-[11px] font-semibold text-slate-600 dark:text-slate-300 mb-1">
              Your Full Name <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <User className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Priyal Kumar"
                className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
              />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-[11px] font-semibold text-slate-600 dark:text-slate-300">
                Official Email (or College ID) <span className="text-rose-500">*</span>
              </label>
              {emailFeedback?.isEducational && (
                <span className="text-[10px] font-bold text-emerald-500 flex items-center gap-1">
                  <GraduationCap className="w-3 h-3" /> College Verified
                </span>
              )}
              {emailFeedback?.valid && !emailFeedback?.isEducational && (
                <span className="text-[10px] font-bold text-sky-500 flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Verified Address
                </span>
              )}
            </div>
            <div className="relative">
              <Mail className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => handleEmailChange(e.target.value)}
                placeholder="student@college.edu or name@gmail.com"
                className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
              />
            </div>

            {emailFeedback && !emailFeedback.valid && (
              <p className="mt-1 text-[10px] text-rose-500 font-medium">
                {emailFeedback.reason}
              </p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 pt-1">
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 dark:text-slate-300 mb-1">
                Age (Optional)
              </label>
              <input
                type="number"
                min="10"
                max="100"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                placeholder="e.g. 21"
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
              />
            </div>
            <div className="flex flex-col justify-end">
              <button
                type="submit"
                disabled={loadingProvider !== null}
                className="w-full py-2 px-3 bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-1.5 disabled:opacity-50"
              >
                {loadingProvider === 'email' ? (
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <Lock className="w-3 h-3" />
                    <span>Enter Tools</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </form>

        {/* Security & Spam Prevention Badge */}
        <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-800/80 flex items-center gap-2 text-[10px] text-slate-500 dark:text-slate-400">
          <ShieldCheck className="w-4 h-4 text-emerald-500 shrink-0" />
          <span>
            Strict zero-spam policy: Temp & disposable emails are disallowed. Live sync with Supabase security.
          </span>
        </div>
      </div>
    </div>
  );
}
