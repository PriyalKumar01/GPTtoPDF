'use client';

import React, { useState, useRef } from 'react';
import {
  Link as LinkIcon,
  Download,
  Clipboard,
  AlertCircle,
  CheckCircle2,
  FileCode,
  Sparkles,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import {
  ChatMessage,
  ConversationData,
  ExtractionProgress,
} from '@/lib/types';
import {
  isValidAiShareUrl,
  identifyPlatform,
  calculateAccurateEstimatedPages,
} from '@/lib/extract-ai';
import { CuteLoader } from './CuteLoader';

interface ConverterHeroProps {
  onConversationReady: (conversation: ConversationData) => void;
  isProcessing: boolean;
  setIsProcessing: (loading: boolean) => void;
}

export function ConverterHero({
  onConversationReady,
  isProcessing,
  setIsProcessing,
}: ConverterHeroProps) {
  const [urlInput, setUrlInput] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'url' | 'direct'>('url');
  const [directText, setDirectText] = useState('');
  const [directTitle, setDirectTitle] = useState('My AI Study Notes');

  const [progress, setProgress] = useState<ExtractionProgress>({
    step: 'idle',
    message: '',
    percent: 0,
  });

  const inputRef = useRef<HTMLInputElement>(null);

  // Quick paste from clipboard
  const handlePasteClipboard = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setUrlInput(text.trim());
        setErrorMsg(null);
      }
    } catch {
      inputRef.current?.focus();
    }
  };

  // Convert via URL endpoint
  const handleConvertUrl = async (targetUrl?: string) => {
    const rawUrl = (targetUrl || urlInput).trim();
    setErrorMsg(null);

    if (!rawUrl) {
      setErrorMsg('Please paste a ChatGPT, Claude, Gemini, or AI shared conversation URL.');
      return;
    }

    if (!isValidAiShareUrl(rawUrl)) {
      setErrorMsg(
        'Please enter a valid shared link from ChatGPT (chatgpt.com/share/...), Claude (claude.ai/share/...), Google Gemini, or Perplexity.'
      );
      return;
    }

    const platform = identifyPlatform(rawUrl);
    setIsProcessing(true);

    try {
      // Step 1: Validating
      setProgress({
        step: 'validating',
        message: `Validating ${platform.toUpperCase()} shared URL format...`,
        percent: 20,
      });
      await new Promise((r) => setTimeout(r, 200));

      // Step 2: Fetching
      setProgress({
        step: 'fetching',
        message: `Connecting to ${platform.toUpperCase()} share service...`,
        percent: 45,
      });

      const response = await fetch('/api/extract', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: rawUrl }),
      });

      const data = await response.json();

      // Step 3: Extracting
      setProgress({
        step: 'extracting',
        message: 'Extracting messages, tables, and code blocks...',
        percent: 75,
      });
      await new Promise((r) => setTimeout(r, 200));

      if (!response.ok || !data.success) {
        throw new Error(
          data.error ||
            'Could not extract conversation. The link may be private or protected by a bot challenge.'
        );
      }

      // Step 4: Preparing document
      setProgress({
        step: 'preparing',
        message: 'Structuring document and formatting academic notes layout...',
        percent: 95,
      });
      await new Promise((r) => setTimeout(r, 150));

      setProgress({
        step: 'ready',
        message: 'Document ready for preview and PDF download!',
        percent: 100,
      });

      onConversationReady(data.conversation);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to convert conversation.');
      setProgress({
        step: 'error',
        message: err.message || 'Conversion failed.',
        percent: 0,
      });
    } finally {
      setIsProcessing(false);
    }
  };

  // Convert via Direct Pasted Text/Markdown
  const handleConvertDirect = () => {
    setErrorMsg(null);
    if (!directText.trim()) {
      setErrorMsg('Please paste your conversation text or markdown notes.');
      return;
    }

    setIsProcessing(true);
    setProgress({
      step: 'extracting',
      message: 'Parsing conversation sections...',
      percent: 50,
    });

    try {
      const raw = directText.trim();
      const messages: ChatMessage[] = [];

      const pattern = /(?:^|\n)(You|ChatGPT|Claude|Gemini|User|Assistant)(?::|\s+said:)?\s*\n/gi;
      const matches = [...raw.matchAll(pattern)];

      if (matches.length >= 2) {
        for (let i = 0; i < matches.length; i++) {
          const match = matches[i];
          const rawRole = match[1].toLowerCase();
          const role =
            rawRole.includes('you') || rawRole.includes('user')
              ? 'user'
              : 'assistant';
          const authorName = role === 'user' ? 'You' : 'AI Assistant';

          const startIndex = match.index! + match[0].length;
          const endIndex =
            i + 1 < matches.length ? matches[i + 1].index! : raw.length;

          const content = raw.substring(startIndex, endIndex).trim();
          if (content) {
            messages.push({
              id: `direct-msg-${i + 1}`,
              role,
              authorName,
              content,
            });
          }
        }
      }

      if (messages.length === 0) {
        messages.push({
          id: 'direct-msg-1',
          role: 'assistant',
          authorName: 'AI Notes',
          content: raw,
        });
      }

      const userCount = messages.filter((m) => m.role === 'user').length;
      const assistantCount = messages.filter((m) => m.role === 'assistant').length;

      const conversation: ConversationData = {
        id: `direct-${Date.now()}`,
        title: directTitle.trim() || 'AI Study Notes',
        createdAt: new Date().toISOString(),
        messages,
        stats: {
          userMessages: userCount,
          assistantMessages: assistantCount,
          totalMessages: messages.length,
          estimatedPages: calculateAccurateEstimatedPages(messages),
        },
      };

      setProgress({
        step: 'ready',
        message: 'Document parsed successfully!',
        percent: 100,
      });

      onConversationReady(conversation);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to parse pasted text.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <section className="pt-8 pb-12 sm:pt-14 sm:pb-16 px-4 sm:px-6 lg:px-8 max-w-6xl mx-auto">
      {/* Hero Headline */}
      <div className="text-center space-y-3 mb-8">
        <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
          Turn AI Conversations Into PDFs.
        </h1>
        <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
          Convert your ChatGPT, Claude, Gemini, or Perplexity study notes, exam question papers, and explanations into clean, beautifully formatted PDFs. No login. No signup.
        </p>
      </div>

      {/* Main Conversion Card */}
      <div className="bg-white dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[2px] p-5 sm:p-7 shadow-sm transition-colors">
        {/* Method Tabs */}
        <div className="flex items-center gap-2 mb-5 border-b border-slate-200 dark:border-[#263448] pb-3">
          <button
            onClick={() => setActiveTab('url')}
            className={`px-3 py-1.5 text-xs font-medium rounded-[2px] transition-colors flex items-center gap-1.5 ${
              activeTab === 'url'
                ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30'
                : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5]'
            }`}
          >
            <LinkIcon className="w-3.5 h-3.5" />
            AI Share Link (ChatGPT / Claude / Gemini)
          </button>
          <button
            onClick={() => setActiveTab('direct')}
            className={`px-3 py-1.5 text-xs font-medium rounded-[2px] transition-colors flex items-center gap-1.5 ${
              activeTab === 'direct'
                ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30'
                : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5]'
            }`}
          >
            <FileCode className="w-3.5 h-3.5" />
            Direct Text / Markdown
          </button>
        </div>

        {/* Tab 1: Share Link Input */}
        {activeTab === 'url' && (
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row gap-2">
              <div className="relative flex-1">
                <input
                  ref={inputRef}
                  type="url"
                  value={urlInput}
                  onChange={(e) => {
                    setUrlInput(e.target.value);
                    if (errorMsg) setErrorMsg(null);
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !isProcessing) {
                      handleConvertUrl();
                    }
                  }}
                  placeholder="Paste your ChatGPT, Claude, or Gemini share link here..."
                  disabled={isProcessing}
                  className="w-full h-11 pl-3.5 pr-20 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] placeholder-slate-400 dark:placeholder-[#64748B] focus:outline-none focus:border-[#3B82F6] transition-colors"
                />

                {/* Paste Clipboard button */}
                <button
                  type="button"
                  onClick={handlePasteClipboard}
                  disabled={isProcessing}
                  title="Paste from clipboard"
                  className="absolute right-2 top-1/2 -translate-y-1/2 px-2 py-1 text-xs text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-slate-200 dark:bg-[#192536] rounded-[2px] flex items-center gap-1 transition-colors"
                >
                  <Clipboard className="w-3 h-3" />
                  <span className="hidden sm:inline">Paste</span>
                </button>
              </div>

              {/* Main Convert CTA */}
              <button
                type="button"
                onClick={() => handleConvertUrl()}
                disabled={isProcessing}
                className="h-11 px-5 bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] disabled:text-slate-500 text-white font-medium text-xs sm:text-sm rounded-[2px] flex items-center justify-center gap-2 transition-colors shrink-0"
              >
                {isProcessing ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>Processing...</span>
                  </>
                ) : (
                  <>
                    <Download className="w-4 h-4" />
                    <span>Convert to PDF</span>
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* Tab 2: Direct Text Input */}
        {activeTab === 'direct' && (
          <div className="space-y-3">
            <div>
              <label className="block text-xs text-slate-700 dark:text-[#94A3B8] mb-1 font-medium">
                Document / Subject Title
              </label>
              <input
                type="text"
                value={directTitle}
                onChange={(e) => setDirectTitle(e.target.value)}
                placeholder="e.g. Software Testing Exam Preparation"
                className="w-full h-9 px-3 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
              />
            </div>
            <div>
              <label className="block text-xs text-slate-700 dark:text-[#94A3B8] mb-1 font-medium">
                Paste Conversation Text or Markdown
              </label>
              <textarea
                rows={6}
                value={directText}
                onChange={(e) => setDirectText(e.target.value)}
                placeholder="Paste your copied chat notes, question paper explanations, or revision guide here..."
                className="w-full p-3 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs font-mono text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
              />
            </div>
            <button
              type="button"
              onClick={handleConvertDirect}
              disabled={isProcessing}
              className="h-10 px-5 bg-[#3B82F6] hover:bg-[#2563EB] text-white font-medium text-xs rounded-[2px] flex items-center justify-center gap-2 transition-colors"
            >
              <Download className="w-4 h-4" />
              <span>Format & Prepare PDF</span>
            </button>
          </div>
        )}

        {/* Real-time Cute Professional Progress Loader */}
        {isProcessing && <CuteLoader progress={progress} />}

        {/* Error Notification */}
        {errorMsg && (
          <div className="mt-4 p-3 bg-red-50 dark:bg-[#EF4444]/10 border border-red-200 dark:border-[#EF4444]/30 rounded-[2px] flex items-start gap-2.5 text-xs text-red-600 dark:text-[#EF4444]">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <p className="font-medium">{errorMsg}</p>
              <p className="text-slate-600 dark:text-[#94A3B8]">
                Tip: You can switch to the <strong>Direct Text / Markdown</strong> tab above and paste your notes directly.
              </p>
            </div>
          </div>
        )}

        {/* Feature Highlights */}
        <div className="mt-5 pt-4 border-t border-slate-200 dark:border-[#263448] flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-xs text-slate-500 dark:text-[#64748B]">
          <span className="flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-[#22C55E]" />
            No account required
          </span>
          <span className="flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5 text-[#3B82F6]" />
            100% Free for students
          </span>
          <span className="flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5 text-slate-500 dark:text-[#94A3B8]" />
            Private & client-side PDF
          </span>
        </div>
      </div>
    </section>
  );
}
