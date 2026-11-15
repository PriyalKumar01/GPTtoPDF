import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Insta Save — Download Instagram Reels, Videos, Photos & Stories HD',
  description:
    'Free online Instagram Downloader (Insta Save): Download Instagram Reels, videos, carousel photos, and stories in original 1080p Full HD without watermark. Fast, safe, and 100% free.',
  keywords: [
    'insta save',
    'instagram reel download',
    'download instagram reels',
    'save instagram video',
    'instagram video download online',
    'instagram story downloader',
    'save insta',
    'ig reel downloader',
    'instagram photo download',
    'instagram carousel downloader',
    'download insta reel without watermark',
    'qwik save instagram',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/instagram`,
  },
  openGraph: {
    title: 'Insta Save — Download Instagram Reels & Videos HD | Qwik Save',
    description:
      'Save public Instagram Reels, videos, photos, and carousels in original 1080p HD quality. Free, instant, and private.',
    url: `${APP_CONFIG.appUrl}/instagram`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Instagram Reel & Video Downloader',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Insta Save — Download Instagram Reels & Videos HD',
    description: 'Save Instagram Reels, videos, and photos in original 1080p HD quality. Zero watermarks.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function InstagramLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Instagram Downloader (Insta Save)',
    operatingSystem: 'All',
    applicationCategory: 'MultimediaApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Free web tool to download Instagram Reels, videos, and photos in full HD 1080p quality without registration or watermarks.',
    url: `${APP_CONFIG.appUrl}/instagram`,
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
