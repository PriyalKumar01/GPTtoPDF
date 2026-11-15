import type { Metadata, Viewport } from 'next';
import './globals.css';
import { ThemeProvider } from '@/lib/theme-context';
import { APP_CONFIG } from '@/lib/config';
import { InitialSplashScreen } from '@/components/InitialSplashScreen';

export const metadata: Metadata = {
  metadataBase: new URL(APP_CONFIG.appUrl),
  title: {
    default: 'Qwik Save — Save Reels & Posts, Merge, Compress or Convert PDF & AI Chats',
    template: '%s | Qwik Save',
  },
  description:
    'Qwik Save — Save in One Click. Save Instagram Reels & posts, Facebook & YouTube videos in HD, convert Image to PDF, Merge/Compress PDFs, and convert ChatGPT/Claude study chats to academic PDFs. Fast, private, and 100% free.',
  keywords: [
    'Qwik Save',
    'insta save',
    'instagram reel download',
    'facebook reel save',
    'img to pdf',
    'merge pdf',
    'compress pdf',
    'pdf to image',
    'video to mp3',
    'AI Chat to PDF',
    'ChatGPT to PDF',
    'Claude to PDF',
    'Gemini to PDF',
    'GPT to PDF',
    'Instagram Reel Downloader',
    'Facebook Video Downloader',
    'YouTube Shorts Downloader',
    'TikTok Video Downloader',
    'Image to PDF',
    'Video to MP3 Extractor',
    'convert chatgpt to pdf',
    'free online tools',
  ],
  authors: [{ name: APP_CONFIG.author.fullName, url: APP_CONFIG.author.linkedinUrl }],
  creator: APP_CONFIG.author.fullName,
  publisher: APP_CONFIG.name,
  alternates: {
    canonical: 'https://www.ai-chat2pdf.live',
  },
  icons: {
    icon: [
      { url: '/images/app-logo.png', type: 'image/png' },
      { url: '/favicon.ico' },
    ],
    shortcut: '/images/app-logo.png',
    apple: '/images/app-logo.png',
  },
  openGraph: {
    title: 'Qwik Save — Save in One Click',
    description:
      'All-in-one productivity suite: Convert AI conversations to academic PDFs, download Instagram Reels & Facebook videos, merge/compress PDFs, and extract MP3 audio. 100% free.',
    url: 'https://www.ai-chat2pdf.live',
    siteName: 'Qwik Save',
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'Qwik Save — Save in One Click',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Qwik Save — Save in One Click',
    description:
      'Convert AI chats to PDF, download Instagram Reels, Facebook & YouTube videos in HD, and manage PDFs.',
    images: ['/images/brand-banner.jpg'],
    creator: '@PriyalKumar',
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'WebApplication',
    name: 'Qwik Save',
    url: 'https://www.ai-chat2pdf.live',
    applicationCategory: 'UtilityApplication',
    operatingSystem: 'All',
    description:
      'Qwik Save - Save all in just one click: Convert AI conversations (ChatGPT, Claude, Gemini) into printable academic PDFs, download Instagram Reels, Facebook & YouTube videos in HD, merge/compress PDFs, and extract audio. Free and client-side private.',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    author: {
      '@type': 'Person',
      name: 'Priyal Kumar',
      url: 'https://priyalkumar01.github.io/priyalkumar.github.io/',
      sameAs: [
        'https://www.linkedin.com/in/priyal-kumar-singh/',
        'https://college-study.netlify.app/',
      ],
    },
  };

  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <head>
        <link rel="icon" href="/images/app-logo.png" type="image/png" />
        <link rel="apple-touch-icon" href="/images/app-logo.png" />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className="min-h-screen bg-[#EEF2F6] dark:bg-[#090E17] text-slate-900 dark:text-[#F1F5F9] flex flex-col selection:bg-blue-600 selection:text-white transition-colors duration-200">
        <ThemeProvider>
          <InitialSplashScreen />
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
