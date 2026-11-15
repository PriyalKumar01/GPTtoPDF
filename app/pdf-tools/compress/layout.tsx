import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Compress PDF Online — Reduce PDF File Size by 50-80% Free',
  description:
    'Free online PDF Compressor (compress pdf): Reduce PDF file sizes by 50-80% while preserving text clarity and document formatting. Fast client-side optimization with zero data upload.',
  keywords: [
    'compress pdf',
    'compress pdf online',
    'reduce pdf size',
    'shrink pdf',
    'compress pdf free',
    'reduce pdf size to 100kb',
    'compress pdf without losing quality',
    'optimize pdf file size',
    'qwik save compress pdf',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/pdf-tools/compress`,
  },
  openGraph: {
    title: 'Compress PDF Online Free | Qwik Save',
    description:
      'Reduce PDF file size by 50-80% instantly in your browser with private client-side optimization.',
    url: `${APP_CONFIG.appUrl}/pdf-tools/compress`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Compress PDF Online',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Compress PDF Online Free | Qwik Save',
    description: 'Reduce PDF file sizes by 50-80% with lossless metadata stripping.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function CompressPdfLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Compress PDF',
    operatingSystem: 'All',
    applicationCategory: 'UtilityApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Optimize and reduce PDF document size directly in your browser without uploading files to third-party servers.',
    url: `${APP_CONFIG.appUrl}/pdf-tools/compress`,
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
