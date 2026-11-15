import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Video to Audio MP3 Extractor — Extract Audio from Reels & Videos',
  description:
    'Extract high-quality MP3 audio from Instagram Reels, Facebook videos, YouTube, and local video files in up to 320kbps studio quality. Free, fast, and no watermark.',
  keywords: [
    'video to mp3',
    'extract audio from video',
    'instagram to mp3',
    'reel to mp3 converter',
    'facebook video to audio',
    'convert video to mp3 online free',
    'audio extractor',
    'extract sound from reel',
    '320kbps mp3 extractor',
    'qwik save audio extractor',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/audio-extractor`,
  },
  openGraph: {
    title: 'Video to Audio MP3 Extractor | Qwik Save',
    description:
      'Extract crystal-clear MP3 tracks from any social media video URL or local file in seconds. 100% free.',
    url: `${APP_CONFIG.appUrl}/audio-extractor`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Video to Audio MP3 Extractor',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Video to MP3 Extractor — Free Online Audio Converter',
    description: 'Extract 320kbps MP3 audio tracks from reels, stories, and videos in one click.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function AudioExtractorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Video to MP3 Audio Extractor',
    operatingSystem: 'All',
    applicationCategory: 'MultimediaApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Extract high-fidelity MP3 audio tracks from Instagram Reels, Facebook videos, and video files.',
    url: `${APP_CONFIG.appUrl}/audio-extractor`,
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
