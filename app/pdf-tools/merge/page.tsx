'use client';

import React, { useState, useRef } from 'react';
import { PDFDocument, degrees, rgb, StandardFonts } from 'pdf-lib';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  FileText,
  Upload,
  Download,
  Trash2,
  ArrowUp,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Plus,
  CheckCircle2,
  Layers,
  FileCheck,
  RotateCw,
  Eye,
  Grid,
  List,
  SlidersHorizontal,
  Sparkles,
} from 'lucide-react';

interface PdfFileItem {
  id: string;
  file: File;
  name: string;
  size: number;
  pageCount: number;
}

interface PageItem {
  id: string;
  fileId: string;
  fileName: string;
  pageIndex: number; // 0-based
  displayPageNumber: number; // 1-based original
  rotation: number; // 0, 90, 180, 270
  thumbnailUrl?: string;
}

export default function MergePdfPage() {
  const [files, setFiles] = useState<PdfFileItem[]>([]);
  const [pages, setPages] = useState<PageItem[]>([]);
  const [viewMode, setViewMode] = useState<'vertical' | 'grid' | 'files'>('vertical');
  const [includeWatermark, setIncludeWatermark] = useState(true);
  const [isMerging, setIsMerging] = useState(false);
  const [isRenderingThumbnails, setIsRenderingThumbnails] = useState(false);
  const [mergedUrl, setMergedUrl] = useState<string | null>(null);
  const [outputFilename, setOutputFilename] = useState('merged_document.pdf');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const workspaceRef = useRef<HTMLDivElement>(null);

  // Load PDF.js helper for crisp client-side visual page thumbnails
  const loadPdfJs = async () => {
    if ((window as any).pdfjsLib) return (window as any).pdfjsLib;
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
    return (window as any).pdfjsLib;
  };

  const handleFiles = async (incoming: FileList | File[]) => {
    const validFiles = Array.from(incoming).filter(
      (f) => f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf')
    );

    if (validFiles.length === 0) return;

    setIsRenderingThumbnails(true);

    try {
      const pdfjsLib = await loadPdfJs().catch(() => null);
      const newFileItems: PdfFileItem[] = [];
      const newPageItems: PageItem[] = [];

      for (const file of validFiles) {
        const fileId = `${file.name}_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
        const arrayBuffer = await file.arrayBuffer();

        let pageCount = 1;
        let pdfDoc: any = null;

        if (pdfjsLib) {
          try {
            pdfDoc = await pdfjsLib.getDocument({ data: arrayBuffer.slice(0) }).promise;
            pageCount = pdfDoc.numPages;
          } catch {
            const doc = await PDFDocument.load(arrayBuffer, { ignoreEncryption: true });
            pageCount = doc.getPageCount();
          }
        } else {
          const doc = await PDFDocument.load(arrayBuffer, { ignoreEncryption: true });
          pageCount = doc.getPageCount();
        }

        newFileItems.push({
          id: fileId,
          file,
          name: file.name,
          size: file.size,
          pageCount,
        });

        // Render visual thumbnail for each individual page
        for (let i = 0; i < pageCount; i++) {
          let thumbUrl: string | undefined = undefined;
          if (pdfDoc) {
            try {
              const page = await pdfDoc.getPage(i + 1);
              const viewport = page.getViewport({ scale: 0.45 });
              const canvas = document.createElement('canvas');
              const ctx = canvas.getContext('2d');
              canvas.width = viewport.width;
              canvas.height = viewport.height;
              if (ctx) {
                await page.render({ canvasContext: ctx, viewport }).promise;
                thumbUrl = canvas.toDataURL('image/jpeg', 0.85);
              }
            } catch (err) {
              console.warn('Could not render page preview:', err);
            }
          }

          newPageItems.push({
            id: `${fileId}_p${i}_${Math.random().toString(36).substring(2, 6)}`,
            fileId,
            fileName: file.name,
            pageIndex: i,
            displayPageNumber: i + 1,
            rotation: 0,
            thumbnailUrl: thumbUrl,
          });
        }
      }

      setFiles((prev) => [...prev, ...newFileItems]);
      setPages((prev) => [...prev, ...newPageItems]);
      setMergedUrl(null);

      setTimeout(() => {
        workspaceRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 150);
    } catch (err) {
      console.error('Error reading PDF files:', err);
      alert('Unable to load some PDF pages. Make sure files are not password-protected.');
    } finally {
      setIsRenderingThumbnails(false);
    }
  };

  const handlePageMove = (index: number, direction: 'up' | 'down') => {
    const target = direction === 'up' ? index - 1 : index + 1;
    if (target < 0 || target >= pages.length) return;
    setPages((prev) => {
      const copy = [...prev];
      const temp = copy[index];
      copy[index] = copy[target];
      copy[target] = temp;
      return copy;
    });
  };

  const handlePageRotate = (pageId: string) => {
    setPages((prev) =>
      prev.map((p) => (p.id === pageId ? { ...p, rotation: (p.rotation + 90) % 360 } : p))
    );
  };

  const handlePageRemove = (pageId: string) => {
    setPages((prev) => prev.filter((p) => p.id !== pageId));
    setMergedUrl(null);
  };

  const handleFileRemove = (fileId: string) => {
    setFiles((prev) => prev.filter((f) => f.id !== fileId));
    setPages((prev) => prev.filter((p) => p.fileId !== fileId));
    setMergedUrl(null);
  };

  const handleRotateAll = () => {
    setPages((prev) => prev.map((p) => ({ ...p, rotation: (p.rotation + 90) % 360 })));
  };

  const handleMerge = async () => {
    if (pages.length < 2) {
      alert('Please have at least 2 pages to merge.');
      return;
    }

    setIsMerging(true);
    try {
      const mergedPdf = await PDFDocument.create();

      // Cache loaded source documents to prevent reading the same file multiple times
      const docCache = new Map<string, PDFDocument>();

      for (const item of files) {
        const buffer = await item.file.arrayBuffer();
        const loadedDoc = await PDFDocument.load(buffer, { ignoreEncryption: true });
        docCache.set(item.id, loadedDoc);
      }

      // Embed standard font for subtle watermark if enabled
      let watermarkFont = null;
      if (includeWatermark) {
        watermarkFont = await mergedPdf.embedFont(StandardFonts.HelveticaBold);
      }

      // Add pages in exact user-ordered sequence with custom rotation
      for (const pageItem of pages) {
        const srcDoc = docCache.get(pageItem.fileId);
        if (!srcDoc) continue;

        const [copiedPage] = await mergedPdf.copyPages(srcDoc, [pageItem.pageIndex]);

        // Apply rotation if modified
        if (pageItem.rotation > 0) {
          const currentRot = copiedPage.getRotation().angle;
          copiedPage.setRotation(degrees(currentRot + pageItem.rotation));
        }

        // Apply subtle professional watermark if checked
        if (includeWatermark && watermarkFont) {
          const { width, height } = copiedPage.getSize();
          copiedPage.drawText('Qwik Save — Save in One Click', {
            x: width * 0.15,
            y: height * 0.45,
            size: 26,
            font: watermarkFont,
            color: rgb(0.55, 0.65, 0.75),
            opacity: 0.18,
            rotate: degrees(45),
          });
        }

        mergedPdf.addPage(copiedPage);
      }

      const mergedPdfBytes = await mergedPdf.save();
      const blob = new Blob([mergedPdfBytes as unknown as BlobPart], { type: 'application/pdf' });
      const url = URL.createObjectURL(blob);
      setMergedUrl(url);

      const link = document.createElement('a');
      link.href = url;
      link.download = outputFilename.endsWith('.pdf') ? outputFilename : `${outputFilename}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (e: any) {
      console.error('Merge error:', e);
      alert('Failed to merge PDFs. Please make sure the files are not password-protected.');
    } finally {
      setIsMerging(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        <section className="pt-8 pb-10 sm:pt-12 sm:pb-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
              <Layers className="w-3.5 h-3.5" />
              <span>Merge PDF — Vertical Page-Level Organizer & Combiner</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Merge PDF Online — Top-to-Bottom Preview, Reorder & Delete Pages
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Combine multiple PDFs with top-to-bottom page-by-page visual previews. Move any page up or down, delete unwanted pages, rotate sideways scans 90°, and merge in one click. 100% Client-side, fast and secure.
            </p>
          </div>

          {/* Upload Dropzone */}
          <div
            onClick={() => fileInputRef.current?.click()}
            className="max-w-4xl mx-auto p-8 rounded-[2px] border border-dashed border-slate-300 dark:border-[#263448] hover:border-[#3B82F6] cursor-pointer bg-white dark:bg-[#141D2B] text-center transition shadow-xs"
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="application/pdf"
              onChange={(e) => e.target.files && handleFiles(e.target.files)}
              className="hidden"
            />
            <div className="w-12 h-12 rounded-[2px] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#94A3B8] flex items-center justify-center mx-auto mb-3">
              <Upload className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-sm sm:text-base text-slate-900 dark:text-[#E8EDF5] mb-1">
              Select or Drop PDF Files Here
            </h3>
            <p className="text-xs text-slate-500 dark:text-[#94A3B8] mb-4">
              Upload 2 or more PDF documents to inspect all pages in top-to-bottom sequence
            </p>
            <button
              type="button"
              className="h-10 px-5 bg-[#3B82F6] hover:bg-[#2563EB] text-white font-medium text-xs sm:text-sm rounded-[2px] inline-flex items-center gap-2 transition shadow-xs"
            >
              <Plus className="w-4 h-4" />
              <span>Browse PDF Files</span>
            </button>
          </div>
        </section>

        {/* Thumbnail Generating Indicator */}
        {isRenderingThumbnails && (
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
            <div className="p-3 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/40 rounded-[2px] flex items-center gap-3 text-xs text-blue-700 dark:text-blue-300">
              <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
              <span>Extracting and rendering top-to-bottom visual previews for all pages...</span>
            </div>
          </div>
        )}

        {/* Workspace: Top-to-Bottom Vertical Stack & Reordering */}
        {pages.length > 0 && (
          <section ref={workspaceRef} className="py-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-white dark:bg-[#141D2B] rounded-[2px] border border-slate-200 dark:border-[#263448] shadow-sm p-5 sm:p-6">
              {/* Header with View Toggle and Stats */}
              <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-200 dark:border-[#263448]">
                <div>
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
                    <span>Document Page Sequence</span>
                    <span className="px-2 py-0.5 rounded-[2px] bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 text-xs font-mono font-bold">
                      {pages.length} Pages from {files.length} Files
                    </span>
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-[#94A3B8] mt-1">
                    Arrange pages top-to-bottom. Move any page up/down, delete unwanted pages, or rotate before merging.
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {/* View Mode Toggle: Vertical Stack vs Grid vs Files */}
                  <div className="flex items-center bg-slate-100 dark:bg-[#0F1623] p-0.5 rounded-[2px] border border-slate-200 dark:border-[#263448]">
                    <button
                      type="button"
                      onClick={() => setViewMode('vertical')}
                      className={`px-3 py-1.5 text-xs font-semibold rounded-[2px] flex items-center gap-1.5 transition ${
                        viewMode === 'vertical'
                          ? 'bg-white dark:bg-[#1E293B] text-blue-600 dark:text-blue-400 shadow-xs'
                          : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                      }`}
                      title="Top to Bottom Vertical Document Stack"
                    >
                      <List className="w-3.5 h-3.5" />
                      <span>Top to Bottom</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setViewMode('grid')}
                      className={`px-3 py-1.5 text-xs font-semibold rounded-[2px] flex items-center gap-1.5 transition ${
                        viewMode === 'grid'
                          ? 'bg-white dark:bg-[#1E293B] text-blue-600 dark:text-blue-400 shadow-xs'
                          : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                      }`}
                      title="Grid Overview"
                    >
                      <Grid className="w-3.5 h-3.5" />
                      <span>Grid View</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setViewMode('files')}
                      className={`px-3 py-1.5 text-xs font-semibold rounded-[2px] flex items-center gap-1.5 transition ${
                        viewMode === 'files'
                          ? 'bg-white dark:bg-[#1E293B] text-blue-600 dark:text-blue-400 shadow-xs'
                          : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                      }`}
                      title="Source Files Overview"
                    >
                      <Layers className="w-3.5 h-3.5" />
                      <span>Files List</span>
                    </button>
                  </div>

                  <button
                    type="button"
                    onClick={handleRotateAll}
                    className="h-8 px-2.5 rounded-[2px] text-xs font-medium text-slate-700 dark:text-[#94A3B8] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:text-slate-900 dark:hover:text-white flex items-center gap-1.5 transition"
                    title="Rotate all pages 90° clockwise"
                  >
                    <RotateCw className="w-3.5 h-3.5" />
                    <span>Rotate All</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="h-8 px-3 rounded-[2px] text-xs font-medium text-slate-700 dark:text-[#94A3B8] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] hover:text-slate-900 dark:hover:text-white flex items-center gap-1.5 transition"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add More PDFs</span>
                  </button>
                </div>
              </div>

              {/* View 1: Top-to-Bottom Vertical Stack (Requested by user) */}
              {viewMode === 'vertical' && (
                <div className="space-y-4 mb-6">
                  {pages.map((p, idx) => (
                    <div
                      key={p.id}
                      className="group p-4 bg-slate-50 dark:bg-[#111A29] rounded-[2px] border border-slate-200 dark:border-[#263448] hover:border-blue-500/60 transition-all shadow-xs flex flex-col md:flex-row items-center justify-between gap-5"
                    >
                      {/* Left: Sequence badge & document metadata */}
                      <div className="w-full md:w-56 shrink-0 space-y-2 text-left">
                        <div className="flex items-center gap-2">
                          <span className="w-8 h-8 rounded-[2px] bg-blue-600 text-white font-mono font-bold text-sm flex items-center justify-center shadow-xs">
                            #{idx + 1}
                          </span>
                          <div>
                            <span className="text-[11px] font-bold text-slate-900 dark:text-[#E8EDF5] block">
                              Page {idx + 1} of {pages.length}
                            </span>
                            <span className="text-[10px] text-slate-500 dark:text-[#94A3B8] font-mono">
                              Original Page {p.displayPageNumber}
                            </span>
                          </div>
                        </div>

                        <div className="p-2 rounded-[2px] bg-white dark:bg-[#0B121E] border border-slate-200 dark:border-[#1E2C42]">
                          <p className="text-[11px] font-medium text-slate-700 dark:text-[#CBD5E1] truncate" title={p.fileName}>
                            {p.fileName}
                          </p>
                          {p.rotation > 0 && (
                            <span className="inline-block mt-1 text-[9px] font-mono font-bold px-1.5 py-0.5 rounded-[2px] bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                              Rotated {p.rotation}°
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Center: Large Crisp Page Thumbnail Preview */}
                      <div className="flex-1 flex justify-center items-center py-1">
                        <div className="w-48 sm:w-56 aspect-[3/4] bg-white dark:bg-[#090D14] rounded-[2px] border border-slate-300 dark:border-[#263448] shadow-sm flex items-center justify-center overflow-hidden relative transition-transform duration-200">
                          {p.thumbnailUrl ? (
                            <img
                              src={p.thumbnailUrl}
                              alt={`Page ${idx + 1}`}
                              className="w-full h-full object-contain transition-transform duration-200"
                              style={{ transform: `rotate(${p.rotation}deg)` }}
                            />
                          ) : (
                            <div className="flex flex-col items-center justify-center p-4 text-slate-400 text-center">
                              <FileText className="w-10 h-10 mb-2 opacity-40 text-blue-500" />
                              <span className="text-xs font-mono font-semibold">Page {p.displayPageNumber}</span>
                              <span className="text-[10px] text-slate-500 mt-1">Generating preview...</span>
                            </div>
                          )}
                          {p.rotation > 0 && (
                            <span className="absolute bottom-2 right-2 text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/75 text-white">
                              {p.rotation}°
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Right: Explicit Action Controls (Move Up, Move Down, Rotate 90°, Remove) */}
                      <div className="w-full md:w-52 shrink-0 flex md:flex-col items-center justify-center gap-2 pt-2 md:pt-0 border-t md:border-t-0 md:border-l border-slate-200 dark:border-[#263448] md:pl-5">
                        <button
                          type="button"
                          disabled={idx === 0}
                          onClick={() => handlePageMove(idx, 'up')}
                          className="flex-1 md:w-full h-9 rounded-[2px] bg-white dark:bg-[#1A2536] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-2 text-xs font-medium text-slate-700 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-blue-400 hover:border-blue-500 disabled:opacity-30 disabled:cursor-not-allowed transition"
                          title="Move page upwards"
                        >
                          <ArrowUp className="w-3.5 h-3.5" />
                          <span>Move Up</span>
                        </button>

                        <button
                          type="button"
                          disabled={idx === pages.length - 1}
                          onClick={() => handlePageMove(idx, 'down')}
                          className="flex-1 md:w-full h-9 rounded-[2px] bg-white dark:bg-[#1A2536] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-2 text-xs font-medium text-slate-700 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-blue-400 hover:border-blue-500 disabled:opacity-30 disabled:cursor-not-allowed transition"
                          title="Move page downwards"
                        >
                          <ArrowDown className="w-3.5 h-3.5" />
                          <span>Move Down</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => handlePageRotate(p.id)}
                          className="flex-1 md:w-full h-9 rounded-[2px] bg-white dark:bg-[#1A2536] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-2 text-xs font-medium text-slate-700 dark:text-[#94A3B8] hover:text-emerald-600 dark:hover:text-emerald-400 hover:border-emerald-500 transition"
                          title="Rotate 90° Clockwise"
                        >
                          <RotateCw className="w-3.5 h-3.5" />
                          <span>Rotate 90°</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => handlePageRemove(p.id)}
                          className="flex-1 md:w-full h-9 rounded-[2px] bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900/50 flex items-center justify-center gap-2 text-xs font-medium text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/50 transition"
                          title="Delete this page completely"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>Delete Page</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* View 2: Pages Grid View */}
              {viewMode === 'grid' && (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4 mb-6">
                  {pages.map((p, idx) => (
                    <div
                      key={p.id}
                      className="group relative bg-slate-50 dark:bg-[#111A29] rounded-[2px] border border-slate-200 dark:border-[#263448] p-2.5 flex flex-col justify-between hover:border-blue-500/60 transition shadow-xs"
                    >
                      <div className="flex items-center justify-between mb-1.5 text-[11px]">
                        <span className="font-mono font-bold px-1.5 py-0.5 rounded-[2px] bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300">
                          #{idx + 1}
                        </span>
                        <button
                          type="button"
                          onClick={() => handlePageRemove(p.id)}
                          className="p-1 text-slate-400 hover:text-red-500 rounded transition"
                          title="Remove this page"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <div className="aspect-[3/4] bg-white dark:bg-[#090D14] rounded-[2px] border border-slate-200 dark:border-slate-800 flex items-center justify-center overflow-hidden relative shadow-inner my-1">
                        {p.thumbnailUrl ? (
                          <img
                            src={p.thumbnailUrl}
                            alt={`Page ${idx + 1}`}
                            className="w-full h-full object-contain transition-transform duration-200"
                            style={{ transform: `rotate(${p.rotation}deg)` }}
                          />
                        ) : (
                          <div className="flex flex-col items-center justify-center p-2 text-slate-400">
                            <FileText className="w-6 h-6 mb-1 opacity-50" />
                            <span className="text-[10px] font-mono">Page {p.displayPageNumber}</span>
                          </div>
                        )}
                        {p.rotation > 0 && (
                          <span className="absolute bottom-1 right-1 text-[9px] font-mono px-1 rounded bg-black/60 text-white">
                            {p.rotation}°
                          </span>
                        )}
                      </div>

                      <p className="text-[10px] text-slate-500 dark:text-[#94A3B8] truncate mb-2 mt-1" title={p.fileName}>
                        {p.fileName}
                      </p>

                      <div className="grid grid-cols-3 gap-1 pt-1.5 border-t border-slate-200 dark:border-[#263448]">
                        <button
                          type="button"
                          disabled={idx === 0}
                          onClick={() => handlePageMove(idx, 'up')}
                          className="h-7 rounded-[2px] bg-white dark:bg-[#1A2536] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-600 dark:text-[#94A3B8] hover:text-blue-600 disabled:opacity-30 disabled:cursor-not-allowed transition"
                          title="Move Earlier"
                        >
                          <ArrowLeft className="w-3 h-3" />
                        </button>

                        <button
                          type="button"
                          onClick={() => handlePageRotate(p.id)}
                          className="h-7 rounded-[2px] bg-white dark:bg-[#1A2536] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-600 dark:text-[#94A3B8] hover:text-emerald-500 transition"
                          title="Rotate 90°"
                        >
                          <RotateCw className="w-3 h-3" />
                        </button>

                        <button
                          type="button"
                          disabled={idx === pages.length - 1}
                          onClick={() => handlePageMove(idx, 'down')}
                          className="h-7 rounded-[2px] bg-white dark:bg-[#1A2536] border border-slate-200 dark:border-[#263448] flex items-center justify-center text-slate-600 dark:text-[#94A3B8] hover:text-blue-600 disabled:opacity-30 disabled:cursor-not-allowed transition"
                          title="Move Later"
                        >
                          <ArrowRight className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* View 3: Files List View */}
              {viewMode === 'files' && (
                <div className="space-y-2 mb-6">
                  {files.map((item, idx) => (
                    <div
                      key={item.id}
                      className="p-3.5 rounded-[2px] bg-slate-50 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-between gap-3"
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-6 h-6 rounded-[2px] bg-slate-200 dark:bg-[#1E293B] text-[10px] font-mono font-bold flex items-center justify-center">
                          #{idx + 1}
                        </span>
                        <FileText className="w-4 h-4 text-blue-500" />
                        <div>
                          <p className="text-xs font-semibold text-slate-900 dark:text-[#E8EDF5]">
                            {item.name}
                          </p>
                          <p className="text-[10px] text-slate-500 dark:text-[#64748B] font-mono">
                            {formatFileSize(item.size)} • {item.pageCount} total pages
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => handleFileRemove(item.id)}
                          className="p-1.5 text-red-500 hover:text-red-700 transition"
                          title="Remove entire file and all its pages"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Watermark Preference & Filename Bar */}
              <div className="p-3.5 mb-4 bg-slate-50 dark:bg-[#0B121E] rounded-[2px] border border-slate-200 dark:border-[#1E2C42] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <label className="flex items-center gap-2.5 cursor-pointer text-xs font-medium text-slate-700 dark:text-[#CBD5E1]">
                  <input
                    type="checkbox"
                    checked={includeWatermark}
                    onChange={(e) => setIncludeWatermark(e.target.checked)}
                    className="w-4 h-4 rounded-[2px] border-slate-300 dark:border-[#263448] text-blue-600 focus:ring-blue-500 cursor-pointer"
                  />
                  <span>Add Subtle &quot;Qwik Save&quot; Brand Watermark on Merged Pages (Subtle Opacity)</span>
                </label>

                <div className="flex items-center gap-2">
                  <label className="text-xs text-slate-500 dark:text-[#94A3B8] font-medium whitespace-nowrap">
                    Filename:
                  </label>
                  <input
                    type="text"
                    value={outputFilename}
                    onChange={(e) => setOutputFilename(e.target.value)}
                    className="h-8 px-2.5 rounded-[2px] bg-white dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] w-48 sm:w-56 focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Bottom Execution Bar */}
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-slate-200 dark:border-[#263448]">
                <p className="text-xs text-slate-500 dark:text-[#94A3B8]">
                  {pages.length} pages will be merged in the exact top-to-bottom sequence displayed above.
                </p>

                <div className="flex items-center gap-2 w-full sm:w-auto">
                  {mergedUrl && (
                    <a
                      href={mergedUrl}
                      download={outputFilename}
                      className="h-10 px-4 rounded-[2px] font-medium text-xs text-slate-700 dark:text-[#E8EDF5] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-1.5 transition"
                    >
                      <Download className="w-4 h-4" />
                      <span>Download Again</span>
                    </a>
                  )}

                  <button
                    type="button"
                    disabled={isMerging || pages.length < 2}
                    onClick={handleMerge}
                    className="h-10 px-6 rounded-[2px] font-semibold text-xs sm:text-sm text-white bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] transition flex items-center justify-center gap-2 shadow-xs"
                  >
                    {isMerging ? (
                      <>
                        <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                        <span>Merging {pages.length} Pages...</span>
                      </>
                    ) : (
                      <>
                        <FileCheck className="w-4 h-4" />
                        <span>Merge {pages.length} Pages & Download</span>
                      </>
                    )}
                  </button>
                </div>
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
