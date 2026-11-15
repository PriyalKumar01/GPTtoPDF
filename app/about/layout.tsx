import type { Metadata } from 'next';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  title: 'About Qwik Save & Founder Priyal Kumar — Mission, Standards & Story',
  description:
    'Learn about Qwik Save and founder Priyal Kumar (CSE\'27 @ HBTU Kanpur, founder of College Study with 5,000+ students). Built to replace scattered websites with a clean 1-click suite.',
  keywords: [
    'about qwik save',
    'priyal kumar',
    'priyal kumar hbtu kanpur',
    'priyal kumar developer',
    'college study priyal kumar',
    'online video downloader',
    'student productivity tools',
    'safe online tools',
    'qwik save story',
  ],
  alternates: {
    canonical: `${APP_CONFIG.appUrl}/about`,
  },
  openGraph: {
    title: 'About Qwik Save & Founder Priyal Kumar',
    description:
      'The story behind Qwik Save: Built by Priyal Kumar to give students and creators an ultra-fast, private, and unified tool suite in one click.',
    url: `${APP_CONFIG.appUrl}/about`,
    siteName: APP_CONFIG.name,
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — About Platform & Founder Priyal Kumar',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'About Qwik Save & Founder Priyal Kumar',
    description: 'Discover how Qwik Save provides safe, 1-click productivity tools for students and creators.',
    images: ['/images/brand-banner.jpg'],
  },
};

export default function AboutLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'AboutPage',
    name: 'About Qwik Save & Founder Priyal Kumar',
    url: `${APP_CONFIG.appUrl}/about`,
    description:
      'Information about the Qwik Save platform and its developer Priyal Kumar (founder of College Study).',
    author: {
      '@type': 'Person',
      name: 'Priyal Kumar',
      jobTitle: 'Founder & Lead Developer',
      alumniOf: 'Harcourt Butler Technical University (HBTU Kanpur)',
      url: 'https://priyalkumar01.github.io/priyalkumar.github.io/',
      sameAs: [
        'https://www.linkedin.com/in/priyal-kumar-singh/',
        'https://college-study.netlify.app/',
      ],
    },
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
