'use client';

import React, { useState, useMemo } from 'react';
import {
  FileText,
  Download,
  Settings,
  RotateCcw,
  Check,
  ChevronDown,
  ChevronUp,
  BookOpen,
  Edit3,
  Trash2,
  Wand2,
  Save,
  X,
  Plus,
  RefreshCw,
} from 'lucide-react';
import { ConversationData, PdfOptions, ChatMessage } from '@/lib/types';
import { downloadConversationPdf } from '@/lib/pdf-generator';
import { APP_CONFIG } from '@/lib/config';
import { calculateAccurateEstimatedPages } from '@/lib/extract-ai';

interface ConversationPreviewProps {
  conversation: ConversationData;
  onReset: () => void;
}

/**
 * Strips repetitive AI boilerplate introductions and conversational pleasantries
 */
function cleanAiPleasantries(raw: string): string {
  return raw
    .replace(
      /^(?:Sure thing!|Sure!|Certainly!|Of course!|Here is (?:the|a) [^\n]+\n+|Below is (?:the|a) [^\n]+\n+|I have reviewed [^\n]+\n+|As requested[^\n]*\n+)+/gi,
      ''
    )
    .replace(
      /(?:\n+(?:Hope this helps!|Let me know if (?:you (?:have any other questions|need more help|want me to explain|like me to explore)|you'd like to explore)[^\n]*|Feel free to ask[^\n]*|If you like,? I want to explore this much in detail[^\n]*|If you'd like to explore this in more detail[^\n]*|Good luck with your exams!|Best of luck!|Happy studying!)[^\n]*)+$/gi,
      ''
    )
    .trim();
}

export function ConversationPreview({
  conversation,
  onReset,
}: ConversationPreviewProps) {
  const [options, setOptions] = useState<PdfOptions>(APP_CONFIG.defaultPdfOptions);
  const [showSettings, setShowSettings] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);

  // Editable messages state
  const [messages, setMessages] = useState<ChatMessage[]>(conversation.messages);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editText, setEditText] = useState<string>('');

  // Accurately calculated live page count
  const liveEstimatedPages = useMemo(() => {
    return calculateAccurateEstimatedPages(messages);
  }, [messages]);

  const userCount = useMemo(
    () => messages.filter((m) => m.role === 'user').length,
    [messages]
  );
  const assistantCount = useMemo(
    () => messages.filter((m) => m.role === 'assistant').length,
    [messages]
  );

  const handleDownload = () => {
    setIsDownloading(true);
    setDownloadSuccess(false);

    try {
      const updatedConversation: ConversationData = {
        ...conversation,
        messages,
        stats: {
          userMessages: userCount,
          assistantMessages: assistantCount,
          totalMessages: messages.length,
          estimatedPages: liveEstimatedPages,
        },
      };

      downloadConversationPdf(updatedConversation, options);
      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 4000);
    } catch (err) {
      console.error('PDF download error:', err);
      alert('Failed to generate PDF. Please try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  const updateOption = <K extends keyof PdfOptions>(
    key: K,
    value: PdfOptions[K]
  ) => {
    setOptions((prev) => ({ ...prev, [key]: value }));
  };

  // Start editing a message
  const handleStartEdit = (msg: ChatMessage) => {
    setEditingId(msg.id);
    setEditText(msg.content);
  };

  // Save edited message
  const handleSaveEdit = (id: string) => {
    setMessages((prev) =>
      prev.map((m) => (m.id === id ? { ...m, content: editText.trim() } : m))
    );
    setEditingId(null);
  };

  // Cancel editing
  const handleCancelEdit = () => {
    setEditingId(null);
    setEditText('');
  };

  // Delete message
  const handleDeleteMessage = (id: string) => {
    if (confirm('Are you sure you want to remove this message from the PDF?')) {
      setMessages((prev) => prev.filter((m) => m.id !== id));
      if (editingId === id) setEditingId(null);
    }
  };

  // One-click strip AI filler phrases from a message
  const handleStripPleasantries = (id: string) => {
    const cleaned = cleanAiPleasantries(editText);
    setEditText(cleaned);
  };

  // Add custom note section
  const handleAddCustomNote = () => {
    const newMsg: ChatMessage = {
      id: `custom-note-${Date.now()}`,
      role: 'assistant',
      authorName: 'CUSTOM EXAM NOTE',
      content: '### Custom Note\nAdd your custom exam revision points or formulas here...',
    };
    setMessages((prev) => [...prev, newMsg]);
    setEditingId(newMsg.id);
    setEditText(newMsg.content);
  };

  return (
    <section className="pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto space-y-6">
      {/* Header Overview Card */}
      <div className="bg-white dark:bg-[#111A29] border border-slate-300 dark:border-[#22334A] rounded-[3px] p-5 sm:p-6 shadow-[0_2px_8px_rgba(15,23,42,0.06)] dark:shadow-[0_4px_16px_rgba(0,0,0,0.4)] transition-colors">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-slate-200 dark:border-[#22334A]">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold text-blue-600 dark:text-[#38BDF8] uppercase tracking-wider mb-1">
              <BookOpen className="w-3.5 h-3.5" />
              <span>Extracted Notes & Exam Guide</span>
            </div>
            <h2 className="text-lg sm:text-xl font-bold text-slate-900 dark:text-[#F1F5F9]">
              {conversation.title}
            </h2>
            {conversation.sourceUrl && (
              <p className="text-xs text-slate-500 dark:text-[#94A3B8] truncate max-w-md mt-0.5">
                {conversation.sourceUrl}
              </p>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="h-9 px-3 text-xs font-semibold text-slate-700 dark:text-[#CBD5E1] bg-slate-100 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] hover:text-slate-900 dark:hover:text-white flex items-center gap-1.5 transition-colors"
            >
              <Settings className="w-3.5 h-3.5" />
              <span>Formatting Options</span>
              {showSettings ? (
                <ChevronUp className="w-3.5 h-3.5" />
              ) : (
                <ChevronDown className="w-3.5 h-3.5" />
              )}
            </button>

            <button
              onClick={handleDownload}
              disabled={isDownloading}
              className="h-9 px-4 bg-[#1D4ED8] hover:bg-[#1E40AF] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] text-white font-semibold text-xs rounded-[2px] flex items-center gap-1.5 transition-colors shadow-sm shrink-0"
            >
              {downloadSuccess ? (
                <>
                  <Check className="w-3.5 h-3.5 text-white" />
                  <span>Downloaded!</span>
                </>
              ) : (
                <>
                  <Download className="w-3.5 h-3.5" />
                  <span>{isDownloading ? 'Building PDF...' : 'Download PDF'}</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Live Accurate Stats Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-4 text-xs">
          <div className="p-3 bg-slate-50 dark:bg-[#0B121E] border border-slate-200 dark:border-[#1E2C42] rounded-[2px]">
            <span className="text-slate-500 dark:text-[#94A3B8] block mb-1 font-medium">User Prompts</span>
            <span className="text-sm font-bold text-slate-900 dark:text-[#F1F5F9]">
              {userCount}
            </span>
          </div>

          <div className="p-3 bg-slate-50 dark:bg-[#0B121E] border border-slate-200 dark:border-[#1E2C42] rounded-[2px]">
            <span className="text-slate-500 dark:text-[#94A3B8] block mb-1 font-medium">AI Answers</span>
            <span className="text-sm font-bold text-slate-900 dark:text-[#F1F5F9]">
              {assistantCount}
            </span>
          </div>

          <div className="p-3 bg-slate-50 dark:bg-[#0B121E] border border-slate-200 dark:border-[#1E2C42] rounded-[2px]">
            <span className="text-slate-500 dark:text-[#94A3B8] block mb-1 font-medium">Total Messages</span>
            <span className="text-sm font-bold text-slate-900 dark:text-[#F1F5F9]">
              {messages.length}
            </span>
          </div>

          <div className="p-3 bg-blue-50/70 dark:bg-[#0B121E] border border-blue-200 dark:border-blue-900/60 rounded-[2px]">
            <span className="text-blue-700 dark:text-[#38BDF8] block mb-1 font-semibold">Est. PDF Pages</span>
            <span className="text-sm font-extrabold text-blue-800 dark:text-[#38BDF8]">
              ~{liveEstimatedPages} pages
            </span>
          </div>
        </div>

        {/* Customization Settings Accordion */}
        {showSettings && (
          <div className="mt-5 pt-4 border-t border-slate-200 dark:border-[#22334A] space-y-4">
            <h3 className="text-xs font-bold text-slate-700 dark:text-[#94A3B8] uppercase tracking-wider">
              Document Formatting Settings
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-5 gap-3 text-xs">
              {/* Paper Size */}
              <div>
                <label className="block text-slate-600 dark:text-[#94A3B8] mb-1 font-semibold">Paper Size</label>
                <select
                  value={options.paperSize}
                  onChange={(e) => updateOption('paperSize', e.target.value as any)}
                  className="w-full h-8 px-2 bg-slate-50 dark:bg-[#0B121E] border border-slate-300 dark:border-[#22334A] rounded-[2px] text-xs text-slate-900 dark:text-[#F1F5F9] focus:outline-none"
                >
                  <option value="A4">A4 (Standard Academic)</option>
                  <option value="Letter">US Letter</option>
                </select>
              </div>

              {/* Font Size */}
              <div>
                <label className="block text-slate-600 dark:text-[#94A3B8] mb-1 font-semibold">Font Scale</label>
                <select
                  value={options.fontSize}
                  onChange={(e) => updateOption('fontSize', e.target.value as any)}
                  className="w-full h-8 px-2 bg-slate-50 dark:bg-[#0B121E] border border-slate-300 dark:border-[#22334A] rounded-[2px] text-xs text-slate-900 dark:text-[#F1F5F9] focus:outline-none"
                >
                  <option value="small">Compact (9pt body)</option>
                  <option value="medium">Standard (10pt body)</option>
                  <option value="large">Readable (11.5pt body)</option>
                </select>
              </div>

              {/* Font Family */}
              <div>
                <label className="block text-slate-600 dark:text-[#94A3B8] mb-1 font-semibold">Typography</label>
                <select
                  value={options.fontFamily}
                  onChange={(e) => updateOption('fontFamily', e.target.value as any)}
                  className="w-full h-8 px-2 bg-slate-50 dark:bg-[#0B121E] border border-slate-300 dark:border-[#22334A] rounded-[2px] text-xs text-slate-900 dark:text-[#F1F5F9] focus:outline-none"
                >
                  <option value="Helvetica">Inter / Helvetica (Modern Clean)</option>
                  <option value="Times">Times New Roman (Academic Serif)</option>
                  <option value="Courier">Courier (Monospace Code)</option>
                </select>
              </div>

              {/* Page Border (MS Word Shadow) */}
              <div>
                <label className="block text-slate-600 dark:text-[#94A3B8] mb-1 font-semibold">Page Border</label>
                <select
                  value={options.borderStyle || 'shadow'}
                  onChange={(e) => updateOption('borderStyle', e.target.value as any)}
                  className="w-full h-8 px-2 bg-slate-50 dark:bg-[#0B121E] border border-slate-300 dark:border-[#22334A] rounded-[2px] text-xs text-slate-900 dark:text-[#F1F5F9] focus:outline-none"
                >
                  <option value="shadow">MS Word Dark Shadow Border</option>
                  <option value="clean">Thin Clean Line</option>
                  <option value="none">No Border</option>
                </select>
              </div>

              {/* College Study Promotion Scope */}
              <div>
                <label className="block text-slate-600 dark:text-[#94A3B8] mb-1 font-semibold">College Study Header</label>
                <select
                  value={options.partnerBadgeScope || 'all-pages'}
                  onChange={(e) => updateOption('partnerBadgeScope', e.target.value as any)}
                  className="w-full h-8 px-2 bg-slate-50 dark:bg-[#0B121E] border border-slate-300 dark:border-[#22334A] rounded-[2px] text-xs text-slate-900 dark:text-[#F1F5F9] focus:outline-none"
                >
                  <option value="all-pages">Every Page Header</option>
                  <option value="first-page">First Page Only</option>
                </select>
              </div>
            </div>

            {/* Toggle checkboxes */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 text-xs text-slate-700 dark:text-[#94A3B8]">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={options.includeTitle}
                  onChange={(e) => updateOption('includeTitle', e.target.checked)}
                  className="rounded-[2px] border-slate-300 dark:border-[#263448] text-[#3B82F6]"
                />
                <span>Include Title Banner</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={options.includeDate}
                  onChange={(e) => updateOption('includeDate', e.target.checked)}
                  className="rounded-[2px] border-slate-300 dark:border-[#263448] text-[#3B82F6]"
                />
                <span>Include Export Date</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={options.includePageNumbers}
                  onChange={(e) => updateOption('includePageNumbers', e.target.checked)}
                  className="rounded-[2px] border-slate-300 dark:border-[#263448] text-[#3B82F6]"
                />
                <span>Include Page Numbers</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={options.includeUserMessages}
                  onChange={(e) => updateOption('includeUserMessages', e.target.checked)}
                  className="rounded-[2px] border-slate-300 dark:border-[#263448] text-[#3B82F6]"
                />
                <span>Include User Prompts</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={options.includeCodeBlocks}
                  onChange={(e) => updateOption('includeCodeBlocks', e.target.checked)}
                  className="rounded-[2px] border-slate-300 dark:border-[#263448] text-[#3B82F6]"
                />
                <span>Include Code Blocks</span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={options.includeAssistantMessages}
                  onChange={(e) => updateOption('includeAssistantMessages', e.target.checked)}
                  className="rounded-[2px] border-slate-300 dark:border-[#263448] text-[#3B82F6]"
                />
                <span>Include AI Notes</span>
              </label>
            </div>

            {/* Mandatory Academic Footer Branding Lockup */}
            <div className="p-3 bg-slate-50 dark:bg-[#0B121E] border border-slate-200 dark:border-[#1E2C42] rounded-[2px] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
              <div className="flex flex-wrap items-center gap-2.5">
                <span className="text-slate-700 dark:text-[#94A3B8] font-medium">Permanent PDF Footer:</span>
                <div className="inline-flex items-center gap-1.5 px-2 py-1 rounded-[2px] bg-white dark:bg-[#141D2B] border border-slate-300 dark:border-[#263448] shadow-xs">
                  <img src="/images/app-logo.jpg" alt="Logo" className="w-4 h-4 rounded-xs object-contain" />
                  <span className="font-bold text-slate-900 dark:text-[#E8EDF5] text-[11px]">AI Chat to PDF</span>
                  <span className="text-[8px] font-bold px-1 py-0.2 bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400 rounded-xs">PDF</span>
                </div>
                <a
                  href="https://www.linkedin.com/in/priyal-kumar-singh/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 px-2 py-1 rounded-[2px] bg-sky-50 dark:bg-sky-950/60 border border-sky-300 dark:border-sky-800 text-[11px] font-bold text-sky-800 dark:text-sky-300 hover:underline"
                >
                  Developer: Priyal Kumar ↗
                </a>
              </div>
              <a
                href="https://college-study.netlify.app/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1.5 text-slate-600 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-[#38BDF8] text-[11px] transition-colors"
              >
                <img src="/images/college-study-logo.jpg" alt="College Study" className="w-4 h-4 rounded-sm object-contain" />
                <span>Partnered with <strong>College Study</strong></span>
              </a>
            </div>
          </div>
        )}
      </div>

      {/* Document Content Preview & Live Editor Card */}
      <div className="bg-white dark:bg-[#111A29] border border-slate-300 dark:border-[#22334A] rounded-[3px] p-5 sm:p-6 shadow-[0_2px_8px_rgba(15,23,42,0.06)] dark:shadow-[0_4px_16px_rgba(0,0,0,0.4)] transition-colors">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-200 dark:border-[#22334A] gap-3 mb-5">
          <div>
            <span className="text-sm font-bold text-slate-900 dark:text-[#F1F5F9] block">
              Document Preview & Editor ({messages.length} messages)
            </span>
            <span className="text-xs text-slate-500 dark:text-[#94A3B8]">
              Click <strong>Edit</strong> on any message below to cut filler lines, remove AI pleasantries, or add custom study notes.
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleAddCustomNote}
              className="h-8 px-2.5 text-xs text-slate-700 dark:text-[#CBD5E1] bg-slate-100 dark:bg-[#0B121E] hover:bg-slate-200 dark:hover:bg-[#162335] border border-slate-300 dark:border-[#22334A] rounded-[2px] flex items-center gap-1 transition-colors"
              title="Add a custom revision section"
            >
              <Plus className="w-3.5 h-3.5 text-blue-600 dark:text-[#38BDF8]" />
              <span>Add Custom Note</span>
            </button>

            <button
              onClick={onReset}
              className="h-8 px-2.5 text-xs text-slate-500 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-[#38BDF8] border border-slate-200 dark:border-[#22334A] rounded-[2px] flex items-center gap-1 transition-colors"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Convert Another</span>
            </button>
          </div>
        </div>

        {/* Message Stream with Interactive Inline Editing */}
        <div className="space-y-5 max-h-[620px] overflow-y-auto pr-2">
          {messages.map((msg, idx) => {
            const isEditing = editingId === msg.id;

            return (
              <div
                key={msg.id || idx}
                className={`p-4 rounded-[2px] border text-xs sm:text-sm leading-relaxed transition-all ${
                  msg.role === 'user'
                    ? 'bg-slate-50 dark:bg-[#0B121E] border-slate-300 dark:border-[#1E2C42]'
                    : 'bg-white dark:bg-[#142033] border-slate-300 dark:border-[#22354E]'
                } ${isEditing ? 'ring-2 ring-blue-500/40 dark:ring-[#38BDF8]/40' : ''}`}
              >
                {/* Message Header & Action Toolbar */}
                <div className="flex items-center justify-between pb-2 mb-3 border-b border-slate-200 dark:border-[#1E2C42]">
                  <div className="flex items-center gap-2">
                    <span
                      className={`font-bold text-xs uppercase tracking-wide ${
                        msg.role === 'user'
                          ? 'text-blue-600 dark:text-[#38BDF8]'
                          : 'text-slate-800 dark:text-[#E2E8F0]'
                      }`}
                    >
                      {msg.authorName}
                    </span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded-sm bg-slate-200/80 dark:bg-[#1E2C42] text-slate-600 dark:text-[#94A3B8]">
                      #{idx + 1}
                    </span>
                    <span className="text-[10px] text-slate-400 dark:text-[#64748B]">
                      {msg.content.replace(/data:image\/[^;]+;base64,[A-Za-z0-9+/=]+/g, '').length} chars
                    </span>
                  </div>

                  {/* Actions: Edit & Delete */}
                  {!isEditing && (
                    <div className="flex items-center gap-1.5">
                      <button
                        onClick={() => handleStartEdit(msg)}
                        className="px-2 py-1 text-[11px] font-medium text-slate-600 dark:text-[#CBD5E1] hover:text-blue-600 dark:hover:text-[#38BDF8] hover:bg-slate-100 dark:hover:bg-[#1A283E] rounded-[2px] flex items-center gap-1 transition-colors"
                        title="Edit this response to cut or add text"
                      >
                        <Edit3 className="w-3 h-3 text-blue-600 dark:text-[#38BDF8]" />
                        <span>Edit Text</span>
                      </button>

                      <button
                        onClick={() => handleDeleteMessage(msg.id)}
                        className="p-1 text-slate-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-[2px] transition-colors"
                        title="Remove this message from PDF"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>

                {/* Body: Either Editor or Preview */}
                {isEditing ? (
                  <div className="space-y-3 pt-1">
                    <div className="flex flex-wrap items-center justify-between gap-2 bg-blue-50/60 dark:bg-[#0B121E] p-2 border border-blue-200/60 dark:border-[#1E2C42] rounded-[2px] text-xs">
                      <span className="text-slate-600 dark:text-[#94A3B8] text-[11px]">
                        ✂ Cut out unwanted text, strip AI introductions, or add notes:
                      </span>
                      <button
                        type="button"
                        onClick={() => handleStripPleasantries(msg.id)}
                        className="px-2 py-1 bg-white dark:bg-[#142033] border border-blue-300 dark:border-[#22354E] hover:border-blue-500 text-blue-700 dark:text-[#38BDF8] font-semibold text-[11px] rounded-[2px] flex items-center gap-1 transition-colors"
                        title="Removes 'Sure! Here is...', 'Hope this helps!', and 'If you like I want to explore this much in detail...'"
                      >
                        <Wand2 className="w-3 h-3" />
                        <span>Auto-Clean AI Pleasantries</span>
                      </button>
                    </div>

                    <textarea
                      value={editText}
                      onChange={(e) => setEditText(e.target.value)}
                      rows={Math.min(22, Math.max(6, editText.split('\n').length + 2))}
                      className="w-full p-3 font-mono text-xs leading-relaxed bg-slate-50 dark:bg-[#0B121E] border border-slate-300 dark:border-[#22354E] rounded-[2px] text-slate-900 dark:text-[#F1F5F9] focus:outline-none focus:ring-1 focus:ring-blue-500 resize-y"
                      placeholder="Type or paste markdown content here..."
                    />

                    <div className="flex items-center justify-end gap-2 pt-1">
                      <button
                        type="button"
                        onClick={handleCancelEdit}
                        className="px-3 py-1.5 text-xs text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-white border border-slate-300 dark:border-[#22354E] rounded-[2px] flex items-center gap-1 transition-colors"
                      >
                        <X className="w-3 h-3" />
                        <span>Cancel</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => handleSaveEdit(msg.id)}
                        className="px-4 py-1.5 text-xs font-semibold text-white bg-[#1D4ED8] hover:bg-[#1E40AF] rounded-[2px] flex items-center gap-1 transition-colors shadow-sm"
                      >
                        <Save className="w-3.5 h-3.5" />
                        <span>Save Changes</span>
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="whitespace-pre-wrap font-sans text-slate-800 dark:text-[#E2E8F0] space-y-2">
                    {msg.content}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Bottom Action Footer */}
        <div className="mt-6 pt-4 border-t border-slate-200 dark:border-[#22334A] flex flex-col sm:flex-row items-center justify-between gap-3">
          <p className="text-xs text-slate-500 dark:text-[#94A3B8]">
            Notes typeset with MS Word shadow border and College Study header.
          </p>

          <div className="flex items-center gap-2.5 w-full sm:w-auto">
            <button
              onClick={onReset}
              className="h-9 px-3 text-xs text-slate-700 dark:text-[#CBD5E1] hover:text-slate-900 dark:hover:text-white border border-slate-300 dark:border-[#22334A] rounded-[2px] transition-colors w-full sm:w-auto"
            >
              Convert Another
            </button>
            <button
              onClick={handleDownload}
              disabled={isDownloading}
              className="h-9 px-5 bg-[#1D4ED8] hover:bg-[#1E40AF] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] text-white font-bold text-xs rounded-[2px] flex items-center justify-center gap-1.5 transition-colors shadow-sm w-full sm:w-auto"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{isDownloading ? 'Building PDF...' : 'Download PDF Now'}</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
