import { ChatMessage } from './types';

export type SupportedPlatform =
  | 'chatgpt'
  | 'claude'
  | 'gemini'
  | 'perplexity'
  | 'generic';

/**
 * Identifies the platform of the shared AI conversation
 */
export function identifyPlatform(url: string): SupportedPlatform {
  const lower = url.toLowerCase();
  if (lower.includes('chatgpt.com') || lower.includes('chat.openai.com') || lower.includes('openai.com')) {
    return 'chatgpt';
  }
  if (lower.includes('claude.ai') || lower.includes('anthropic.com')) {
    return 'claude';
  }
  if (
    lower.includes('gemini.google.com') ||
    lower.includes('share.gemini.google') ||
    lower.includes('gemini.google') ||
    lower.includes('g.co/gemini') ||
    lower.includes('gemini') ||
    lower.includes('bard.google.com')
  ) {
    return 'gemini';
  }
  if (lower.includes('perplexity.ai') || lower.includes('perplexity')) {
    return 'perplexity';
  }
  return 'generic';
}

/**
 * Validates whether the given URL is a supported AI public share link
 */
export function isValidAiShareUrl(url: string): boolean {
  if (!url || typeof url !== 'string') return false;
  let trimmed = url.trim();

  // Allow sample conversation aliases
  if (trimmed.includes('os-exam-notes') || trimmed.includes('ml-exam-notes')) {
    return true;
  }

  // Auto-prefix https:// if protocol was omitted
  if (!/^https?:\/\//i.test(trimmed)) {
    trimmed = `https://${trimmed}`;
  }

  try {
    const parsed = new URL(trimmed);
    const host = parsed.hostname.toLowerCase();
    const path = parsed.pathname.toLowerCase();

    // ChatGPT
    if (
      host === 'chatgpt.com' ||
      host === 'chat.openai.com' ||
      host.endsWith('.chatgpt.com') ||
      host.endsWith('.openai.com') ||
      host.includes('chatgpt')
    ) {
      return true;
    }

    // Claude
    if (
      host === 'claude.ai' ||
      host.endsWith('.claude.ai') ||
      host.includes('claude') ||
      host.includes('anthropic.com')
    ) {
      return true;
    }

    // Google Gemini (including g.co/gemini, share.gemini.google, gemini.google.com)
    if (
      host === 'gemini.google.com' ||
      host === 'share.gemini.google' ||
      host === 'g.co' ||
      host.endsWith('.g.co') ||
      host.includes('gemini') ||
      host.includes('bard.google.com') ||
      host.endsWith('.google.com') ||
      host.endsWith('.gemini.google') ||
      host.includes('gemini.google')
    ) {
      return true;
    }

    // Perplexity
    if (host.includes('perplexity.ai') || host.includes('perplexity')) {
      return true;
    }

    // Generic public shareable URL or general web article
    if (
      path.includes('/share') ||
      path.includes('/c/') ||
      path.includes('/chat') ||
      path.includes('/p/') ||
      path.includes('/s/') ||
      path.length > 1
    ) {
      return true;
    }

    return false;
  } catch {
    return false;
  }
}

/**
 * Accurately estimates PDF pages without counting huge Base64 data URLs
 */
export function calculateAccurateEstimatedPages(messages: ChatMessage[]): number {
  if (!messages || messages.length === 0) return 1;
  let textLength = 0;
  let imageCount = 0;
  let codeBlocks = 0;

  for (const m of messages) {
    const stripped = m.content
      .replace(/!\[[\s\S]*?\]\(data:image\/[\s\S]*?\)/gi, '')
      .replace(/!\[[\s\S]*?\]\([^)]+\)/gi, '')
      .replace(/data:image\/[^;\s"'>]+;base64,[A-Za-z0-9+/=\s\r\n_-]+/gi, '')
      .trim();
    textLength += stripped.length;

    const imgMatches = m.content.match(/!\[[\s\S]*?\]\([\s\S]*?\)/g);
    if (imgMatches) {
      imageCount += imgMatches.length;
    }

    const codeMatches = (m.content.match(/```/g) || []).length;
    codeBlocks += Math.floor(codeMatches / 2);
  }

  const basePages = Math.ceil(textLength / 2850);
  const extraFromCode = Math.floor(codeBlocks * 0.25);
  const extraFromImages = Math.floor(imageCount * 0.4);
  return Math.max(1, basePages + extraFromCode + extraFromImages);
}
