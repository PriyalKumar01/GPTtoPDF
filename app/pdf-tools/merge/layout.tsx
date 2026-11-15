import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Merge PDF Files Online — Combine Multiple PDFs Free',
  description:
    'Free online PDF Merger (merge pdf): Combine 2 or more PDF documents into a single consolidated PDF file. Drag and drop page reordering, client-side privacy, zero server uploads.',
  keywords: [
    'merge pdf',
    'merge pdf online',
    'combine pdf',
    'combine pdf files',
    'join pdf',
    'merge multiple pdfs free',
    'pdf joiner online',
    'combine pdf documents free',
    'merge pdf no limit',
    'qwik save merge pdf',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/pdf-tools/merge`,
  },
  openGraph: {
    title: 'Merge PDF Files Online Free | Qwik Save',
    description:
      'Combine multiple PDF documents into one cleanly formatted file. Fast, client-side private, and free.',
    url: `${APP_CONFIG.appUrl}/pdf-tools/merge`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Merge PDF Online',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Merge PDF Files Online Free | Qwik Save',
    description: 'Combine multiple PDF documents with drag-and-drop page ordering. 100% private.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function MergePdfLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Merge PDF',
    operatingSystem: 'All',
    applicationCategory: 'UtilityApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Combine multiple PDF documents into one file client-side without uploading to external servers.',
    url: `${APP_CONFIG.appUrl}/pdf-tools/merge`,
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
