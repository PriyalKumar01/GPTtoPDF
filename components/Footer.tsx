import React from 'react';
import Link from 'next/link';
import {
  FileText,
  ArrowUpRight,
  Images,
  QrCode,
  Layers,
  Minimize2,
  Music,
  ShieldCheck,
  CheckCircle2,
  HelpCircle,
  FileCode,
} from 'lucide-react';
import {
  InstagramIcon,
  FacebookIcon,
  YouTubeIcon,
  LinkedInIcon,
} from '@/components/BrandIcons';
import { APP_CONFIG } from '@/lib/config';

export function Footer() {
  return (
    <footer className="border-t border-slate-200 dark:border-[#263448] bg-white dark:bg-[#090D14] text-xs text-slate-600 dark:text-[#94A3B8] py-12 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12">
          {/* Brand & Attribution - Featuring Wide Rectangular Logo */}
          <div className="space-y-4 lg:col-span-4">
            <Link href="/" className="inline-block">
              <img
                src="/images/qwik-save-banner.png"
                alt="Qwik Save - Save in One Click"
                className="w-52 sm:w-60 h-auto object-contain rounded-[2px]"
              />
            </Link>
            <p className="text-[11px] text-slate-600 dark:text-[#94A3B8] leading-relaxed max-w-sm">
              <strong>{APP_CONFIG.brandName}</strong> — {APP_CONFIG.tagline}. High-speed digital suite: Convert AI chats to academic PDFs, download Instagram Reels, save Facebook & YouTube videos in HD, merge/compress PDFs, and extract audio in one click.
            </p>
            <div className="pt-1 flex flex-col gap-2">
              <a
                href={APP_CONFIG.author.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] rounded-[3px] text-xs font-semibold text-slate-800 dark:text-[#E8EDF5] hover:text-[#0F172A] dark:hover:text-white hover:underline transition-colors w-fit"
              >
                <LinkedInIcon className="w-3.5 h-3.5 text-[#0077B5]" />
                <span>Developer: Priyal Kumar</span>
                <ArrowUpRight className="w-3.5 h-3.5 opacity-60" />
              </a>

              <a
                href={APP_CONFIG.partner.url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 px-2.5 py-1.5 bg-slate-50 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] rounded-[3px] text-[11px] text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-white hover:border-slate-400 dark:hover:border-slate-600 transition-colors w-fit"
              >
                <img
                  src="/images/college-study-logo.jpg"
                  alt="College Study"
                  className="w-5 h-5 rounded-sm object-contain"
                />
                <span>Partnered with <strong>College Study</strong> (5,000+ Students)</span>
              </a>
            </div>
          </div>

          {/* Right Columns Grid - Shifted to Right with Balanced Spacing */}
          <div className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-3 gap-8">
            {/* Social Media Downloaders */}
            <div>
              <h4 className="font-bold text-slate-900 dark:text-[#E8EDF5] text-xs uppercase tracking-wider mb-3">
                Social Downloaders
              </h4>
              <ul className="space-y-2.5 text-xs">
                <li>
                  <Link
                    href="/instagram"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <InstagramIcon className="w-3.5 h-3.5 text-pink-500 shrink-0" />
                    <span>Instagram Reel Downloader</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/facebook"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <FacebookIcon className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                    <span>Facebook Video Downloader</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/youtube"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <YouTubeIcon className="w-3.5 h-3.5 text-red-600 shrink-0" />
                    <span>YouTube, Shorts & TikTok</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/audio-extractor"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <Music className="w-3.5 h-3.5 text-indigo-500 shrink-0" />
                    <span>Audio Extractor (Video to MP3)</span>
                  </Link>
                </li>
              </ul>
            </div>

            {/* PDF & Document Tools Suite */}
            <div>
              <h4 className="font-bold text-slate-900 dark:text-[#E8EDF5] text-xs uppercase tracking-wider mb-3">
                PDF & Tools Suite
              </h4>
              <ul className="space-y-2.5 text-xs">
                <li>
                  <Link
                    href="/ai-to-pdf"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <FileText className="w-3.5 h-3.5 text-red-500 shrink-0" />
                    <span>AI Chat to PDF Converter</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/pdf-tools/merge"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <Layers className="w-3.5 h-3.5 text-blue-500 shrink-0" />
                    <span>Merge Multiple PDFs</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/pdf-tools/compress"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <Minimize2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                    <span>Compress PDF File Size</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/pdf-tools/pdf-to-image"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <FileCode className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                    <span>PDF to Image (JPG/PNG)</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/image-to-pdf"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <Images className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <span>JPG & PNG to PDF Converter</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/qr-code"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <QrCode className="w-3.5 h-3.5 text-purple-500 shrink-0" />
                    <span>QR Code Studio</span>
                  </Link>
                </li>
              </ul>
            </div>

            {/* Legal, Support & About */}
            <div>
              <h4 className="font-bold text-slate-900 dark:text-[#E8EDF5] text-xs uppercase tracking-wider mb-3">
                Information & Privacy
              </h4>
              <ul className="space-y-2.5 text-xs">
                <li>
                  <Link
                    href="/about"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5 text-blue-500 shrink-0" />
                    <span>About Qwik Save & Story</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/how-it-works"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <HelpCircle className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    <span>How It Works</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/features"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <FileText className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    <span>Features & Formatting</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/privacy"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                    <span>Privacy Policy & Data Safety</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/terms"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <FileText className="w-3.5 h-3.5 text-sky-500 shrink-0" />
                    <span>Terms of Service</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/data-deletion"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5 text-rose-500 shrink-0" />
                    <span>User Data Deletion</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/owner"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <ShieldCheck className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                    <span>Owner Portal (Admin)</span>
                  </Link>
                </li>
                <li>
                  <Link
                    href="/faq"
                    className="text-slate-700 dark:text-[#94A3B8] hover:text-[#0F172A] dark:hover:text-[#3B82F6] hover:underline transition-colors flex items-center gap-2"
                  >
                    <HelpCircle className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    <span>Frequently Asked Questions</span>
                  </Link>
                </li>
              </ul>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-6 border-t border-slate-200 dark:border-[#1E293B] flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px]">
          <div>
            © {new Date().getFullYear()} {APP_CONFIG.name}. Built with precision by{' '}
            <a
              href={APP_CONFIG.author.linkedinUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-slate-900 dark:text-white font-medium hover:underline"
            >
              {APP_CONFIG.author.name}
            </a>
            . All rights reserved.
          </div>
          <div className="flex items-center gap-4 text-slate-500 dark:text-slate-500">
            <span>Free & Open Web Tools</span>
            <span>•</span>
            <span>Client-Side Security</span>
            <span>•</span>
            <span>No Cookies or Tracking</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
