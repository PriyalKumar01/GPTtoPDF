'use client';

import React, { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  FileText,
  Sun,
  Moon,
  ArrowUpRight,
  Menu,
  X,
  ChevronDown,
  Layers,
  Video,
  Images,
  Minimize2,
  Music,
  QrCode,
  Sparkles,
  Compass,
  Info,
  HelpCircle,
  User,
  LogOut,
} from 'lucide-react';
import { InstagramIcon, FacebookIcon, YouTubeIcon } from '@/components/BrandIcons';
import { useTheme } from '@/lib/theme-context';
import { APP_CONFIG } from '@/lib/config';
import { useAuth } from '@/lib/use-auth';
import { AuthModal } from '@/components/AuthModal';

export function Navbar() {
  const { theme, toggleTheme } = useTheme();
  const {
    user,
    isAuthenticated,
    isAuthModalOpen,
    openAuthModal,
    closeAuthModal,
    signOut,
    handleAuthSuccess,
  } = useAuth();
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const [socialDropdownOpen, setSocialDropdownOpen] = useState(false);
  const [pdfDropdownOpen, setPdfDropdownOpen] = useState(false);

  const socialTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const pdfTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleSocialEnter = () => {
    if (socialTimeoutRef.current) clearTimeout(socialTimeoutRef.current);
    setSocialDropdownOpen(true);
  };
  const handleSocialLeave = () => {
    socialTimeoutRef.current = setTimeout(() => setSocialDropdownOpen(false), 150);
  };

  const handlePdfEnter = () => {
    if (pdfTimeoutRef.current) clearTimeout(pdfTimeoutRef.current);
    setPdfDropdownOpen(true);
  };
  const handlePdfLeave = () => {
    pdfTimeoutRef.current = setTimeout(() => setPdfDropdownOpen(false), 150);
  };

  // Close menus on route change
  useEffect(() => {
    setMobileMenuOpen(false);
    setSocialDropdownOpen(false);
    setPdfDropdownOpen(false);
  }, [pathname]);

  const socialLinks = [
    {
      title: 'Instagram Downloader',
      desc: 'Reels, videos, photos & carousels',
      href: '/instagram',
      icon: InstagramIcon,
      badge: 'Hot',
      badgeColor: 'bg-pink-500/10 text-pink-600 dark:text-pink-400 border-pink-500/20',
    },
    {
      title: 'Facebook Downloader',
      desc: 'Public videos & reels in Full HD 1080p',
      href: '/facebook',
      icon: FacebookIcon,
      badge: 'Fast',
      badgeColor: 'bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20',
    },
    {
      title: 'YouTube & Universal',
      desc: 'Shorts, TikTok (no watermark), X & Pins',
      href: '/youtube',
      icon: YouTubeIcon,
      badge: 'Universal',
      badgeColor: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20',
    },
    {
      title: 'Audio Extractor',
      desc: 'Extract MP3 audio track from any video',
      href: '/audio-extractor',
      icon: Music,
      badge: 'MP3',
      badgeColor: 'bg-indigo-500/10 text-indigo-500 border-indigo-500/20',
    },
  ];

  const pdfLinks = [
    {
      title: 'Image to PDF Converter',
      desc: 'Convert JPG, PNG to customizable PDF',
      href: '/image-to-pdf',
      icon: Images,
      badge: 'Free',
    },
    {
      title: 'Merge PDF Documents',
      desc: 'Combine multiple PDFs in custom order',
      href: '/pdf-tools/merge',
      icon: Layers,
      badge: 'Client-Side',
    },
    {
      title: 'Compress PDF File',
      desc: 'Reduce file size with smart compression',
      href: '/pdf-tools/compress',
      icon: Minimize2,
      badge: '50-80%',
    },
    {
      title: 'PDF to Image Converter',
      desc: 'Extract pages as high-res JPG/PNG',
      href: '/pdf-tools/pdf-to-image',
      icon: FileText,
      badge: 'High-Res',
    },
    {
      title: 'QR Code Studio',
      desc: 'Create custom QR codes for URLs & WiFi',
      href: '/qr-code',
      icon: QrCode,
      badge: 'Instant',
    },
  ];

  const isSocialActive = ['/instagram', '/facebook', '/youtube', '/audio-extractor'].includes(pathname);
  const isPdfActive = [
    '/image-to-pdf',
    '/pdf-tools',
    '/pdf-tools/merge',
    '/pdf-tools/compress',
    '/pdf-tools/pdf-to-image',
    '/qr-code',
  ].includes(pathname);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200 dark:border-[#263448] bg-white/95 dark:bg-[#090D14]/95 backdrop-blur-md transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between">
        {/* Brand Logo - Wide Horizontal Brand Logo (Matching Footer, Save in One Click) */}
        <Link href="/" className="flex items-center group shrink-0">
          <img
            src="/images/qwik-save-banner.png"
            alt="Qwik Save - Save in One Click"
            className="h-8 sm:h-9 w-auto object-contain rounded-[2px]"
          />
        </Link>

        {/* Desktop Navigation - Left Aligned directly after Logo */}
        <nav className="hidden lg:flex items-center gap-7 text-xs ml-8 flex-1">
          {/* Item 1: Explore All Features (Dashboard) - First in Navbar */}
          <Link
            href="/"
            className={`relative py-1 font-medium transition-colors after:content-[''] after:absolute after:bottom-[-2px] after:left-0 after:h-[2px] after:bg-[#1877F2] after:transition-all after:duration-200 ${
              pathname === '/' || pathname === '/tools'
                ? 'text-blue-600 dark:text-[#3B82F6] font-semibold after:w-full'
                : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] after:w-0 hover:after:w-full'
            }`}
          >
            <span>Explore All Features</span>
          </Link>

          {/* Item 2: Social Downloaders (Dropdown) */}
          <div
            className="relative"
            onMouseEnter={handleSocialEnter}
            onMouseLeave={handleSocialLeave}
          >
            <button
              onClick={() => setSocialDropdownOpen(!socialDropdownOpen)}
              className={`relative py-1 font-medium transition-colors flex items-center gap-1 after:content-[''] after:absolute after:bottom-[-2px] after:left-0 after:h-[2px] after:bg-[#1877F2] after:transition-all after:duration-200 ${
                isSocialActive
                  ? 'text-blue-600 dark:text-[#3B82F6] font-semibold after:w-full'
                  : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] after:w-0 hover:after:w-full'
              }`}
            >
              <span>Social Downloaders</span>
              <ChevronDown className="w-3 h-3 opacity-60" />
            </button>

            {socialDropdownOpen && (
              <div className="absolute top-full left-0 w-72 pt-2 z-50 animate-in fade-in duration-150">
                <div className="bg-white dark:bg-[#0F172A] border border-slate-200 dark:border-[#263448] rounded-[3px] shadow-xl p-1.5 space-y-0.5">
                  <div className="px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800/80 mb-1">
                    Social Video & Audio
                  </div>
                  {socialLinks.map((item) => {
                    const Icon = item.icon;
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className="px-2.5 py-2 rounded-[2px] flex items-start gap-2.5 transition-colors group text-slate-700 dark:text-slate-200 hover:bg-[#1E293B] hover:text-white dark:hover:bg-[#1E293B] dark:hover:text-white"
                      >
                        <div className="w-7 h-7 rounded-[2px] bg-slate-100 dark:bg-[#1E293B] group-hover:bg-white/10 group-hover:text-white flex items-center justify-center text-slate-600 dark:text-slate-300 mt-0.5 shrink-0 transition-colors">
                          <Icon className="w-4 h-4" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between gap-1">
                            <span className="font-semibold text-xs text-slate-800 dark:text-slate-100 group-hover:text-white transition-colors truncate">
                              {item.title}
                            </span>
                            {item.badge && (
                              <span
                                className={`text-[9px] font-bold uppercase px-1.5 py-0.2 rounded-[2px] border shrink-0 ${
                                  item.badgeColor || 'bg-blue-950/50 text-blue-400 border-blue-900/40'
                                }`}
                              >
                                {item.badge}
                              </span>
                            )}
                          </div>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400 group-hover:text-slate-300 transition-colors mt-0.5 truncate">
                            {item.desc}
                          </p>
                        </div>
                      </Link>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* Item 3: PDF & Document Suite (Dropdown) */}
          <div
            className="relative"
            onMouseEnter={handlePdfEnter}
            onMouseLeave={handlePdfLeave}
          >
            <button
              onClick={() => setPdfDropdownOpen(!pdfDropdownOpen)}
              className={`relative py-1 font-medium transition-colors flex items-center gap-1 after:content-[''] after:absolute after:bottom-[-2px] after:left-0 after:h-[2px] after:bg-[#1877F2] after:transition-all after:duration-200 ${
                isPdfActive
                  ? 'text-blue-600 dark:text-[#3B82F6] font-semibold after:w-full'
                  : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] after:w-0 hover:after:w-full'
              }`}
            >
              <span>PDF & Tools Suite</span>
              <ChevronDown className="w-3 h-3 opacity-60" />
            </button>

            {pdfDropdownOpen && (
              <div className="absolute top-full left-0 w-80 pt-2 z-50 animate-in fade-in duration-150">
                <div className="bg-white dark:bg-[#0F172A] border border-slate-200 dark:border-[#263448] rounded-[3px] shadow-xl p-1.5 space-y-0.5">
                  <div className="px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800/80 mb-1">
                    PDF & Document Conversion
                  </div>
                  {pdfLinks.map((item) => {
                    const Icon = item.icon;
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className="px-2.5 py-2 rounded-[2px] flex items-start gap-2.5 transition-colors group text-slate-700 dark:text-slate-200 hover:bg-[#1E293B] hover:text-white dark:hover:bg-[#1E293B] dark:hover:text-white"
                      >
                        <div className="w-7 h-7 rounded-[2px] bg-slate-100 dark:bg-[#1E293B] group-hover:bg-white/10 group-hover:text-white flex items-center justify-center text-slate-600 dark:text-slate-300 mt-0.5 shrink-0 transition-colors">
                          <Icon className="w-4 h-4" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between gap-1">
                            <span className="font-semibold text-xs text-slate-800 dark:text-slate-100 group-hover:text-white transition-colors truncate">
                              {item.title}
                            </span>
                            {item.badge && (
                              <span className="text-[9px] font-bold uppercase px-1.5 py-0.2 rounded-[2px] bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-700 shrink-0">
                                {item.badge}
                              </span>
                            )}
                          </div>
                          <p className="text-[10px] text-slate-500 dark:text-slate-400 group-hover:text-slate-300 transition-colors mt-0.5 truncate">
                            {item.desc}
                          </p>
                        </div>
                      </Link>
                    );
                  })}
                  <div className="border-t border-slate-100 dark:border-slate-800 mt-1 pt-1.5 px-2">
                    <Link
                      href="/pdf-tools"
                      className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline flex items-center justify-between"
                    >
                      <span>Explore PDF Hub Overview</span>
                      <span>→</span>
                    </Link>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Item 4: AI to PDF */}
          <Link
            href="/ai-to-pdf"
            className={`relative py-1 font-medium transition-colors after:content-[''] after:absolute after:bottom-[-2px] after:left-0 after:h-[2px] after:bg-[#1877F2] after:transition-all after:duration-200 ${
              pathname === '/ai-to-pdf'
                ? 'text-blue-600 dark:text-[#3B82F6] font-semibold after:w-full'
                : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] after:w-0 hover:after:w-full'
            }`}
          >
            AI to PDF
          </Link>

          {/* Item 5: About (Requested New Page) */}
          <Link
            href="/about"
            className={`relative py-1 font-medium transition-colors after:content-[''] after:absolute after:bottom-[-2px] after:left-0 after:h-[2px] after:bg-[#1877F2] after:transition-all after:duration-200 ${
              pathname === '/about'
                ? 'text-blue-600 dark:text-[#3B82F6] font-semibold after:w-full'
                : 'text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] after:w-0 hover:after:w-full'
            }`}
          >
            About
          </Link>
        </nav>

        {/* Right Actions */}
        <div className="flex items-center gap-2.5">
          <Link
            href="/faq"
            className="hidden sm:inline-flex items-center gap-1.5 px-2.5 py-1 text-xs text-slate-700 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-[#3B82F6] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] rounded-[2px] transition-colors"
            title="Frequently Asked Questions & Help"
          >
            <HelpCircle className="w-3.5 h-3.5 text-blue-500" />
            <span className="font-medium">FAQs</span>
          </Link>

          <button
            onClick={toggleTheme}
            aria-label="Toggle theme"
            className="w-8 h-8 flex items-center justify-center rounded-[2px] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] transition-colors"
          >
            {theme === 'dark' ? (
              <Sun className="w-4 h-4 text-[#E8EDF5]" strokeWidth={1.75} />
            ) : (
              <Moon className="w-4 h-4 text-slate-800" strokeWidth={1.75} />
            )}
          </button>

          {/* User Account / Sign In */}
          {isAuthenticated && user ? (
            <div className="flex items-center gap-1.5 pl-1">
              <div
                className="flex items-center gap-1.5 px-2 py-1 rounded-[2px] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] text-xs"
                title={`Signed in as ${user.email}`}
              >
                <div className="w-5 h-5 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center text-[10px] uppercase">
                  {user.name.charAt(0)}
                </div>
                <span className="font-semibold text-slate-800 dark:text-[#E8EDF5] max-w-[100px] truncate hidden md:inline">
                  {user.name}
                </span>
              </div>
              <button
                onClick={signOut}
                title="Sign Out"
                className="w-8 h-8 flex items-center justify-center text-slate-400 hover:text-rose-500 rounded-[2px] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] transition-colors"
                aria-label="Sign Out"
              >
                <LogOut className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => openAuthModal()}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-500 rounded-[2px] shadow-sm transition-all"
            >
              <User className="w-3.5 h-3.5" />
              <span>Sign In</span>
            </button>
          )}

          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="lg:hidden w-8 h-8 flex items-center justify-center rounded-[2px] bg-slate-100 dark:bg-[#141D2B] border border-slate-200 dark:border-[#263448] text-slate-700 dark:text-[#94A3B8]"
            aria-label="Open navigation menu"
          >
            {mobileMenuOpen ? <X className="w-4 h-4" /> : <Menu className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-b border-slate-200 dark:border-[#263448] bg-white dark:bg-[#0F1623] px-4 py-3 space-y-3 text-xs max-h-[85vh] overflow-y-auto">
          {/* Mobile Auth Banner */}
          <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
            {isAuthenticated && user ? (
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center text-xs uppercase">
                    {user.name.charAt(0)}
                  </div>
                  <div>
                    <p className="font-bold text-slate-900 dark:text-white leading-tight">
                      {user.name}
                    </p>
                    <p className="text-[10px] text-slate-500 dark:text-slate-400 truncate max-w-[170px]">
                      {user.email}
                    </p>
                  </div>
                </div>
                <button
                  onClick={signOut}
                  className="px-2 py-1 text-[11px] text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded border border-rose-200 dark:border-rose-900/40"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  openAuthModal();
                }}
                className="w-full py-2 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded text-xs flex items-center justify-center gap-2"
              >
                <User className="w-3.5 h-3.5" />
                <span>Sign In / Register</span>
              </button>
            )}
          </div>

          <div>
            <p className="px-2 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Overview
            </p>
            <Link
              href="/"
              className="block px-2.5 py-1.5 rounded-[2px] font-semibold text-blue-600 dark:text-[#3B82F6] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
            >
              Explore All Features
            </Link>
            <Link
              href="/ai-to-pdf"
              className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
            >
              AI Chat to PDF Converter
            </Link>
          </div>

          <div>
            <p className="px-2 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Social Downloaders
            </p>
            <div className="space-y-1">
              <Link
                href="/instagram"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                Instagram Reels & Posts
              </Link>
              <Link
                href="/facebook"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                Facebook Videos & Reels
              </Link>
              <Link
                href="/youtube"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                YouTube Shorts & Universal
              </Link>
              <Link
                href="/audio-extractor"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                Audio Extractor (MP3)
              </Link>
            </div>
          </div>

          <div>
            <p className="px-2 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
              PDF & Document Tools
            </p>
            <div className="space-y-1">
              <Link
                href="/image-to-pdf"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                Image to PDF
              </Link>
              <Link
                href="/pdf-tools/merge"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                Merge PDF Documents
              </Link>
              <Link
                href="/pdf-tools/compress"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                Compress PDF File
              </Link>
              <Link
                href="/pdf-tools/pdf-to-image"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                PDF to Image
              </Link>
              <Link
                href="/qr-code"
                className="block px-2.5 py-1.5 rounded-[2px] text-slate-800 dark:text-[#E8EDF5] hover:bg-slate-100 dark:hover:bg-[#141D2B]"
              >
                QR Code Studio
              </Link>
            </div>
          </div>

          <div className="pt-2 border-t border-slate-200 dark:border-[#263448] flex items-center justify-between">
            <Link
              href="/about"
              className="block px-2.5 py-1.5 font-semibold text-slate-900 dark:text-white"
            >
              About & Developer →
            </Link>
            <Link
              href="/faq"
              className="block px-2.5 py-1.5 text-slate-500 dark:text-[#94A3B8]"
            >
              FAQs
            </Link>
          </div>
        </div>
      )}

      {/* Global Auth Modal */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={closeAuthModal}
        onSuccess={handleAuthSuccess}
      />
    </header>
  );
}
