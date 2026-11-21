'use client';

import React, { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';

function AuthCallbackContent() {
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState('');
  const [userData, setUserData] = useState<{ name: string; email: string; provider: string } | null>(null);
  const [deepLinkUrl, setDeepLinkUrl] = useState('');

  useEffect(() => {
    const code = searchParams.get('code');
    const error = searchParams.get('error');
    const errorDescription = searchParams.get('error_description');
    const provider = searchParams.get('provider') || searchParams.get('state') || 'google';

    if (error) {
      setStatus('error');
      setErrorMessage(errorDescription || error || 'Authentication was denied or cancelled.');
      return;
    }

    if (!code) {
      setStatus('error');
      setErrorMessage('No authorization code was received.');
      return;
    }

    const currentOrigin = window.location.origin;
    // Standard OAuth 2.0 requires exact match with the registered authorization callback URI
    const redirectUri = `${currentOrigin}/auth/callback`;

    const storedName = typeof window !== 'undefined' ? (sessionStorage.getItem('qs_reg_name') || localStorage.getItem('qs_reg_name') || '') : '';
    const storedAge = typeof window !== 'undefined' ? (sessionStorage.getItem('qs_reg_age') || localStorage.getItem('qs_reg_age') || '') : '';

    fetch('/api/auth', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        provider,
        code,
        redirectUri,
        name: storedName,
        age: storedAge,
      }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success && data.user) {
          setStatus('success');
          setUserData({
            name: data.user.name,
            email: data.user.email,
            provider: data.provider || provider,
          });

          const deepLink =
            data.deepLink ||
            `qwiksave://auth/callback?name=${encodeURIComponent(data.user.name)}&email=${encodeURIComponent(
              data.user.email
            )}&provider=${encodeURIComponent(data.provider || provider)}`;

          setDeepLinkUrl(deepLink);

          // Auto-trigger Android deep link
          setTimeout(() => {
            window.location.href = deepLink;
          }, 400);
        } else {
          setStatus('error');
          setErrorMessage(data.error || 'Failed to verify account credentials.');
        }
      })
      .catch((err) => {
        setStatus('error');
        setErrorMessage(err.message || 'Connection error during authentication.');
      });
  }, [searchParams]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 text-white px-4">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-2xl text-center">
        {/* App Logo */}
        <div className="flex justify-center mb-6">
          <img
            src="/app-logo.png"
            alt="Qwik Save"
            className="w-20 h-20 rounded-2xl shadow-lg shadow-sky-500/20 object-contain"
          />
        </div>

        {status === 'loading' && (
          <div className="space-y-4">
            <div className="inline-block w-10 h-10 border-4 border-sky-400 border-t-transparent rounded-full animate-spin" />
            <h2 className="text-xl font-bold text-white">Verifying Account...</h2>
            <p className="text-sm text-slate-400">
              Please wait while we secure your authenticated session.
            </p>
          </div>
        )}

        {status === 'success' && (
          <div className="space-y-5 animate-in fade-in duration-300">
            <div className="w-14 h-14 bg-emerald-500/10 border border-emerald-500 text-emerald-400 rounded-full flex items-center justify-center mx-auto text-2xl">
              ✓
            </div>
            <div>
              <h2 className="text-2xl font-black text-white">Authentication Verified!</h2>
              <p className="text-sm text-slate-400 mt-1">
                Welcome, <span className="text-sky-400 font-semibold">{userData?.name}</span> ({userData?.email})
              </p>
            </div>

            <div className="bg-slate-800/60 rounded-xl p-4 border border-slate-700/60 text-xs text-slate-300">
              Redirecting back to Qwik Save app...
            </div>

            {deepLinkUrl && (
              <a
                href={deepLinkUrl}
                className="block w-full py-3.5 px-4 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-bold rounded-xl shadow-lg transition-all"
              >
                Open Qwik Save App 🚀
              </a>
            )}
          </div>
        )}

        {status === 'error' && (
          <div className="space-y-4 animate-in fade-in duration-300">
            <div className="w-14 h-14 bg-rose-500/10 border border-rose-500 text-rose-400 rounded-full flex items-center justify-center mx-auto text-2xl">
              ✕
            </div>
            <h2 className="text-xl font-bold text-white">Authentication Failed</h2>
            <p className="text-sm text-rose-300/90">{errorMessage}</p>

            <a
              href="/"
              className="inline-block mt-4 text-xs font-semibold text-sky-400 hover:underline"
            >
              ← Return to Qwik Save Home
            </a>
          </div>
        )}
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-slate-950 text-white">
          <div className="inline-block w-8 h-8 border-4 border-sky-400 border-t-transparent rounded-full animate-spin" />
        </div>
      }
    >
      <AuthCallbackContent />
    </Suspense>
  );
}
