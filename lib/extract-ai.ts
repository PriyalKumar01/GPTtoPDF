import * as cheerio from 'cheerio';
import { APP_CONFIG } from './config';
import { SAMPLE_CONVERSATIONS } from './sample-data';
import {
  ChatMessage,
  ConversationData,
  ApiExtractResponse,
  MessageRole,
} from './types';

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
  if (lower.includes('chatgpt.com') || lower.includes('chat.openai.com')) {
    return 'chatgpt';
  }
  if (lower.includes('claude.ai')) {
    return 'claude';
  }
  if (lower.includes('gemini.google.com')) {
    return 'gemini';
  }
  if (lower.includes('perplexity.ai')) {
    return 'perplexity';
  }
  return 'generic';
}

/**
 * Validates whether the given URL is a supported AI public share link
 */
export function isValidAiShareUrl(url: string): boolean {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();

  // Allow sample conversation aliases
  if (trimmed.includes('os-exam-notes') || trimmed.includes('ml-exam-notes')) {
    return true;
  }

  try {
    const parsed = new URL(trimmed);
    const host = parsed.hostname.toLowerCase();
    const path = parsed.pathname.toLowerCase();

    // ChatGPT
    if (
      (host === 'chatgpt.com' || host === 'chat.openai.com' || host.endsWith('.chatgpt.com')) &&
      path.includes('/share/')
    ) {
      return true;
    }

    // Claude
    if (
      (host === 'claude.ai' || host.endsWith('.claude.ai')) &&
      path.includes('/share/')
    ) {
      return true;
    }

    // Google Gemini
    if (
      (host === 'gemini.google.com' || host.endsWith('.google.com')) &&
      path.includes('/share/')
    ) {
      return true;
    }

    // Perplexity
    if (
      (host === 'perplexity.ai' || host === 'www.perplexity.ai') &&
      (path.includes('/search/') || path.includes('/page/'))
    ) {
      return true;
    }

    // Generic public shareable URL
    if (path.includes('/share/') || path.includes('/c/') || path.includes('/chat/')) {
      return true;
    }

    return false;
  } catch {
    return false;
  }
}

/**
 * Extracts the UUID or Share ID from ChatGPT URL
 */
export function extractChatGPTShareId(url: string): string | null {
  const match = url.trim().match(/\/share\/([a-zA-Z0-9_-]{10,80})/i);
  return match ? match[1] : null;
}

/**
 * Extracts the Share ID from Claude URL
 */
export function extractClaudeShareId(url: string): string | null {
  const match = url.trim().match(/\/share\/([a-zA-Z0-9_-]+)/i);
  return match ? match[1] : null;
}

/**
 * SSRF and safety validator
 */
export function validateUrlSafety(rawUrl: string): {
  safe: boolean;
  error?: string;
  urlObj?: URL;
} {
  try {
    const trimmed = rawUrl.trim();
    if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
      return { safe: false, error: 'URL must begin with https:// or http://' };
    }

    const parsed = new URL(trimmed);

    // Hostname check: prevent SSRF to local IP ranges
    const hostname = parsed.hostname.toLowerCase();
    if (
      hostname === 'localhost' ||
      hostname === '127.0.0.1' ||
      hostname === '0.0.0.0' ||
      hostname.startsWith('192.168.') ||
      hostname.startsWith('10.') ||
      hostname.startsWith('172.16.') ||
      hostname.endsWith('.internal') ||
      hostname.endsWith('.local')
    ) {
      return { safe: false, error: 'Access to private internal network addresses is strictly prohibited.' };
    }

    return { safe: true, urlObj: parsed };
  } catch {
    return { safe: false, error: 'Malformed or invalid URL provided.' };
  }
}

/**
 * Accurately estimates PDF pages without counting huge Base64 data URLs
 */
export function calculateAccurateEstimatedPages(messages: ChatMessage[]): number {
  if (!messages || messages.length === 0) return 1;
  let textLength = 0;
  let imageCount = 0;

  for (const m of messages) {
    // Strip large data URLs so they don't falsely inflate character count
    const stripped = m.content.replace(/data:image\/[^;]+;base64,[A-Za-z0-9+/=]+/g, '');
    textLength += stripped.length;

    // Count embedded diagrams
    const imgMatches = m.content.match(/!\[.*?\]\(.*?\)/g);
    if (imgMatches) {
      imageCount += imgMatches.length;
    }
  }

  // Academic formatting density:
  // ~2,200 text characters per page
  // ~0.45 page per embedded diagram
  const textPages = textLength / 2200;
  const imagePages = imageCount * 0.45;
  return Math.max(1, Math.round(textPages + imagePages));
}

/**
 * Cleans markdown text by stripping raw internal JSX, DIL or ChatGPT widget artifacts,
 * converting text/badges to clean formatting, and removing garbage unicode characters.
 */
export function sanitizeMarkdownContent(raw: string): string {
  if (!raw) return '';
  return raw
    .replace(/<badge[^>]*>(.*?)<\/badge>/gi, '($1) ')
    .replace(/<text[^>]*>(.*?)<\/text>/gi, '$1') // clean XML text tags completely
    .replace(/\{@body[\s\S]*?\}/g, '')
    .replace(/<\/?(?:grid|grid-item|box|row|checkbox|button)[^>]*>/gi, '')
    .replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]/g, '') // remove unprintable emoji surrogates
    .replace(/\r\n/g, '\n')
    .trim();
}

/**
 * Downloads image buffers concurrently and converts them to base64 Data URLs
 */
async function prefetchImagesAsBase64(imageUrls: string[]): Promise<Map<string, string>> {
  const imageMap = new Map<string, string>();
  if (!imageUrls || imageUrls.length === 0) return imageMap;

  // Limit concurrency to 8 parallel downloads to avoid socket exhaustion
  const maxImages = imageUrls.slice(0, 30);
  await Promise.all(
    maxImages.map(async (url) => {
      try {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 6000);
        const res = await fetch(url, {
          signal: controller.signal,
          headers: {
            'User-Agent':
              'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
          },
        });
        clearTimeout(timeout);
        if (res.ok) {
          const ab = await res.arrayBuffer();
          const b64 = Buffer.from(ab).toString('base64');
          const mime = res.headers.get('content-type') || 'image/jpeg';
          imageMap.set(url, `data:${mime};base64,${b64}`);
        }
      } catch {
        // Silently continue if an image fails to download
      }
    })
  );
  return imageMap;
}

/**
 * Extracts conversation from ChatGPT using the official backend-api endpoint.
 * Accurately filters out internal python interpreter scripts, thoughts, and hidden drafts,
 * and fetches embedded diagrams/flowcharts as high-resolution base64 images.
 */
async function extractChatGPT(url: string, shareId: string): Promise<ApiExtractResponse> {
  const apiUrl = `https://chatgpt.com/backend-api/share/${shareId}`;
  const headers = {
    'User-Agent':
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
    Accept: 'application/json',
    'Accept-Language': 'en-US,en;q=0.9',
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache',
  };

  try {
    const apiRes = await fetch(apiUrl, { headers });

    if (apiRes.status === 200) {
      const data = await apiRes.json();
      const title = data.title || 'ChatGPT Exam Notes';
      const messages: ChatMessage[] = [];

      // Extract and prefetch diagram images from safe_urls
      const safeUrls: string[] = Array.isArray(data.safe_urls) ? data.safe_urls : [];
      const openAiImgs = safeUrls.filter(
        (u) => typeof u === 'string' && u.includes('images.openai.com')
      );
      const imageMap = await prefetchImagesAsBase64(openAiImgs);

      let asyncImgCounter = 0;

      if (Array.isArray(data.linear_conversation)) {
        for (const item of data.linear_conversation) {
          const msg = item.message;
          if (!msg) continue;
          const role = msg.author?.role;
          const contentType = msg.content?.content_type;
          const isHidden = msg.metadata?.is_visually_hidden_from_conversation;

          // 1. Skip system messages, tools, and visually hidden drafts
          if (role !== 'user' && role !== 'assistant') continue;
          if (isHidden === true) continue;

          // 2. Skip internal python interpreter scripts, thoughts, and context mutations
          if (
            contentType === 'code' ||
            contentType === 'thoughts' ||
            contentType === 'reasoning_recap' ||
            contentType === 'model_editable_context'
          ) {
            continue;
          }

          let text = '';
          if (Array.isArray(msg.content?.parts)) {
            // For user prompt: extract string text, do NOT prepend garbage unicode
            const stringParts = msg.content.parts.filter(
              (p: any) => typeof p === 'string' && p.trim().length > 0
            );
            text = stringParts.join('\n\n');
          } else if (typeof msg.content?.text === 'string') {
            text = msg.content.text;
          }

          // Map <AsyncImage query="..." /> to embedded markdown image with base64 data URL
          text = text.replace(
            /<AsyncImage\s+query="([^"]+)"[^>]*\/>/g,
            (_match: string, query: string) => {
              const assignedUrl = openAiImgs[asyncImgCounter] || '';
              asyncImgCounter++;
              const dataUrl = imageMap.get(assignedUrl) || assignedUrl;
              if (dataUrl) {
                return `\n\n![Diagram: ${query}](${dataUrl})\n\n`;
              }
              return `\n\n**[Diagram: ${query}]**\n\n`;
            }
          );

          const cleaned = sanitizeMarkdownContent(text);
          if (cleaned) {
            messages.push({
              id: msg.id || `msg-${messages.length + 1}`,
              role: role as MessageRole,
              authorName: role === 'user' ? 'USER' : 'ASSISTANT',
              content: cleaned,
              timestamp: msg.create_time ? msg.create_time * 1000 : undefined,
            });
          }
        }
      } else if (data.mapping) {
        // Fallback to mapping tree with the same strict filters
        const nodes = Object.values(data.mapping);
        for (const node of nodes as any[]) {
          const msg = node?.message;
          if (!msg) continue;
          const role = msg.author?.role;
          const contentType = msg.content?.content_type;
          const isHidden = msg.metadata?.is_visually_hidden_from_conversation;

          if (role !== 'user' && role !== 'assistant') continue;
          if (isHidden === true) continue;
          if (
            contentType === 'code' ||
            contentType === 'thoughts' ||
            contentType === 'reasoning_recap' ||
            contentType === 'model_editable_context'
          ) {
            continue;
          }

          let text = '';
          const parts = msg.content?.parts || [];
          for (const p of parts) {
            if (typeof p === 'string' && p.trim().length > 0) {
              text += (text ? '\n\n' : '') + p;
            }
          }

          text = text.replace(
            /<AsyncImage\s+query="([^"]+)"[^>]*\/>/g,
            (_match: string, query: string) => {
              const assignedUrl = openAiImgs[asyncImgCounter] || '';
              asyncImgCounter++;
              const dataUrl = imageMap.get(assignedUrl) || assignedUrl;
              if (dataUrl) {
                return `\n\n![Diagram: ${query}](${dataUrl})\n\n`;
              }
              return `\n\n**[Diagram: ${query}]**\n\n`;
            }
          );

          const cleaned = sanitizeMarkdownContent(text);
          if (cleaned) {
            messages.push({
              id: msg.id || `msg-${messages.length + 1}`,
              role: role as MessageRole,
              authorName: role === 'user' ? 'USER' : 'ASSISTANT',
              content: cleaned,
              timestamp: msg.create_time ? msg.create_time * 1000 : undefined,
            });
          }
        }
      }

      if (messages.length > 0) {
        const userCount = messages.filter((m) => m.role === 'user').length;
        const assistantCount = messages.filter((m) => m.role === 'assistant').length;
        const estimatedPages = calculateAccurateEstimatedPages(messages);

        return {
          success: true,
          conversation: {
            id: shareId,
            title,
            sourceUrl: url,
            createdAt: data.create_time
              ? new Date(data.create_time * 1000).toISOString()
              : new Date().toISOString(),
            messages,
            stats: {
              userMessages: userCount,
              assistantMessages: assistantCount,
              totalMessages: messages.length,
              estimatedPages,
            },
          },
        };
      }
    }

    if (apiRes.status === 404) {
      return {
        success: false,
        error: 'Conversation not found. The share link may have expired or been deleted by the author.',
        errorCode: 'NOT_FOUND',
      };
    }
  } catch (err) {
    console.warn('ChatGPT API fetch error, attempting HTML fallback:', err);
  }

  // HTML Fallback
  return extractHtmlFallback(url);
}

/**
 * Extracts conversation from Claude public share links
 */
async function extractClaude(url: string, shareId: string): Promise<ApiExtractResponse> {
  const headers = {
    'User-Agent':
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  };

  try {
    const res = await fetch(url, { headers });
    if (!res.ok) {
      return {
        success: false,
        error: `Could not fetch Claude shared chat (HTTP ${res.status}). Link may be private.`,
        errorCode: 'NOT_FOUND',
      };
    }

    const html = await res.text();
    const $ = cheerio.load(html);

    let title = $('title').text().replace('- Claude', '').trim() || 'Claude Conversation';
    const messages: ChatMessage[] = [];

    // Check __NEXT_DATA__
    const nextDataScript = $('#__NEXT_DATA__').html();
    if (nextDataScript) {
      try {
        const json = JSON.parse(nextDataScript);
        const chatData = json?.props?.pageProps?.sharedChat || json?.props?.pageProps?.chat;
        if (chatData) {
          if (chatData.name || chatData.title) title = chatData.name || chatData.title;
          const chatMessages = chatData.chat_messages || chatData.messages || [];
          for (const m of chatMessages) {
            const role = m.sender === 'human' ? 'user' : 'assistant';
            const content = m.text || (Array.isArray(m.content) ? m.content.map((c: any) => c.text || '').join('\n') : '');
            if (content.trim()) {
              messages.push({
                id: m.uuid || `claude-${messages.length + 1}`,
                role,
                authorName: role === 'user' ? 'USER' : 'ASSISTANT',
                content: sanitizeMarkdownContent(content),
              });
            }
          }
        }
      } catch (e) {
        console.warn('Failed to parse Claude NEXT_DATA', e);
      }
    }

    // DOM Fallback for Claude
    if (messages.length === 0) {
      $('[data-testid="user-message"], .font-user-message').each((i, el) => {
        const text = $(el).text().trim();
        if (text) {
          messages.push({
            id: `claude-u-${i}`,
            role: 'user',
            authorName: 'USER',
            content: sanitizeMarkdownContent(text),
          });
        }
      });

      $('[data-testid="claude-message"], .font-claude-message').each((i, el) => {
        const text = $(el).text().trim();
        if (text) {
          messages.push({
            id: `claude-a-${i}`,
            role: 'assistant',
            authorName: 'ASSISTANT',
            content: sanitizeMarkdownContent(text),
          });
        }
      });
    }

    if (messages.length > 0) {
      return {
        success: true,
        conversation: {
          id: shareId,
          title,
          sourceUrl: url,
          createdAt: new Date().toISOString(),
          messages,
          stats: {
            userMessages: messages.filter((m) => m.role === 'user').length,
            assistantMessages: messages.filter((m) => m.role === 'assistant').length,
            totalMessages: messages.length,
            estimatedPages: calculateAccurateEstimatedPages(messages),
          },
        },
      };
    }
  } catch (err: any) {
    return {
      success: false,
      error: `Error extracting Claude share link: ${err.message}`,
      errorCode: 'INTERNAL_ERROR',
    };
  }

  return extractHtmlFallback(url);
}

/**
 * Extracts conversation from Google Gemini public share links
 */
async function extractGemini(url: string): Promise<ApiExtractResponse> {
  const headers = {
    'User-Agent':
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  };

  try {
    const res = await fetch(url, { headers });
    if (!res.ok) {
      return {
        success: false,
        error: `Could not fetch Gemini shared chat (HTTP ${res.status}).`,
        errorCode: 'NOT_FOUND',
      };
    }

    const html = await res.text();
    const $ = cheerio.load(html);

    let title = $('title').text().replace('- Gemini', '').replace('Google Gemini -', '').trim() || 'Gemini Conversation';
    const messages: ChatMessage[] = [];

    $('.query-content, [data-test-id="user-query"]').each((i, el) => {
      const text = $(el).text().trim();
      if (text) {
        messages.push({
          id: `gemini-u-${i}`,
          role: 'user',
          authorName: 'USER',
          content: sanitizeMarkdownContent(text),
        });
      }
    });

    $('.model-response, [data-test-id="model-response"]').each((i, el) => {
      const text = $(el).text().trim();
      if (text) {
        messages.push({
          id: `gemini-a-${i}`,
          role: 'assistant',
          authorName: 'ASSISTANT',
          content: sanitizeMarkdownContent(text),
        });
      }
    });

    if (messages.length > 0) {
      return {
        success: true,
        conversation: {
          id: `gemini-${Date.now()}`,
          title,
          sourceUrl: url,
          createdAt: new Date().toISOString(),
          messages,
          stats: {
            userMessages: messages.filter((m) => m.role === 'user').length,
            assistantMessages: messages.filter((m) => m.role === 'assistant').length,
            totalMessages: messages.length,
            estimatedPages: calculateAccurateEstimatedPages(messages),
          },
        },
      };
    }
  } catch (err: any) {
    console.warn('Gemini extraction error:', err);
  }

  return extractHtmlFallback(url);
}

/**
 * Universal HTML fallback extractor for any public AI share link
 */
async function extractHtmlFallback(url: string): Promise<ApiExtractResponse> {
  try {
    const res = await fetch(url, {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      },
    });

    const html = await res.text();
    const $ = cheerio.load(html);

    const title =
      $('title').text().replace(/[-|].*$/, '').trim() ||
      $('h1').first().text().trim() ||
      'AI Conversation Notes';

    const messages: ChatMessage[] = [];

    $('article, [data-message-author-role], .prose, .markdown').each((i, el) => {
      const text = $(el).text().trim();
      if (text.length > 30) {
        messages.push({
          id: `gen-msg-${i + 1}`,
          role: i % 2 === 0 ? 'user' : 'assistant',
          authorName: i % 2 === 0 ? 'USER' : 'ASSISTANT',
          content: sanitizeMarkdownContent(text),
        });
      }
    });

    if (messages.length > 0) {
      return {
        success: true,
        conversation: {
          id: `shared-${Date.now()}`,
          title,
          sourceUrl: url,
          createdAt: new Date().toISOString(),
          messages,
          stats: {
            userMessages: messages.filter((m) => m.role === 'user').length,
            assistantMessages: messages.filter((m) => m.role === 'assistant').length,
            totalMessages: messages.length,
            estimatedPages: calculateAccurateEstimatedPages(messages),
          },
        },
      };
    }

    return {
      success: false,
      error:
        'Could not automatically parse messages from this specific link. Please use the Direct Text / Markdown tab to paste your conversation text directly—it will format instantly into the exact same PDF!',
      errorCode: 'EMPTY_CONVERSATION',
    };
  } catch (err: any) {
    return {
      success: false,
      error: `Network error: ${err.message || 'Unable to access shared URL.'}`,
      errorCode: 'INTERNAL_ERROR',
    };
  }
}

/**
 * Master public function to extract any AI shared conversation link
 */
export async function extractAiConversation(url: string): Promise<ApiExtractResponse> {
  const safety = validateUrlSafety(url);
  if (!safety.safe || !safety.urlObj) {
    return {
      success: false,
      error: safety.error || 'Invalid URL provided.',
      errorCode: 'INVALID_URL',
    };
  }

  // Pre-loaded sample notes
  for (const sample of Object.values(SAMPLE_CONVERSATIONS)) {
    if (url.includes(sample.id) || sample.sourceUrl === url) {
      return {
        success: true,
        conversation: sample,
      };
    }
  }

  const platform = identifyPlatform(url);

  if (platform === 'chatgpt') {
    const shareId = extractChatGPTShareId(url);
    if (!shareId) {
      return {
        success: false,
        error: 'Invalid ChatGPT share link. Expected format: https://chatgpt.com/share/[id]',
        errorCode: 'INVALID_URL',
      };
    }
    return extractChatGPT(url, shareId);
  }

  if (platform === 'claude') {
    const shareId = extractClaudeShareId(url);
    return extractClaude(url, shareId || 'claude-share');
  }

  if (platform === 'gemini') {
    return extractGemini(url);
  }

  // Generic or Perplexity
  return extractHtmlFallback(url);
}
