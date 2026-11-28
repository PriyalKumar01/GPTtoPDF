'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  Music,
  Upload,
  Download,
  Link as LinkIcon,
  FileAudio,
  AlertCircle,
  CheckCircle2,
  X,
  FileCheck,
} from 'lucide-react';
import { DownloaderProgressLoader } from '@/components/DownloaderProgressLoader';
import { useAuth } from '@/lib/use-auth';
import { AuthModal } from '@/components/AuthModal';

export default function AudioExtractorPage() {
  const {
    isAuthenticated,
    isAuthModalOpen,
    openAuthModal,
    closeAuthModal,
    requireAuth,
    handleAuthSuccess,
  } = useAuth();
  const [activeTab, setActiveTab] = useState<'local' | 'url'>('local');
  const [localFile, setLocalFile] = useState<File | null>(null);
  const [urlInput, setUrlInput] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [audioFilename, setAudioFilename] = useState('extracted_audio.wav');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const resultRef = useRef<HTMLDivElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const f = e.target.files[0];
      setLocalFile(f);
      setErrorMsg(null);
      setAudioUrl(null);
    }
  };

  const handleExtractLocal = async () => {
    if (!localFile) return;

    requireAuth(async () => {
      setIsProcessing(true);
      setErrorMsg(null);
      setAudioUrl(null);

      try {
        const arrayBuffer = await localFile.arrayBuffer();
        const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
        const decodedBuffer = await audioCtx.decodeAudioData(arrayBuffer);

        // Convert audio buffer to WAV
        const wavBlob = audioBufferToWav(decodedBuffer);
        const url = URL.createObjectURL(wavBlob);
        const filename = `${localFile.name.replace(/\.[^/.]+$/, '')}_audio.wav`;

        setAudioUrl(url);
        setAudioFilename(filename);
        setTimeout(() => {
          resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 150);

        // Auto download
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      } catch (e: any) {
        console.error('Audio extraction error:', e);
        setErrorMsg('Could not decode audio from this video file. Please verify the format.');
      } finally {
        setIsProcessing(false);
      }
    });
  };

  const handleExtractUrl = async () => {
    if (!urlInput.trim()) {
      setErrorMsg('Please enter a video URL');
      return;
    }

    requireAuth(async () => {
      setIsProcessing(true);
      setErrorMsg(null);
      setAudioUrl(null);

      try {
        // Use our universal or instagram API to fetch audio stream
        const res = await fetch('/api/universal-download', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ url: urlInput.trim() }),
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
          throw new Error(data.error || 'Failed to extract audio from this URL.');
        }

        const streamUrl = data.data.audioUrl || data.data.downloadUrl;
        if (!streamUrl) {
          throw new Error('No audio stream found for this link.');
        }

        const proxyAudio = `/api/proxy-download?url=${encodeURIComponent(
          streamUrl
        )}&filename=audio_${data.data.id}.mp3`;

        setAudioUrl(proxyAudio);
        setAudioFilename(`audio_${data.data.id}.mp3`);
        setTimeout(() => {
          resultRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 150);
      } catch (e: any) {
        console.error('URL extraction error:', e);
        setErrorMsg(e.message || 'Could not extract audio track from this URL.');
      } finally {
        setIsProcessing(false);
      }
    });
  };

  // Fast WAV encoding helper
  function audioBufferToWav(buffer: AudioBuffer): Blob {
    const numChannels = buffer.numberOfChannels;
    const sampleRate = buffer.sampleRate;
    const format = 1; // PCM
    const bitDepth = 16;

    let result: Float32Array;
    if (numChannels === 2) {
      const left = buffer.getChannelData(0);
      const right = buffer.getChannelData(1);
      result = new Float32Array(left.length + right.length);
      for (let i = 0; i < left.length; i++) {
        result[i * 2] = left[i];
        result[i * 2 + 1] = right[i];
      }
    } else {
      result = buffer.getChannelData(0);
    }

    const dataLength = result.length * (bitDepth / 8);
    const headerBuffer = new ArrayBuffer(44);
    const view = new DataView(headerBuffer);

    function writeString(offset: number, str: string) {
      for (let i = 0; i < str.length; i++) {
        view.setUint8(offset + i, str.charCodeAt(i));
      }
    }

    writeString(0, 'RIFF');
    view.setUint32(4, 36 + dataLength, true);
    writeString(8, 'WAVE');
    writeString(12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, format, true);
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * numChannels * (bitDepth / 8), true);
    view.setUint16(32, numChannels * (bitDepth / 8), true);
    view.setUint16(34, bitDepth, true);
    writeString(36, 'data');
    view.setUint32(40, dataLength, true);

    const int16Array = new Int16Array(result.length);
    for (let i = 0; i < result.length; i++) {
      const s = Math.max(-1, Math.min(1, result[i]));
      int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
    }

    return new Blob([view, int16Array], { type: 'audio/wav' });
  }

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20">
              <Music className="w-3.5 h-3.5" />
              <span>Video to MP3 — Free Online Audio Extractor</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Video to MP3 — Extract Audio from Reels & Videos Free
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Extract background sound and music from Instagram Reels, TikTok, Facebook videos, or local video files in crystal-clear MP3 format. Fast, private, and 100% free.
            </p>
          </div>

          <div className="bg-white dark:bg-[#141D2B] rounded-[2px] p-5 sm:p-7 shadow-sm border border-slate-200 dark:border-[#263448] max-w-4xl mx-auto">
            {/* Tabs */}
            <div className="flex items-center gap-2 mb-5 border-b border-slate-200 dark:border-[#263448] pb-3">
              <button
                type="button"
                onClick={() => {
                  setActiveTab('local');
                  setErrorMsg(null);
                }}
                className={`px-3 py-1.5 text-xs font-medium rounded-[2px] transition-colors flex items-center gap-1.5 ${
                  activeTab === 'local'
                    ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30 font-semibold'
                    : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5]'
                }`}
              >
                <Upload className="w-3.5 h-3.5" />
                <span>Upload Local Video File</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setActiveTab('url');
                  setErrorMsg(null);
                }}
                className={`px-3 py-1.5 text-xs font-medium rounded-[2px] transition-colors flex items-center gap-1.5 ${
                  activeTab === 'url'
                    ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30 font-semibold'
                    : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5]'
                }`}
              >
                <LinkIcon className="w-3.5 h-3.5" />
                <span>Extract from Video Link</span>
              </button>
            </div>

            {/* Tab 1: Local File */}
            {activeTab === 'local' && (
              <div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="video/*"
                  onChange={handleFileChange}
                  className="hidden"
                />

                {!localFile ? (
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="p-8 border border-dashed border-slate-300 dark:border-[#263448] rounded-[2px] text-center cursor-pointer hover:border-[#3B82F6] transition"
                  >
                    <FileAudio className="w-10 h-10 text-slate-400 mx-auto mb-2" />
                    <p className="font-semibold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5]">
                      Click to choose video file (MP4, MOV, WEBM)
                    </p>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-1">
                      100% processed directly in your browser
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="p-3.5 rounded-[2px] bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <FileAudio className="w-5 h-5 text-purple-500" />
                        <div>
                          <p className="text-xs font-semibold text-slate-900 dark:text-[#E8EDF5]">
                            {localFile.name}
                          </p>
                          <p className="text-[10px] text-slate-500 dark:text-[#64748B] font-mono">
                            {(localFile.size / (1024 * 1024)).toFixed(1)} MB
                          </p>
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => setLocalFile(null)}
                        className="p-1 text-slate-400 hover:text-red-500"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>

                    <button
                      type="button"
                      disabled={isProcessing}
                      onClick={handleExtractLocal}
                      className="w-full h-11 bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] text-white font-medium text-xs sm:text-sm rounded-[2px] flex items-center justify-center gap-2 transition"
                    >
                      {isProcessing ? 'Extracting Audio Track...' : 'Extract & Download Audio'}
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Tab 2: URL */}
            {activeTab === 'url' && (
              <div className="space-y-3">
                <div className="flex flex-col sm:flex-row items-center gap-2">
                  <input
                    type="url"
                    value={urlInput}
                    onChange={(e) => setUrlInput(e.target.value)}
                    placeholder="Paste Reel, TikTok, or video link here..."
                    className="w-full h-11 px-3.5 bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] rounded-[2px] text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
                  />
                  <button
                    type="button"
                    disabled={isProcessing}
                    onClick={handleExtractUrl}
                    className="w-full sm:w-auto h-11 px-6 bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] text-white font-medium text-xs sm:text-sm rounded-[2px] flex items-center justify-center gap-2 shrink-0 transition"
                  >
                    {isProcessing ? 'Extracting...' : 'Extract Audio'}
                  </button>
                </div>
              </div>
            )}

            {/* Professional Progress Loader with Realistic Percentage */}
            {isProcessing && (
              <DownloaderProgressLoader
                platform="audio"
                isComplete={!isProcessing && audioUrl !== null}
              />
            )}

            {/* Error */}
            {errorMsg && (
              <div className="mt-4 p-3 bg-red-50 dark:bg-[#EF4444]/10 border border-red-200 dark:border-[#EF4444]/30 rounded-[2px] flex items-start gap-2 text-xs text-red-600 dark:text-[#EF4444]">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <p>{errorMsg}</p>
              </div>
            )}

            {/* Result Download Link */}
            {audioUrl && (
              <div ref={resultRef} className="mt-4 p-3 rounded-[2px] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 flex items-center justify-between">
                <span className="text-xs font-semibold text-emerald-800 dark:text-emerald-300 flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Audio Extracted Successfully!</span>
                </span>
                <a
                  href={audioUrl}
                  download={audioFilename}
                  className="h-8 px-3 rounded-[2px] text-xs font-semibold text-white bg-[#3B82F6] hover:bg-[#2563EB] flex items-center gap-1.5"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download Again</span>
                </a>
              </div>
            )}
          </div>
        </section>

        <CollegeStudyPromo />
      </main>

      {/* Compulsory Auth Modal */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={closeAuthModal}
        onSuccess={handleAuthSuccess}
        title="Sign In Required"
        subtitle="Sign in with Google, GitHub, X or College email to extract audio tracks."
      />

      <Footer />
    </div>
  );
}
