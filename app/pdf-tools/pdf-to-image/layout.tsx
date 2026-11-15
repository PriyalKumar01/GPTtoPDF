import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'PDF to Image Converter — Convert PDF to JPG & PNG Online Free',
  description:
    'Free online PDF to Image Converter (pdf to image): Convert PDF pages into high-resolution JPG or PNG images. High-definition 300 DPI export, client-side private, instant download.',
  keywords: [
    'pdf to image',
    'pdf to jpg',
    'pdf to png',
    'convert pdf to image',
    'pdf to pictures',
    'extract images from pdf',
    'pdf to jpg converter online free',
    'high quality pdf to image',
    'client-side pdf to image',
    'qwik save pdf to image',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/pdf-tools/pdf-to-image`,
  },
  openGraph: {
    title: 'PDF to Image Converter (JPG & PNG) | Qwik Save',
    description:
      'Turn PDF documents into high-resolution JPG or PNG pictures directly in your browser. 100% free & private.',
    url: `${APP_CONFIG.appUrl}/pdf-tools/pdf-to-image`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — PDF to Image Converter',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'PDF to Image Converter — High-Res JPG & PNG',
    description: 'Convert PDF pages into crystal-clear JPG/PNG images in seconds.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function PdfToImageLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save PDF to Image Converter',
    operatingSystem: 'All',
    applicationCategory: 'UtilityApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Convert PDF pages into high-resolution JPG and PNG images directly inside your browser.',
    url: `${APP_CONFIG.appUrl}/pdf-tools/pdf-to-image`,
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
