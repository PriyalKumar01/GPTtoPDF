import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Facebook Video Downloader — Save FB Reels & Watch Clips in 1080p Full HD',
  description:
    'Download Facebook public videos, reels, and watch clips in Full HD 1080p, 720p HD, and SD with exact file size display. Free, fast, and no login required.',
  keywords: [
    'facebook reel save',
    'fb video download',
    'facebook video downloader',
    'save facebook video online',
    'download facebook reels online',
    'facebook reel download 1080p',
    'fb watch downloader',
    'facebook video saver hd',
    'save fb reels',
    'qwik save facebook',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/facebook`,
  },
  openGraph: {
    title: 'Facebook Video & Reel Downloader HD | Qwik Save',
    description:
      'Save public Facebook videos and reels in crisp 1080p Full HD. Fast, no watermark, with exact MB file size display.',
    url: `${APP_CONFIG.appUrl}/facebook`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Facebook Video Downloader',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Facebook Video Downloader — Save FB Reels in 1080p HD',
    description: 'Download Facebook reels and videos in 1080p Full HD with file size display.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function FacebookLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Facebook Video Downloader',
    operatingSystem: 'All',
    applicationCategory: 'MultimediaApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Free web tool to download Facebook public videos and reels in high-definition 1080p without registration or ads.',
    url: `${APP_CONFIG.appUrl}/facebook`,
  };

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      {children}
    </>
  );
}
