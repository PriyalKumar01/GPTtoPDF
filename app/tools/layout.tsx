import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Qwik Save — Save Reels & Posts, Merge, Compress or Convert PDF & AI Chats',
  description:
    'Explore the full Qwik Save productivity suite: Save Instagram Reels, Facebook videos, YouTube Shorts, convert Image to PDF, Merge & Compress PDF, and format AI chat notes in 1 click.',
  keywords: [
    'Qwik Save',
    'save reels',
    'save post',
    'merge pdf',
    'convert pdf',
    'img to pdf',
    'ai chats to pdf',
    'instagram downloader',
    'facebook video download',
    'audio extractor',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/tools`,
  },
  openGraph: {
    title: 'Qwik Save — Save Reels, Posts, Merge or Convert PDF & AI Chats',
    description:
      'All-in-one productivity suite for saving social media reels, converting and merging PDFs, and formatting AI chat study notes in 1 click.',
    url: `${APP_CONFIG.appUrl}/tools`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Explore Dashboard',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Qwik Save — Save Reels, Posts, Merge or Convert PDF & AI Chats',
    description: 'All-in-one suite for video downloading, PDF conversion, and AI notes.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function ToolsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
