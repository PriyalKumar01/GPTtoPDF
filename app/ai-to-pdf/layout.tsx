import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'AI Chat to PDF Converter — Turn ChatGPT, Claude, Gemini into Study PDFs | Qwik Save',
  description:
    'Convert ChatGPT, Claude, Gemini, and Perplexity shared links into beautiful, academic-formatted PDF notes in seconds with Qwik Save. 100% Free, zero watermark.',
  keywords: [
    'ai chat to pdf',
    'chatgpt to pdf',
    'claude to pdf',
    'gemini to pdf',
    'perplexity to pdf',
    'ai notes converter',
    'qwik save ai',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/ai-to-pdf`,
  },
  openGraph: {
    title: 'AI Chat to PDF Converter — Qwik Save',
    description: 'Turn ChatGPT, Claude, and Gemini chats into publication-quality academic PDFs in 1 click.',
    url: `${APP_CONFIG.appUrl}/ai-to-pdf`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'AI Chat to PDF Converter',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'AI Chat to PDF Converter — Qwik Save',
    description: 'Turn ChatGPT, Claude, and Gemini chats into publication-quality academic PDFs.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function AiToPdfLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
