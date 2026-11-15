'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  FileText,
  Upload,
  Download,
  Trash2,
  Images,
  CheckCircle2,
  FileCheck,
} from 'lucide-react';

interface RenderedPage {
  pageNumber: number;
  dataUrl: string;
}

export default function PdfToImagePage() {
  const [file, setFile] = useState<File | null>(null);
  const [isConverting, setIsConverting] = useState(false);
  const [renderedPages, setRenderedPages] = useState<RenderedPage[]>([]);
  const [format, setFormat] = useState<'jpg' | 'png'>('jpg');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = (f: File) => {
    if (f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf')) {
      setFile(f);
      setRenderedPages([]);
    }
  };

  const handleConvert = async () => {
    if (!file) return;

    setIsConverting(true);
    setRenderedPages([]);

    try {
      // Dynamically load PDF.js from cdnjs to avoid any bundling/worker issues
      if (!(window as any).pdfjsLib) {
        await new Promise((resolve, reject) => {
          const script = document.createElement('script');
          script.src = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js';
          script.onload = () => {
            (window as any).pdfjsLib.GlobalWorkerOptions.workerSrc =
              'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
            resolve(true);
          };
          script.onerror = reject;
          document.head.appendChild(script);
        });
      }

      const pdfjsLib = (window as any).pdfjsLib;
      const arrayBuffer = await file.arrayBuffer();
      const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
      const pages: RenderedPage[] = [];

      for (let i = 1; i <= pdf.numPages; i++) {
        const page = await pdf.getPage(i);
        const viewport = page.getViewport({ scale: 2.0 }); // 2x scale for high resolution

        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        canvas.height = viewport.height;
        canvas.width = viewport.width;

        if (context) {
          await page.render({ canvasContext: context, viewport }).promise;
          const mime = format === 'png' ? 'image/png' : 'image/jpeg';
          const dataUrl = canvas.toDataURL(mime, 0.95);
          pages.push({ pageNumber: i, dataUrl });
        }
      }

      setRenderedPages(pages);
    } catch (e: any) {
      console.error('PDF to Image error:', e);
      alert('Unable to extract images. The PDF may be encrypted or corrupted.');
    } finally {
      setIsConverting(false);
    }
  };

  const downloadImage = (dataUrl: string, pageNum: number) => {
    const a = document.createElement('a');
    a.href = dataUrl;
    const baseName = file ? file.name.replace(/\.[^/.]+$/, '') : 'page';
    a.download = `${baseName}_page_${pageNum}.${format}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
              <Images className="w-3.5 h-3.5" />
              <span>PDF to Image — Free Online PDF to JPG / PNG</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              PDF to Image Converter — Convert PDF to JPG & PNG Free
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Extract every page of your PDF as high-resolution 300 DPI JPG or PNG images. 100% Client-side, fast, and no server upload required.
            </p>
          </div>

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
              Select or Drop PDF File Here
            </h3>
            <p className="text-xs text-slate-500 dark:text-[#94A3B8] mb-4">
              Select a PDF document to convert into images
            </p>
            <button
              type="button"
              className="h-10 px-5 bg-[#3B82F6] hover:bg-[#2563EB] text-white font-medium text-xs sm:text-sm rounded-[2px] inline-flex items-center gap-2 transition"
            >
              <Upload className="w-4 h-4" />
              <span>Browse PDF File</span>
            </button>
          </div>
        </section>

        {file && (
          <section className="py-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-white dark:bg-[#141D2B] rounded-[2px] border border-slate-200 dark:border-[#263448] shadow-sm p-6 max-w-4xl mx-auto mb-8">
              <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-200 dark:border-[#263448]">
                <div className="flex items-center gap-3">
                  <FileText className="w-6 h-6 text-blue-500" />
                  <div>
                    <h4 className="font-semibold text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5]">
                      {file.name}
                    </h4>
                    <p className="text-[11px] text-slate-500 dark:text-[#94A3B8] font-mono">
                      {(file.size / 1024).toFixed(1)} KB
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <select
                    value={format}
                    onChange={(e) => setFormat(e.target.value as any)}
                    className="h-8 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-800 dark:text-[#E8EDF5]"
                  >
                    <option value="jpg">JPG Format</option>
                    <option value="png">PNG Format</option>
                  </select>

                  <button
                    type="button"
                    disabled={isConverting}
                    onClick={handleConvert}
                    className="h-8 px-4 rounded-[2px] font-semibold text-xs text-white bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] transition flex items-center gap-1.5"
                  >
                    {isConverting ? 'Rendering...' : 'Convert to Images'}
                  </button>
                </div>
              </div>
            </div>

            {/* Display rendered pages */}
            {renderedPages.length > 0 && (
              <div className="max-w-7xl mx-auto">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold text-sm text-slate-900 dark:text-[#E8EDF5]">
                    Extracted Pages ({renderedPages.length})
                  </h3>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                  {renderedPages.map((p) => (
                    <div
                      key={p.pageNumber}
                      className="rounded-[2px] border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#141D2B] overflow-hidden flex flex-col"
                    >
                      <div className="relative aspect-[3/4] bg-slate-100 dark:bg-[#0F1623] p-2 flex items-center justify-center">
                        <img
                          src={p.dataUrl}
                          alt={`Page ${p.pageNumber}`}
                          className="max-w-full max-h-full object-contain shadow-sm"
                        />
                        <span className="absolute top-1.5 left-1.5 px-1 py-0.5 rounded-[2px] text-[9px] font-mono font-bold bg-slate-900/80 text-white">
                          Page {p.pageNumber}
                        </span>
                      </div>

                      <div className="p-2 border-t border-slate-200 dark:border-[#263448] mt-auto">
                        <button
                          type="button"
                          onClick={() => downloadImage(p.dataUrl, p.pageNumber)}
                          className="w-full h-8 rounded-[2px] text-xs font-semibold text-white bg-[#3B82F6] hover:bg-[#2563EB] flex items-center justify-center gap-1.5 transition"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download {format.toUpperCase()}</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
