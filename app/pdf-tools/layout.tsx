import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'PDF Tools Suite — Merge, Compress, Convert PDF to Image & JPG to PDF',
  description:
    'Complete online PDF utilities suite: Merge multiple PDFs, compress large PDF files by 50-80%, convert PDF to JPG/PNG, and turn images into PDF. 100% private client-side processing.',
  keywords: [
    'pdf tools',
    'merge pdf',
    'compress pdf',
    'pdf to image',
    'img to pdf',
    'free pdf editor online',
    'combine pdf files',
    'reduce pdf size',
    'qwik save pdf tools',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/pdf-tools`,
  },
  openGraph: {
    title: 'Free PDF Tools Suite Online | Qwik Save',
    description:
      'Merge, compress, and convert PDF files with zero server uploads and total privacy.',
    url: `${APP_CONFIG.appUrl}/pdf-tools`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Free PDF Tools Suite',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Free PDF Tools Suite — Merge, Compress & Convert',
    description: 'All-in-one browser PDF utility suite. Free, client-side, and no file size limits.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function PdfToolsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
