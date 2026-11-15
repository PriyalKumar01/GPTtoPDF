'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  Images,
  Upload,
  FileUp,
  Download,
  Trash2,
  RotateCw,
  ArrowLeft,
  ArrowRight,
  Plus,
  Settings2,
  CheckCircle2,
  FileCheck,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import {
  ImageToPdfItem,
  ImageToPdfOptions,
  convertImagesToPdf,
} from '@/lib/image-to-pdf';

export default function ImageToPdfPage() {
  const [items, setItems] = useState<ImageToPdfItem[]>([]);
  const [options, setOptions] = useState<ImageToPdfOptions>({
    pageSize: 'A4',
    orientation: 'auto',
    margin: 'none',
    filename: 'images-converted.pdf',
  });
  const [isConverting, setIsConverting] = useState(false);
  const [progress, setProgress] = useState<{ current: number; total: number } | null>(null);
  const [pdfBlob, setPdfBlob] = useState<Blob | null>(null);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [faqOpen, setFaqOpen] = useState<number | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const workspaceRef = useRef<HTMLDivElement>(null);

  const processFiles = (files: FileList | File[]) => {
    const validImageTypes = [
      'image/jpeg',
      'image/png',
      'image/webp',
      'image/gif',
      'image/bmp',
      'image/svg+xml',
    ];

    const newItems: Promise<ImageToPdfItem>[] = Array.from(files)
      .filter((file) => validImageTypes.includes(file.type) || /\.(jpe?g|png|webp|gif|bmp|svg)$/i.test(file.name))
      .map((file) => {
        return new Promise((resolve) => {
          const previewUrl = URL.createObjectURL(file);
          const img = new Image();
          img.onload = () => {
            resolve({
              id: `${file.name}_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
              file,
              previewUrl,
              name: file.name,
              size: file.size,
              width: img.naturalWidth || 800,
              height: img.naturalHeight || 600,
              rotation: 0,
            });
          };
          img.onerror = () => {
            resolve({
              id: `${file.name}_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
              file,
              previewUrl,
              name: file.name,
              size: file.size,
              width: 800,
              height: 600,
              rotation: 0,
            });
          };
          img.src = previewUrl;
        });
      });

    Promise.all(newItems).then((loaded) => {
      setItems((prev) => [...prev, ...loaded]);
      setPdfBlob(null);
      setPdfUrl(null);
      setTimeout(() => {
        workspaceRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 150);
    });
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      processFiles(e.dataTransfer.files);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      processFiles(e.target.files);
    }
  };

  const handleRotate = (id: string) => {
    setItems((prev) =>
      prev.map((item) => {
        if (item.id === id) {
          const nextRotation = (item.rotation + 90) % 360;
          return { ...item, rotation: nextRotation };
        }
        return item;
      })
    );
  };

  const handleMove = (index: number, direction: 'left' | 'right') => {
    const targetIndex = direction === 'left' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= items.length) return;

    setItems((prev) => {
      const copy = [...prev];
      const temp = copy[index];
      copy[index] = copy[targetIndex];
      copy[targetIndex] = temp;
      return copy;
    });
  };

  const handleRemove = (id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id));
    setPdfBlob(null);
    setPdfUrl(null);
  };

  const handleClearAll = () => {
    setItems([]);
    setPdfBlob(null);
    setPdfUrl(null);
  };

  const handleConvert = async () => {
    if (items.length === 0) return;

    setIsConverting(true);
    setProgress({ current: 0, total: items.length });

    try {
      const blob = await convertImagesToPdf(items, options, (current, total) => {
        setProgress({ current, total });
      });

      setPdfBlob(blob);
      const url = URL.createObjectURL(blob);
      setPdfUrl(url);

      const link = document.createElement('a');
      link.href = url;
      link.download = options.filename.endsWith('.pdf')
        ? options.filename
        : `${options.filename}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      console.error('PDF Conversion error:', err);
      alert('Failed to generate PDF. Please try again.');
    } finally {
      setIsConverting(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const faqs = [
    {
      q: 'Is my data and images private?',
      a: '100% yes! All conversion happens directly inside your web browser using client-side JavaScript. Your photos are NEVER uploaded to any remote server or stored anywhere.',
    },
    {
      q: 'Which image formats can I convert to PDF?',
      a: 'You can convert JPG, JPEG, PNG, WEBP, GIF, and BMP images into a single clean PDF document.',
    },
    {
      q: 'Can I reorder or rotate the images before converting?',
      a: 'Yes! You can reorder images by clicking the left/right arrow buttons, or rotate individual photos 90° clockwise with one click.',
    },
    {
      q: 'What is the "Fit to Image" page size option?',
      a: '"Fit to Image" adjusts each PDF page dimensions to exactly match the resolution and aspect ratio of your photo, eliminating any white sidebars or letterboxing.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        {/* Hero Section */}
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
              <Images className="w-3.5 h-3.5" />
              <span>Img to PDF — Free Online Image to PDF Converter</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Img to PDF — Convert JPG, PNG & Photos to PDF Free
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Merge, reorder, rotate, and convert multiple photos (JPG, PNG, WEBP) into one high-quality PDF document.
              Fast, completely private, and works 100% in your browser without uploading to any server.
            </p>
          </div>

          {/* Dropzone Container */}
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`max-w-4xl mx-auto p-8 sm:p-10 rounded-[2px] border border-dashed transition-all cursor-pointer bg-white dark:bg-[#141D2B] ${
              dragOver
                ? 'border-blue-500 bg-blue-50/20 dark:bg-[#192536]'
                : 'border-slate-300 dark:border-[#263448] hover:border-[#3B82F6]'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp,image/gif,image/bmp"
              onChange={handleFileChange}
              className="hidden"
            />

            <div className="w-12 h-12 rounded-[2px] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#94A3B8] flex items-center justify-center mx-auto mb-3">
              <FileUp className="w-6 h-6" />
            </div>

            <h3 className="font-semibold text-sm sm:text-base text-slate-900 dark:text-[#E8EDF5] mb-1 text-center">
              Select or Drop Images Here
            </h3>
            <p className="text-xs text-slate-500 dark:text-[#94A3B8] mb-4 text-center">
              Supports JPG, PNG, WEBP, GIF, and BMP (Batch selection supported)
            </p>

            <div className="flex justify-center">
              <button
                type="button"
                className="h-10 px-5 bg-[#3B82F6] hover:bg-[#2563EB] text-white font-medium text-xs sm:text-sm rounded-[2px] flex items-center gap-2 transition"
              >
                <Upload className="w-4 h-4" />
                <span>Browse Images</span>
              </button>
            </div>
          </div>
        </section>

        {/* Image Queue & PDF Settings Workspace */}
        {items.length > 0 && (
          <section ref={workspaceRef} className="py-8 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex items-center justify-between flex-wrap gap-4 mb-4">
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-[#E8EDF5] flex items-center gap-2">
                  <Images className="w-4 h-4 text-blue-500" />
                  <span>Images in Queue ({items.length})</span>
                </h3>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="h-8 px-3 rounded-[2px] text-xs font-semibold text-slate-700 dark:text-[#94A3B8] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] hover:text-blue-600 transition flex items-center gap-1.5"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add More</span>
                </button>

                <button
                  type="button"
                  onClick={handleClearAll}
                  className="h-8 px-3 rounded-[2px] text-xs font-semibold text-red-600 dark:text-[#EF4444] bg-red-50 dark:bg-[#EF4444]/10 border border-red-200 dark:border-[#EF4444]/30 transition flex items-center gap-1.5"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Clear All</span>
                </button>
              </div>
            </div>

            {/* Grid of uploaded images */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3 mb-6">
              {items.map((item, index) => (
                <div
                  key={item.id}
                  className="rounded-[2px] border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#141D2B] overflow-hidden shadow-sm flex flex-col"
                >
                  <div className="relative aspect-[3/4] bg-slate-100 dark:bg-[#0F1623] flex items-center justify-center overflow-hidden p-2">
                    <img
                      src={item.previewUrl}
                      alt={item.name}
                      style={{
                        transform: `rotate(${item.rotation}deg)`,
                        transition: 'transform 0.2s ease',
                      }}
                      className="max-w-full max-h-full object-contain"
                    />

                    <span className="absolute top-1.5 left-1.5 px-1 py-0.5 rounded-[2px] text-[9px] font-mono font-bold bg-slate-900/80 text-white">
                      #{index + 1}
                    </span>

                    <button
                      type="button"
                      onClick={() => handleRotate(item.id)}
                      className="absolute top-1.5 right-1.5 p-1 rounded-[2px] bg-white dark:bg-[#141D2B] text-slate-700 dark:text-[#E8EDF5] border border-slate-200 dark:border-[#263448] shadow-sm hover:scale-105 transition"
                      title="Rotate 90° Clockwise"
                    >
                      <RotateCw className="w-3 h-3" />
                    </button>
                  </div>

                  <div className="p-2 bg-slate-50 dark:bg-[#111A29] border-t border-slate-200 dark:border-[#263448] mt-auto">
                    <p className="text-[10px] font-medium text-slate-800 dark:text-[#E8EDF5] truncate">
                      {item.name}
                    </p>
                    <p className="text-[9px] text-slate-500 dark:text-[#64748B] font-mono">
                      {formatFileSize(item.size)}
                    </p>

                    <div className="flex items-center justify-between mt-1.5 pt-1 border-t border-slate-200 dark:border-[#263448]">
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          disabled={index === 0}
                          onClick={() => handleMove(index, 'left')}
                          className="p-1 rounded text-slate-500 hover:text-slate-800 dark:hover:text-white disabled:opacity-30"
                          title="Move Left"
                        >
                          <ArrowLeft className="w-3 h-3" />
                        </button>
                        <button
                          type="button"
                          disabled={index === items.length - 1}
                          onClick={() => handleMove(index, 'right')}
                          className="p-1 rounded text-slate-500 hover:text-slate-800 dark:hover:text-white disabled:opacity-30"
                          title="Move Right"
                        >
                          <ArrowRight className="w-3 h-3" />
                        </button>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemove(item.id)}
                        className="p-1 rounded text-red-500 hover:text-red-700"
                        title="Remove"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Settings & Customization Panel */}
            <div className="bg-white dark:bg-[#141D2B] rounded-[2px] p-5 shadow-sm border border-slate-200 dark:border-[#263448] mb-8">
              <div className="flex items-center gap-2 mb-4 pb-2 border-b border-slate-200 dark:border-[#263448]">
                <Settings2 className="w-4 h-4 text-blue-500" />
                <h4 className="font-semibold text-xs uppercase tracking-wider text-slate-900 dark:text-[#E8EDF5]">
                  PDF Page Settings
                </h4>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                    Page Size
                  </label>
                  <select
                    value={options.pageSize}
                    onChange={(e) =>
                      setOptions({ ...options, pageSize: e.target.value as any })
                    }
                    className="w-full h-9 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-800 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
                  >
                    <option value="A4">A4 (210 × 297 mm)</option>
                    <option value="Letter">US Letter (8.5 × 11 in)</option>
                    <option value="Fit">Fit to Image (Zero Borders)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                    Orientation
                  </label>
                  <select
                    value={options.orientation}
                    onChange={(e) =>
                      setOptions({ ...options, orientation: e.target.value as any })
                    }
                    className="w-full h-9 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-800 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
                  >
                    <option value="auto">Auto (Match Image Ratio)</option>
                    <option value="portrait">Portrait</option>
                    <option value="landscape">Landscape</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                    Margins
                  </label>
                  <select
                    value={options.margin}
                    onChange={(e) =>
                      setOptions({ ...options, margin: e.target.value as any })
                    }
                    className="w-full h-9 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-800 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
                  >
                    <option value="none">No Margin (0 mm)</option>
                    <option value="small">Small Margin (10 mm)</option>
                    <option value="normal">Normal Margin (20 mm)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                    Output Filename
                  </label>
                  <input
                    type="text"
                    value={options.filename}
                    onChange={(e) => setOptions({ ...options, filename: e.target.value })}
                    className="w-full h-9 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-800 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
                  />
                </div>
              </div>

              <div className="mt-5 pt-4 border-t border-slate-200 dark:border-[#263448] flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="text-xs text-slate-500 dark:text-[#94A3B8]">
                  {pdfBlob ? (
                    <span className="text-emerald-600 dark:text-emerald-400 font-semibold flex items-center gap-1.5">
                      <CheckCircle2 className="w-4 h-4" />
                      PDF Created successfully! ({formatFileSize(pdfBlob.size)})
                    </span>
                  ) : (
                    <span>Ready to convert {items.length} image(s) to PDF</span>
                  )}
                </div>

                <div className="flex items-center gap-2 w-full sm:w-auto">
                  {pdfUrl && (
                    <a
                      href={pdfUrl}
                      download={options.filename}
                      className="h-10 px-4 rounded-[2px] font-medium text-xs text-slate-700 dark:text-[#E8EDF5] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-1.5 transition"
                    >
                      <Download className="w-4 h-4" />
                      <span>Download Again</span>
                    </a>
                  )}

                  <button
                    type="button"
                    disabled={isConverting}
                    onClick={handleConvert}
                    className="h-10 px-6 rounded-[2px] font-semibold text-xs sm:text-sm text-white bg-[#3B82F6] hover:bg-[#2563EB] disabled:bg-slate-300 dark:disabled:bg-[#1E293B] transition flex items-center justify-center gap-2 whitespace-nowrap"
                  >
                    {isConverting ? (
                      <>
                        <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                        <span>
                          Converting...{' '}
                          {progress ? `(${progress.current}/${progress.total})` : ''}
                        </span>
                      </>
                    ) : (
                      <>
                        <FileCheck className="w-4 h-4" />
                        <span>Convert & Download PDF</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </section>
        )}

        {/* FAQs */}
        <section className="py-12 border-t border-slate-200 dark:border-[#263448]">
          <div className="max-w-4xl mx-auto px-4 sm:px-6">
            <div className="text-center mb-8">
              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
                Frequently Asked Questions
              </h2>
            </div>

            <div className="space-y-2">
              {faqs.map((faq, index) => {
                const isOpen = faqOpen === index;
                return (
                  <div
                    key={index}
                    className="rounded-[2px] border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#111A29] overflow-hidden"
                  >
                    <button
                      onClick={() => setFaqOpen(isOpen ? null : index)}
                      className="w-full py-3.5 px-4 text-left flex items-center justify-between gap-4 font-medium text-xs sm:text-sm text-slate-900 dark:text-[#E8EDF5]"
                    >
                      <span>{faq.q}</span>
                      {isOpen ? (
                        <ChevronUp className="w-4 h-4 shrink-0 text-blue-500" />
                      ) : (
                        <ChevronDown className="w-4 h-4 shrink-0 text-slate-400" />
                      )}
                    </button>
                    {isOpen && (
                      <div className="px-4 pb-3.5 text-xs text-slate-600 dark:text-[#94A3B8] leading-relaxed border-t border-slate-100 dark:border-[#263448] pt-2.5">
                        {faq.a}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
