import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'Image to PDF Converter — Convert JPG, PNG to PDF Free Online',
  description:
    'Free online Image to PDF Converter (img to pdf): Convert JPG, JPEG, PNG, WEBP, and GIF pictures into a clean PDF document. Client-side private, reorder pages, no upload to server.',
  keywords: [
    'img to pdf',
    'image to pdf',
    'jpg to pdf',
    'convert jpg to pdf',
    'png to pdf',
    'convert image to pdf online',
    'photos to pdf',
    'picture to pdf converter',
    'merge images to pdf',
    'jpeg to pdf free',
    'client-side image to pdf',
    'qwik save image to pdf',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/image-to-pdf`,
  },
  openGraph: {
    title: 'Image to PDF Converter — Fast & Free Online | Qwik Save',
    description:
      'Convert JPG, PNG, and photos to a clean PDF in seconds. 100% in-browser client-side privacy.',
    url: `${APP_CONFIG.appUrl}/image-to-pdf`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Image to PDF Converter',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Image to PDF Converter — JPG & PNG to PDF Free',
    description: 'Combine photos into a high-quality PDF in seconds with client-side privacy.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function ImageToPdfLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Qwik Save Image to PDF Converter',
    operatingSystem: 'All',
    applicationCategory: 'UtilityApplication',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    description:
      'Convert JPG, PNG, and image files to clean PDF documents in your browser without uploading to external servers.',
    url: `${APP_CONFIG.appUrl}/image-to-pdf`,
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
