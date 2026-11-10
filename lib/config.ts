export const APP_CONFIG = {
  name: 'AI Chat to PDF',
  tagline: 'Convert AI Conversations to Documents & Academic PDFs',
  description:
    'Convert your ChatGPT, Claude, Gemini, and Perplexity study notes into clean, beautifully formatted academic PDFs. No login. No signup. Just paste your link and download.',
  // Central application URL used for links and in the PDF footer
  appUrl: process.env.NEXT_PUBLIC_APP_URL || 'https://www.ai-chat2pdf.live',
  domain: 'ai-chat2pdf.live',
  author: {
    name: 'Priyal Kumar',
    fullName: 'Priyal Kumar Singh',
    linkedinUrl: 'https://www.linkedin.com/in/priyal-kumar-singh/',
  },
  partner: {
    name: 'College Study',
    tagline: 'Your Study Hub',
    badgeText: 'Partnered with College Study',
    url: process.env.NEXT_PUBLIC_PARTNER_URL || 'https://college-study.netlify.app/',
  },
  supportedHosts: ['chatgpt.com', 'chat.openai.com'],
  defaultPdfOptions: {
    paperSize: 'A4' as 'A4' | 'Letter',
    fontSize: 'medium' as 'small' | 'medium' | 'large',
    fontFamily: 'Helvetica' as 'Helvetica' | 'Times' | 'Courier',
    borderStyle: 'shadow' as 'shadow' | 'clean' | 'none',
    partnerBadgeScope: 'all-pages' as 'all-pages' | 'first-page',
    includeTitle: true,
    includeDate: true,
    includePageNumbers: true,
    includeUserMessages: true,
    includeAssistantMessages: true,
    includeCodeBlocks: true,
    includeImages: true,
  },
};
