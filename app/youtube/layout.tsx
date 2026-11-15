import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'YouTube Shorts & Universal Video Downloader — TikTok Without Watermark',
  description:
    'Download YouTube Shorts, TikTok videos without watermark, Twitter/X clips, and Pinterest videos in Full HD. 100% free, fast, and no registration required.',
  keywords: [
    'youtube shorts download',
    'tiktok video download without watermark',
    'download tiktok video no watermark',
    'twitter video downloader',
    'download x video',
    'pinterest video download',
    'universal video downloader',
    'save online video hd',
    'free video downloader online',
    'qwik save youtube',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/youtube`,
  },
  openGraph: {
    title: 'YouTube & Universal Video Downloader | Qwik Save',
    description:
      'Download YouTube Shorts, TikTok without watermark, X/Twitter clips, and Pinterest videos in crisp HD quality.',
    url: `${APP_CONFIG.appUrl}/youtube`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Universal Video Downloader',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'YouTube Shorts & TikTok Downloader Without Watermark',
    description: 'Download HD video clips from YouTube, TikTok, Twitter/X, and Pinterest for free.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function YoutubeLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Universal Video Downloader',
    operatingSystem: 'All',
    applicationCategory: 'MultimediaApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Download YouTube Shorts, TikTok videos without watermark, Twitter/X, and Pinterest videos in Full HD.',
    url: `${APP_CONFIG.appUrl}/youtube`,
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
