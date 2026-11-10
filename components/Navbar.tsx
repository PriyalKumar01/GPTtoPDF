'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { FileText, Sun, Moon, ArrowUpRight, Menu, X, Sparkles } from 'lucide-react';
import { useTheme } from '@/lib/theme-context';
import { APP_CONFIG } from '@/lib/config';

export function Navbar() {
  const { theme, toggleTheme } = useTheme();
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navLinks = [
    { label: 'Converter', href: '/' },
    { label: 'How It Works', href: '/how-it-works' },
    { label: 'Features', href: '/features' },
    { label: 'Supported Platforms', href: '/supported-platforms' },
    { label: 'Privacy', href: '/privacy' },
    { label: 'FAQ', href: '/faq' },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200 dark:border-[#263448] bg-white/95 dark:bg-[#090D14]/95 backdrop-blur-md transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between">
        {/* Brand with Clean Logo without text + Typography */}
        <Link
          href="/"
          className="flex items-center gap-2.5 text-slate-900 dark:text-[#E8EDF5] group"
        >
          <img
            src="/images/app-logo.jpg"
            alt="AI Chat to PDF Logo"
            className="w-8 h-8 rounded-[3px] object-contain shadow-sm border border-slate-200 dark:border-[#263448] bg-white dark:bg-[#111A29] p-0.5"
          />
          <div className="flex flex-col">
            <div className="flex items-center gap-1.5 leading-none">
              <span className="font-bold text-sm tracking-tight text-slate-900 dark:text-[#E8EDF5]">
                AI Chat to PDF
              </span>
              <span className="text-[9px] font-bold uppercase px-1 py-0.2 rounded-sm bg-red-100 dark:bg-red-950/80 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/60 leading-tight">
                PDF
              </span>
            </div>
            <span className="text-[10px] text-slate-500 dark:text-[#94A3B8] font-medium tracking-tight mt-0.5 leading-none">
              Convert Chats to Documents
            </span>
          </div>
        </Link>

        {/* Navigation Links for Desktop with Animated Underline */}
        <nav className="hidden lg:flex items-center gap-6 text-xs">
          {navLinks.map((link) => {
            const isActive = pathname === link.href;
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`relative py-1.5 font-medium transition-colors duration-200 group ${
                  isActive
                    ? 'text-blue-600 dark:text-[#3B82F6]'
                    : 'text-slate-600 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-[#3B82F6]'
                }`}
              >
                <span>{link.label}</span>
                {/* Smooth Animated Blue Underline */}
                <span
                  className={`absolute bottom-0 left-0 h-[2px] bg-blue-600 dark:bg-[#3B82F6] transition-all duration-300 ease-out ${
                    isActive ? 'w-full' : 'w-0 group-hover:w-full'
                  }`}
                />
              </Link>
            );
          })}
        </nav>

        {/* Right Actions */}
        <div className="flex items-center gap-2.5">
          {/* Developer: Priyal Kumar link */}
          <a
            href={APP_CONFIG.author.linkedinUrl}
            target="_blank"
            rel="noopener noreferrer"
            title="Connect with Developer Priyal Kumar on LinkedIn"
            className="hidden sm:inline-flex items-center gap-1 px-2.5 py-1 text-xs text-slate-700 dark:text-[#94A3B8] hover:text-blue-600 dark:hover:text-[#3B82F6] bg-slate-100 dark:bg-[#0F1623] border border-slate-200 dark:border-[#263448] rounded-[2px] transition-colors"
          >
            <span className="font-medium">Developer: Priyal Kumar</span>
            <ArrowUpRight className="w-3 h-3 opacity-70" strokeWidth={2} />
          </a>

          {/* Theme Toggle Button */}
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

          {/* Mobile Menu Button */}
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
        <div className="lg:hidden border-b border-slate-200 dark:border-[#263448] bg-white dark:bg-[#0F1623] px-4 py-3 space-y-1 text-xs">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setMobileMenuOpen(false)}
              className={`block px-3 py-2 rounded-[2px] transition-colors ${
                pathname === link.href
                  ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] font-semibold'
                  : 'text-slate-700 dark:text-[#94A3B8] hover:bg-slate-50 dark:hover:bg-[#141D2B]'
              }`}
            >
              {link.label}
            </Link>
          ))}
          <div className="pt-2 border-t border-slate-200 dark:border-[#263448] mt-2">
            <a
              href={APP_CONFIG.author.linkedinUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center justify-between px-3 py-2 text-xs text-blue-600 dark:text-[#3B82F6]"
            >
              <span>Made by Priyal Kumar</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>
      )}
    </header>
  );
}
