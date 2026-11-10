import type { Metadata, Viewport } from 'next';
import './globals.css';
import { ThemeProvider } from '@/lib/theme-context';
import { APP_CONFIG } from '@/lib/config';

export const metadata: Metadata = {
  metadataBase: new URL(APP_CONFIG.appUrl),
  title: {
    default: 'AI Chat to PDF — Convert AI Conversations to PDF Free',
    template: '%s | AI Chat to PDF',
  },
  description:
    'Free academic converter: Turn public ChatGPT, Claude, Gemini, and Perplexity shared chats into clean, printable PDFs with diagrams. No login or signup required.',
  keywords: [
    'AI Chat to PDF',
    'ChatGPT to PDF',
    'Claude to PDF',
    'Gemini to PDF',
    'GPT to PDF',
    'convert chatgpt to pdf',
    'convert ai chat to pdf',
    'chatgpt study notes',
    'exam notes from chatgpt',
    'ai chat to pdf converter',
    'free chatgpt pdf export',
    'no signup pdf converter',
  ],
  authors: [{ name: APP_CONFIG.author.fullName, url: APP_CONFIG.author.linkedinUrl }],
  creator: APP_CONFIG.author.fullName,
  publisher: APP_CONFIG.name,
  alternates: {
    canonical: 'https://www.ai-chat2pdf.live',
  },
  icons: {
    icon: [
      { url: '/images/app-logo.jpg', type: 'image/jpeg' },
      { url: '/favicon.ico' },
    ],
    shortcut: '/images/app-logo.jpg',
    apple: '/images/app-logo.jpg',
  },
  openGraph: {
    title: 'AI Chat to PDF — Convert AI Conversations to PDF Free',
    description:
      'Turn public ChatGPT, Claude, Gemini, and Perplexity shared chats into clean, printable academic PDFs with diagrams. Zero login required.',
    url: 'https://www.ai-chat2pdf.live',
    siteName: 'AI Chat to PDF',
    images: [
      {
        url: '/images/brand-banner.jpg',
        width: 1200,
        height: 630,
        alt: 'AI Chat to PDF — Free Academic Document Converter',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'AI Chat to PDF — Convert AI Conversations to PDF Free',
    description:
      'Turn ChatGPT, Claude, and Gemini chats into clean printable PDFs with diagrams. No signup required.',
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
    name: 'AI Chat to PDF',
    url: 'https://www.ai-chat2pdf.live',
    applicationCategory: 'UtilityApplication',
    operatingSystem: 'All',
    description:
      'Free web application to convert ChatGPT, Claude, Gemini, and Perplexity shared conversations into printable academic PDFs with embedded diagrams and classic borders.',
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'USD',
    },
    author: {
      '@type': 'Person',
      name: 'Priyal Kumar Singh',
      url: 'https://www.linkedin.com/in/priyal-kumar-singh/',
    },
  };

  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <head>
        <link rel="icon" href="/images/app-logo.jpg" type="image/jpeg" />
        <link rel="apple-touch-icon" href="/images/app-logo.jpg" />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className="min-h-screen bg-[#EEF2F6] dark:bg-[#090E17] text-slate-900 dark:text-[#F1F5F9] flex flex-col selection:bg-blue-600 selection:text-white transition-colors duration-200">
        <ThemeProvider>
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
