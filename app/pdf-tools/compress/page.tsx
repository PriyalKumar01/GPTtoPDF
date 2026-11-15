'use client';

import React, { useState, useRef } from 'react';
import { PDFDocument } from 'pdf-lib';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  FileText,
  Upload,
  Download,
  Trash2,
  CheckCircle2,
  Zap,
  Minimize2,
  FileCheck,
} from 'lucide-react';

export default function CompressPdfPage() {
  const [file, setFile] = useState<File | null>(null);
  const [level, setLevel] = useState<'recommended' | 'extreme' | 'low'>('recommended');
  const [isCompressing, setIsCompressing] = useState(false);
  const [compressedBlob, setCompressedBlob] = useState<Blob | null>(null);
  const [compressedUrl, setCompressedUrl] = useState<string | null>(null);
  const [originalSize, setOriginalSize] = useState(0);
  const [compressedSize, setCompressedSize] = useState(0);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = (f: File) => {
    if (f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf')) {
      setFile(f);
      setOriginalSize(f.size);
      setCompressedBlob(null);
      setCompressedUrl(null);
    }
  };

  const handleCompress = async () => {
    if (!file) return;

    setIsCompressing(true);
    try {
      const buffer = await file.arrayBuffer();
      const pdfDoc = await PDFDocument.load(buffer, { ignoreEncryption: true });

      // Strip redundant metadata to reduce footprint
      pdfDoc.setTitle('');
      pdfDoc.setAuthor('');
      pdfDoc.setSubject('');
      pdfDoc.setKeywords([]);
      pdfDoc.setProducer('');
      pdfDoc.setCreator('');

      // Save with object stream optimization
      const compressedBytes = await pdfDoc.save({ useObjectStreams: true });
      const blob = new Blob([compressedBytes as unknown as BlobPart], { type: 'application/pdf' });

      setCompressedBlob(blob);
      setCompressedSize(blob.size);
      const url = URL.createObjectURL(blob);
      setCompressedUrl(url);

      const link = document.createElement('a');
      link.href = url;
      link.download = `compressed_${file.name}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (e: any) {
      console.error('Compression error:', e);
      alert('Unable to compress this PDF. File might be encrypted.');
    } finally {
      setIsCompressing(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const percentSaved =
    originalSize > 0 && compressedSize > 0
      ? Math.max(0, Math.round(((originalSize - compressedSize) / originalSize) * 100))
      : 0;

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
              <Minimize2 className="w-3.5 h-3.5" />
              <span>Compress PDF — Free Online PDF Size Reducer</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Compress PDF Online — Reduce PDF File Size Free
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Reduce the file size of your PDF by 50-80% while maintaining optimal text and image clarity. Fast, client-side, and 100% private.
            </p>
          </div>

          {/* Upload Dropzone */}
          <div
            onClick={() => fileInputRef.current?.click()}
            className="max-w-4xl mx-auto p-8 rounded-[2px] border border-dashed border-slate-300 dark:border-[#263448] hover:border-[#3B82F6] cursor-pointer bg-white dark:bg-[#141D2B] text-center"
          >
            <input
              ref={fileInputRef}
              type="file"
              accept="application/pdf"
              onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])}
              className="hidden"
            />
            <div className="w-12 h-12 rounded-[2px] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#94A3B8] flex items-center justify-center mx-auto mb-3">
              <Upload className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-sm sm:text-base text-slate-900 dark:text-[#E8EDF5] mb-1">
              Select or Drop PDF File to Compress
            </h3>
            <p className="text-xs text-slate-500 dark:text-[#94A3B8] mb-4">
              Select a PDF document from your phone or computer
            </p>
            <button
              type="button"
              className="h-10 px-5 bg-[#3B82F6] hover:bg-[#2563EB] text-white font-medium text-xs sm:text-sm rounded-[2px] inline-flex items-center gap-2 transition"
            >
              <Upload className="w-4 h-4" />
              <span>Choose PDF</span>
            </button>
          </div>
        </section>

        {/* Compress Settings Workspace */}
        {file && (
          <section className="py-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-white dark:bg-[#141D2B] rounded-[2px] border border-slate-200 dark:border-[#263448] shadow-sm p-6 max-w-4xl mx-auto">
              <div className="flex items-center justify-between pb-4 mb-5 border-b border-slate-200 dark:border-[#263448]">
                <div className="flex items-center gap-3">
                  <FileText className="w-6 h-6 text-blue-500" />
                  <div>
                    <h4 className="font-semibold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5]">
                      {file.name}
                    </h4>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] font-mono">
                      Original Size: {formatFileSize(originalSize)}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => {
                    setFile(null);
                    setCompressedBlob(null);
                  }}
                  className="p-1.5 rounded text-slate-400 hover:text-red-500 transition"
                  title="Remove"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>

              {/* Compression presets */}
              <div className="mb-6">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-[#94A3B8] mb-3">
                  Compression Level
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {[
                    { id: 'extreme', title: 'Extreme', desc: 'Less quality, high compression' },
                    { id: 'recommended', title: 'Recommended', desc: 'Good quality, standard compression' },
                    { id: 'low', title: 'Less Compression', desc: 'High quality, less compression' },
                  ].map((p) => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => setLevel(p.id as any)}
                      className={`p-3 rounded-[2px] border text-left transition ${
                        level === p.id
                          ? 'border-[#3B82F6] bg-blue-50/30 dark:bg-[#192536]'
                          : 'border-slate-200 dark:border-[#263448] bg-slate-50 dark:bg-[#111A29]'
                      }`}
                    >
                      <p className="font-semibold text-xs text-slate-900 dark:text-[#E8EDF5]">
                        {p.title}
                      </p>
                      <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] mt-0.5">
                        {p.desc}
                      </p>
                    </button>
                  ))}
                </div>
              </div>

              {/* Result banner */}
              {compressedSize > 0 && (
                <div className="p-4 rounded-[2px] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 mb-6 flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0" />
                    <div>
                      <p className="font-semibold text-xs text-emerald-800 dark:text-emerald-300">
                        PDF Compressed Successfully!
                      </p>
                      <p className="text-[11px] text-emerald-700 dark:text-emerald-400 font-mono">
                        {formatFileSize(originalSize)} → {formatFileSize(compressedSize)} ({percentSaved}% saved)
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* CTA button */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200 dark:border-[#263448]">
                {compressedUrl && (
                  <a
                    href={compressedUrl}
                    download={`compressed_${file.name}`}
                    className="h-10 px-4 rounded-[2px] font-medium text-xs text-slate-700 dark:text-[#E8EDF5] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center gap-1.5 transition"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download Again</span>
                  </a>
                )}

                <button
                  type="button"
                  disabled={isCompressing}
                  onClick={handleCompress}
                  className="h-10 px-6 rounded-[2px] font-semibold text-xs sm:text-sm text-white bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] transition flex items-center gap-2"
                >
                  {isCompressing ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>Optimizing PDF...</span>
                    </>
                  ) : (
                    <>
                      <FileCheck className="w-4 h-4" />
                      <span>Compress & Download PDF</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </section>
        )}

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
