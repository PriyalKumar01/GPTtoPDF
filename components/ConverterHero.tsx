'use client';

import React, { useState, useRef } from 'react';
import Link from 'next/link';
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
  Images,
  QrCode,
  ArrowRight,
  FileText,
  Plus,
  Trash2,
  ArrowUp,
  ArrowDown,
} from 'lucide-react';
import { InstagramIcon, FacebookIcon, YouTubeIcon } from '@/components/BrandIcons';
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
  const [activeTab, setActiveTab] = useState<'url' | 'direct' | 'builder'>('url');
  const [directText, setDirectText] = useState('');
  const [directTitle, setDirectTitle] = useState('My AI Study Notes');

  // Structured Notes Builder state
  const [builderSubject, setBuilderSubject] = useState('Operating Systems — Process Scheduling Notes');
  const [builderSections, setBuilderSections] = useState<Array<{ id: string; question: string; answer: string }>>([
    {
      id: 'sec-1',
      question: '1. What is Preemptive vs. Non-preemptive Scheduling?',
      answer:
        'In preemptive scheduling, the CPU can be taken away from a running process if a higher priority process arrives (e.g. Round Robin, SRTF). In non-preemptive scheduling, once a process gets CPU, it keeps it until it terminates or blocks for I/O (e.g. FCFS, SJF).',
    },
    {
      id: 'sec-2',
      question: '2. Explain the Round Robin (RR) Algorithm with Example',
      answer:
        'Round Robin allocates a fixed time quantum (e.g. 2ms or 4ms) to each process in a circular FIFO queue. It ensures fair allocation, responsive time-sharing, and completely prevents process starvation.',
    },
  ]);

  const handleAddBuilderSection = () => {
    setBuilderSections((prev) => [
      ...prev,
      {
        id: `sec_${Date.now()}_${Math.random().toString(36).substring(2, 5)}`,
        question: `${prev.length + 1}. `,
        answer: '',
      },
    ]);
  };

  const handleRemoveBuilderSection = (id: string) => {
    setBuilderSections((prev) => prev.filter((s) => s.id !== id));
  };

  const handleMoveBuilderSection = (index: number, direction: 'up' | 'down') => {
    const target = direction === 'up' ? index - 1 : index + 1;
    if (target < 0 || target >= builderSections.length) return;
    setBuilderSections((prev) => {
      const copy = [...prev];
      const temp = copy[index];
      copy[index] = copy[target];
      copy[target] = temp;
      return copy;
    });
  };

  const handleUpdateBuilderSection = (id: string, field: 'question' | 'answer', value: string) => {
    setBuilderSections((prev) =>
      prev.map((s) => (s.id === id ? { ...s, [field]: value } : s))
    );
  };

  const applyTextFormat = (
    secId: string,
    field: 'question' | 'answer',
    format: 'bold' | 'italic' | 'bullet'
  ) => {
    setBuilderSections((prev) =>
      prev.map((sec) => {
        if (sec.id !== secId) return sec;
        const current = sec[field];
        let updated = current;
        if (format === 'bold') {
          updated = current ? `**${current}**` : '**bold topic**';
        } else if (format === 'italic') {
          updated = current ? `*${current}*` : '*italic topic*';
        } else if (format === 'bullet') {
          updated = current ? `${current}\n• ` : '• ';
        }
        return { ...sec, [field]: updated };
      })
    );
  };

  const handleConvertBuilder = () => {
    setErrorMsg(null);
    if (builderSections.length === 0) {
      setErrorMsg('Please add at least one question or topic to build your notes.');
      return;
    }

    setIsProcessing(true);
    setProgress({
      step: 'preparing',
      message: 'Structuring custom academic study notes layout...',
      percent: 60,
    });

    try {
      const messages: ChatMessage[] = [];

      builderSections.forEach((sec, idx) => {
        if (sec.question.trim()) {
          messages.push({
            id: `builder-q-${idx + 1}`,
            role: 'user',
            authorName: '', // Left blank so 'USER' label is omitted and question is highlighted cleanly
            content: sec.question.trim(),
            timestamp: new Date().toLocaleTimeString(),
          });
        }
        if (sec.answer.trim()) {
          messages.push({
            id: `builder-a-${idx + 1}`,
            role: 'assistant',
            authorName: '', // Left blank so 'ASSISTANT' label is omitted
            content: sec.answer.trim(),
            timestamp: new Date().toLocaleTimeString(),
          });
        }
      });

      const userCount = messages.filter((m) => m.role === 'user').length;
      const assistantCount = messages.filter((m) => m.role === 'assistant').length;

      const conversation: ConversationData = {
        id: `builder-${Date.now()}`,
        sourceUrl: '',
        title: builderSubject.trim() || 'Custom AI Study Notes',
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
        message: 'Custom notes structured successfully!',
        percent: 100,
      });

      onConversationReady(conversation);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to build custom notes.');
    } finally {
      setIsProcessing(false);
    }
  };

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
          <button
            onClick={() => setActiveTab('builder')}
            className={`px-3 py-1.5 text-xs font-medium rounded-[2px] transition-colors flex items-center gap-1.5 ${
              activeTab === 'builder'
                ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30'
                : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5]'
            }`}
          >
            <FileText className="w-3.5 h-3.5" />
            Structured Notes Builder
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

        {/* Tab 3: Structured Notes Builder */}
        {activeTab === 'builder' && (
          <div className="space-y-4">
            <div>
              <label className="block text-xs text-slate-700 dark:text-[#94A3B8] mb-1 font-semibold">
                Document / Exam Notes Subject Title
              </label>
              <input
                type="text"
                value={builderSubject}
                onChange={(e) => setBuilderSubject(e.target.value)}
                placeholder="e.g. Operating Systems — Process Synchronization Notes"
                className="w-full h-9 px-3 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs font-semibold text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
              />
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-[#94A3B8]">
                  Modular Q&A / Topic Sections ({builderSections.length})
                </span>
                <button
                  type="button"
                  onClick={handleAddBuilderSection}
                  className="h-7 px-2.5 rounded-[2px] text-xs font-semibold bg-blue-50 dark:bg-[#1E293B] text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-900/60 hover:bg-blue-100 dark:hover:bg-[#28384E] flex items-center gap-1 transition"
                >
                  <Plus className="w-3 h-3" />
                  <span>Add Topic / Question</span>
                </button>
              </div>

              {builderSections.map((sec, idx) => (
                <div
                  key={sec.id}
                  className="p-3 bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] rounded-[2px] space-y-2.5 relative group"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[11px] font-bold text-blue-600 dark:text-blue-400 font-mono">
                      Section #{idx + 1}
                    </span>
                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        disabled={idx === 0}
                        onClick={() => handleMoveBuilderSection(idx, 'up')}
                        className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white disabled:opacity-20"
                        title="Move Up"
                      >
                        <ArrowUp className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        disabled={idx === builderSections.length - 1}
                        onClick={() => handleMoveBuilderSection(idx, 'down')}
                        className="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-white disabled:opacity-20"
                        title="Move Down"
                      >
                        <ArrowDown className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        disabled={builderSections.length <= 1}
                        onClick={() => handleRemoveBuilderSection(sec.id)}
                        className="p-1 text-red-400 hover:text-red-600 disabled:opacity-20 ml-1"
                        title="Delete Section"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Question / Topic Toolbar & Input */}
                  <div className="space-y-1">
                    <div className="flex items-center justify-between">
                      <label className="text-[11px] font-semibold text-slate-700 dark:text-[#CBD5E1]">
                        Question / Topic Header (Highlighted in PDF)
                      </label>
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() => applyTextFormat(sec.id, 'question', 'bold')}
                          className="px-1.5 py-0.5 rounded-[2px] text-[10px] font-bold bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#CBD5E1] hover:text-blue-600 transition"
                          title="Wrap with **Bold**"
                        >
                          <strong>B</strong>
                        </button>
                        <button
                          type="button"
                          onClick={() => applyTextFormat(sec.id, 'question', 'italic')}
                          className="px-1.5 py-0.5 rounded-[2px] text-[10px] italic font-serif bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#CBD5E1] hover:text-blue-600 transition"
                          title="Wrap with *Italic*"
                        >
                          <em>I</em>
                        </button>
                      </div>
                    </div>
                    <input
                      type="text"
                      value={sec.question}
                      onChange={(e) => handleUpdateBuilderSection(sec.id, 'question', e.target.value)}
                      placeholder="Topic Heading or Question (e.g. 1. What is Preemptive Scheduling?)"
                      className="w-full h-8 px-2.5 bg-white dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs font-semibold text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500"
                    />
                  </div>

                  {/* Answer / Explanation Toolbar & Textarea */}
                  <div className="space-y-1">
                    <div className="flex items-center justify-between">
                      <label className="text-[11px] font-semibold text-slate-700 dark:text-[#CBD5E1]">
                        Academic Explanation / Answer (Normal clean paragraph)
                      </label>
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() => applyTextFormat(sec.id, 'answer', 'bold')}
                          className="px-1.5 py-0.5 rounded-[2px] text-[10px] font-bold bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#CBD5E1] hover:text-blue-600 transition"
                          title="Bold (**text**)"
                        >
                          <strong>B</strong>
                        </button>
                        <button
                          type="button"
                          onClick={() => applyTextFormat(sec.id, 'answer', 'italic')}
                          className="px-1.5 py-0.5 rounded-[2px] text-[10px] italic font-serif bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#CBD5E1] hover:text-blue-600 transition"
                          title="Italic (*text*)"
                        >
                          <em>I</em>
                        </button>
                        <button
                          type="button"
                          onClick={() => applyTextFormat(sec.id, 'answer', 'bullet')}
                          className="px-1.5 py-0.5 rounded-[2px] text-[10px] bg-white dark:bg-[#1E293B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#CBD5E1] hover:text-blue-600 transition"
                          title="Insert Bullet Point (• )"
                        >
                          • Bullet
                        </button>
                      </div>
                    </div>
                    <textarea
                      rows={3}
                      value={sec.answer}
                      onChange={(e) => handleUpdateBuilderSection(sec.id, 'answer', e.target.value)}
                      placeholder="Explanation, Answer paragraph, bullet points, or formulas..."
                      className="w-full p-2.5 bg-white dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 resize-none leading-relaxed"
                    />
                  </div>
                </div>
              ))}
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                onClick={handleConvertBuilder}
                disabled={isProcessing}
                className="h-10 px-5 bg-[#3B82F6] hover:bg-[#2563EB] text-white font-medium text-xs rounded-[2px] flex items-center justify-center gap-2 transition-colors shadow-xs"
              >
                <Download className="w-4 h-4" />
                <span>Build & Preview Academic PDF</span>
              </button>

              <button
                type="button"
                onClick={handleAddBuilderSection}
                className="h-10 px-3.5 rounded-[2px] border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#111A29] text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-white text-xs font-medium flex items-center gap-1.5 transition"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Another Topic</span>
              </button>
            </div>
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

      {/* Quick Access to New Utility Pages */}
      <div className="mt-8 pt-6 border-t border-slate-200 dark:border-[#263448]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <h2 className="text-xs sm:text-sm font-bold uppercase tracking-wider text-slate-700 dark:text-[#E8EDF5]">
              Popular Free Utilities & Social Downloaders
            </h2>
          </div>
          <Link
            href="/tools"
            className="text-xs text-blue-600 dark:text-[#3B82F6] hover:underline flex items-center gap-1 font-medium"
          >
            <span>All Tools</span>
            <ArrowRight className="w-3 h-3" />
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
          <Link
            href="/instagram"
            className="group p-4 rounded-[2px] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:border-pink-500/50 shadow-sm transition flex flex-col justify-between"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="w-8 h-8 rounded-[2px] bg-pink-50 dark:bg-pink-950/40 text-pink-600 dark:text-pink-400 flex items-center justify-center transition">
                <InstagramIcon className="w-4 h-4" />
              </div>
              <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-[2px] bg-pink-500/10 text-pink-600 dark:text-pink-400 border border-pink-500/20">
                Trending
              </span>
            </div>
            <div>
              <h3 className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] group-hover:text-pink-600 dark:group-hover:text-pink-400 transition-colors">
                Instagram Downloader
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-1 line-clamp-2">
                Download Reels, Videos & Carousel photos in HD without watermark.
              </p>
            </div>
          </Link>

          <Link
            href="/facebook"
            className="group p-4 rounded-[2px] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:border-blue-500/50 shadow-sm transition flex flex-col justify-between"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="w-8 h-8 rounded-[2px] bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center transition">
                <FacebookIcon className="w-4 h-4" />
              </div>
              <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-[2px] bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                Full HD
              </span>
            </div>
            <div>
              <h3 className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                Facebook Downloader
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-1 line-clamp-2">
                Save Facebook public reels, watch videos, and clips in 1080p/720p.
              </p>
            </div>
          </Link>

          <Link
            href="/youtube"
            className="group p-4 rounded-[2px] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:border-red-500/50 shadow-sm transition flex flex-col justify-between"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="w-8 h-8 rounded-[2px] bg-red-50 dark:bg-red-950/40 text-red-600 dark:text-red-400 flex items-center justify-center transition">
                <YouTubeIcon className="w-4 h-4 text-red-600" />
              </div>
              <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-[2px] bg-red-500/10 text-red-600 dark:text-red-400 border border-red-500/20">
                Universal
              </span>
            </div>
            <div>
              <h3 className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] group-hover:text-red-600 dark:group-hover:text-red-400 transition-colors">
                YouTube & Universal
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-1 line-clamp-2">
                Download YouTube Shorts, TikTok (no watermark), X & Pinterest.
              </p>
            </div>
          </Link>

          <Link
            href="/pdf-tools"
            className="group p-4 rounded-[2px] bg-white dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:border-blue-500/50 shadow-sm transition flex flex-col justify-between"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="w-8 h-8 rounded-[2px] bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center transition">
                <Images className="w-4 h-4" />
              </div>
              <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-[2px] bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                Suite
              </span>
            </div>
            <div>
              <h3 className="font-bold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                PDF Tools & Suite
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-1 line-clamp-2">
                Merge PDFs, Compress files, convert PDF to Images & JPG to PDF.
              </p>
            </div>
          </Link>
        </div>
      </div>

      {/* Master Feature Directory & Direct Action Table */}
      <div className="mt-8 pt-6 border-t border-slate-200 dark:border-[#263448]">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h3 className="text-xs sm:text-sm font-bold uppercase tracking-wider text-slate-800 dark:text-[#E8EDF5]">
                Direct Feature & Keyword Directory
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-[#94A3B8]">
                Find any tool instantly with supported formats and 1-click access.
              </p>
            </div>
            <Link
              href="/tools"
              className="text-xs text-blue-600 dark:text-[#3B82F6] hover:underline font-semibold"
            >
              Explore All Features →
            </Link>
          </div>

          <div className="border border-slate-200 dark:border-[#263448] rounded-[2px] overflow-hidden bg-white dark:bg-[#111A29]">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-[#263448] bg-slate-50 dark:bg-[#0D1524] text-[11px] font-bold text-slate-700 dark:text-[#E8EDF5]">
                    <th className="py-2.5 px-3.5">Feature & Tool</th>
                    <th className="py-2.5 px-3.5">Key Capability</th>
                    <th className="py-2.5 px-3.5">Supported Formats</th>
                    <th className="py-2.5 px-3.5 text-right">Direct Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-[#1E293B]">
                  {[
                    {
                      name: 'AI Chat to PDF',
                      desc: 'Academic PDF formatted with code & tables',
                      formats: 'ChatGPT, Claude, Gemini, DeepSeek',
                      href: '/',
                      badge: 'Academic Core',
                      color: 'text-red-500',
                    },
                    {
                      name: 'Instagram Reel Downloader',
                      desc: 'Save public reels, videos & carousels without watermark',
                      formats: 'Reels, Video MP4, HD Photos',
                      href: '/instagram',
                      badge: '1080p HD',
                      color: 'text-pink-500',
                    },
                    {
                      name: 'Facebook Video Downloader',
                      desc: 'Download FB watch clips & public reels with exact MB sizes',
                      formats: '1080p, 720p, 480p MP4',
                      href: '/facebook',
                      badge: 'Full HD',
                      color: 'text-blue-500',
                    },
                    {
                      name: 'YouTube Shorts & Universal',
                      desc: 'Shorts, TikTok (no watermark), X/Twitter & Pinterest',
                      formats: 'YouTube, TikTok, Twitter, Pinterest',
                      href: '/youtube',
                      badge: 'Universal',
                      color: 'text-red-500',
                    },
                    {
                      name: 'Merge PDF Documents',
                      desc: 'Combine multiple PDFs in custom order with client privacy',
                      formats: 'All PDF Documents',
                      href: '/pdf-tools/merge',
                      badge: 'Client-Side',
                      color: 'text-blue-500',
                    },
                    {
                      name: 'Compress PDF File',
                      desc: 'Reduce PDF file size by 50-80% without quality loss',
                      formats: 'Heavy PDFs, Notes, Scans',
                      href: '/pdf-tools/compress',
                      badge: 'Optimized',
                      color: 'text-emerald-500',
                    },
                    {
                      name: 'PDF to Image Converter',
                      desc: 'Extract individual pages as high-resolution images',
                      formats: 'PDF to JPG, PNG',
                      href: '/pdf-tools/pdf-to-image',
                      badge: 'High-Res',
                      color: 'text-amber-500',
                    },
                    {
                      name: 'Audio Extractor (Video to MP3)',
                      desc: 'Extract audio MP3 track from any URL or local video file',
                      formats: 'MP4, MOV, WebM to MP3',
                      href: '/audio-extractor',
                      badge: 'Audio MP3',
                      color: 'text-indigo-500',
                    },
                  ].map((row, idx) => (
                    <tr
                      key={idx}
                      className="hover:bg-emerald-50/50 dark:hover:bg-[#122329]/40 transition-colors group"
                    >
                      <td className="py-2.5 px-3.5 font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
                        <span className={`w-1.5 h-1.5 rounded-full ${row.color.replace('text-', 'bg-')}`} />
                        <span className="group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">
                          {row.name}
                        </span>
                      </td>
                      <td className="py-2.5 px-3.5 text-slate-600 dark:text-[#94A3B8]">
                        {row.desc}
                      </td>
                      <td className="py-2.5 px-3.5 text-slate-500 dark:text-[#94A3B8] font-mono text-[11px]">
                        {row.formats}
                      </td>
                      <td className="py-2.5 px-3.5 text-right">
                        <Link
                          href={row.href}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-semibold text-white bg-[#0F172A] hover:bg-[#1E293B] dark:bg-[#1E293B] dark:hover:bg-[#334155] rounded-[2px] border border-slate-700 dark:border-[#334155] transition shadow-xs group-hover:scale-105"
                        >
                          <span>Use This</span>
                          <ArrowRight className="w-3 h-3" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
    </section>
  );
}
