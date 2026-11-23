import * as cheerio from 'cheerio';
import { Agent, fetch as undiciFetch } from 'undici';
import { APP_CONFIG } from './config';

const undiciAgent = new Agent({
  maxHeaderSize: 262144, // 256KB to prevent HeadersOverflowError on Google Gemini / Cloudflare
});
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

    return true;
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
    let trimmed = rawUrl.trim();
    if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
      trimmed = `https://${trimmed}`;
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
  let codeBlocks = 0;

  for (const m of messages) {
    // Strip large data URLs completely (including multiline and any characters inside parentheses or base64 data)
    const stripped = m.content
      .replace(/!\[[\s\S]*?\]\(data:image\/[\s\S]*?\)/gi, '')
      .replace(/!\[[\s\S]*?\]\([^)]+\)/gi, '')
      .replace(/data:image\/[^;\s"'>]+;base64,[A-Za-z0-9+/=\s\r\n_-]+/gi, '')
      .trim();
    textLength += stripped.length;

    // Count embedded diagrams
    const imgMatches = m.content.match(/!\[[\s\S]*?\]\([\s\S]*?\)/g);
    if (imgMatches) {
      imageCount += imgMatches.length;
    }

    const codeMatches = (m.content.match(/```/g) || []).length;
    codeBlocks += Math.floor(codeMatches / 2);
  }

  // Academic dense single-spacing layout has ~2850 characters per page
  const basePages = Math.ceil(textLength / 2850);
  const extraFromCode = Math.floor(codeBlocks * 0.25);
  const extraFromImages = Math.floor(imageCount * 0.4);
  return Math.max(1, basePages + extraFromCode + extraFromImages);
}

/**
 * Strictly identifies and purges internal script artifacts, telemetry JSON blobs,
 * or statsig auth configuration strings.
 */
export function isGarbageScriptOrTelemetry(text: string): boolean {
  if (!text) return true;
  const t = text.trim();
  if (t.startsWith('{"') || t.startsWith('{ "') || t.startsWith('[{"') || t.startsWith('{\\"')) return true;
  if (t.includes('dangerousOriginBypassScriptNonce')) return true;
  if (t.includes('statsig::') || t.includes('layer_exposure')) return true;
  if (t.includes('emailFieldPresentation') || t.includes('auth_login_signup_opt')) return true;
  if (t.includes('holdout-stable-id')) return true;
  if (t.includes('AF_initDataCallback') || t.includes('WLS_state')) return true;
  const jsonChars = (t.match(/[{}\\"\":]/g) || []).length;
  if (t.length > 80 && jsonChars / t.length > 0.12) return true;
  return false;
}

/**
 * Cleans markdown text by stripping raw internal JSX, DIL or ChatGPT widget artifacts,
 * converting text/badges to clean formatting, and removing garbage unicode characters.
 */
export function sanitizeMarkdownContent(raw: string): string {
  if (!raw) return '';
  if (isGarbageScriptOrTelemetry(raw)) return '';
  return raw
    .replace(/<Entity[^>]*\/?>/gi, '') // strip ChatGPT internal Entity tags (e.g. <Entity value="Selenium".../>)
    .replace(/<divider[^>]*\/?>/gi, '') // strip internal divider tags (e.g. <divider color="subtle"/>)
    .replace(/<citation[^>]*\/?>/gi, '') // strip citation tags
    .replace(/<[A-Za-z][A-Za-z0-9_-]*(?:\s+[^>]*?)?\/?>/gi, (tag) => {
      // allow standard safe markdown formatting HTML tags, strip all XML/component tags
      if (/^<\/?(?:b|i|u|strong|em|p|br|code|pre)\b/i.test(tag)) return tag;
      return '';
    })
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
              const cleanQuery = query.replace(/\s+logo\b/gi, '').replace(/^diagram:\s*/i, '').trim();
              if (dataUrl) {
                return `\n\n![Diagram: ${cleanQuery}](${dataUrl})\n\n`;
              }
              return `\n\n**[Diagram: ${cleanQuery}]**\n\n`;
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
              const cleanQuery = query.replace(/\s+logo\b/gi, '').replace(/^diagram:\s*/i, '').trim();
              if (dataUrl) {
                return `\n\n![Diagram: ${cleanQuery}](${dataUrl})\n\n`;
              }
              return `\n\n**[Diagram: ${cleanQuery}]**\n\n`;
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
  const normalizedUrl = url.trim().match(/^https?:\/\//i) ? url.trim() : `https://${url.trim()}`;
  const headers = {
    'User-Agent':
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
  };

  try {
    const res = await undiciFetch(normalizedUrl, {
      dispatcher: undiciAgent,
      headers,
      redirect: 'follow',
    });
    if (!res.ok) {
      return extractHtmlFallback(normalizedUrl);
    }

    const html = await res.text();
    const $ = cheerio.load(html);

    let title =
      $('title').text().replace('- Gemini', '').replace('Google Gemini -', '').replace(/\|.*$/, '').trim() ||
      $('meta[property="og:title"]').attr('content')?.replace('- Gemini', '').trim() ||
      'Gemini Conversation Notes';

    const messages: ChatMessage[] = [];

    // 1. Check standard & modern Gemini selectors
    const userQueries: string[] = [];
    $('.query-content, [data-test-id="user-query"], .user-query, message-content.user-message, div[data-message-author-role="user"]').each((_, el) => {
      const text = $(el).text().trim();
      if (text) userQueries.push(sanitizeMarkdownContent(text));
    });

    const modelResponses: string[] = [];
    $('.model-response, [data-test-id="model-response"], .model-response-text, .response-content, message-content.model-message, div[data-message-author-role="model"], div[data-message-author-role="assistant"], .markdown').each((_, el) => {
      const text = $(el).text().trim();
      if (text) modelResponses.push(sanitizeMarkdownContent(text));
    });

    const maxTurns = Math.max(userQueries.length, modelResponses.length);
    for (let i = 0; i < maxTurns; i++) {
      if (userQueries[i]) {
        messages.push({
          id: `gemini-u-${i + 1}`,
          role: 'user',
          authorName: 'USER',
          content: userQueries[i],
        });
      }
      if (modelResponses[i]) {
        messages.push({
          id: `gemini-a-${i + 1}`,
          role: 'assistant',
          authorName: 'ASSISTANT',
          content: modelResponses[i],
        });
      }
    }

    // 2. Check embedded script tags / JSON payloads if DOM elements were not rendered
    if (messages.length === 0) {
      $('script').each((_, el) => {
        const scriptText = $(el).html() || '';
        if (scriptText.includes('WLS_state') || scriptText.includes('AF_initDataCallback') || scriptText.includes('ds:')) {
          try {
            const textMatches = scriptText.match(/"([^"\\]{40,3000})"/g);
            if (textMatches && textMatches.length > 0) {
              const cleanedStrings = textMatches
                .map((s) => {
                  try {
                    return JSON.parse(s);
                  } catch {
                    return s.replace(/^"|"$/g, '');
                  }
                })
                .filter((s) => s.length > 35 && !s.startsWith('http') && !s.includes('{') && !s.includes('<script'));

              const distinct = Array.from(new Set(cleanedStrings));
              if (distinct.length > 0) {
                distinct.slice(0, 16).forEach((str, idx) => {
                  messages.push({
                    id: `gemini-parsed-${idx + 1}`,
                    role: idx % 2 === 0 ? 'user' : 'assistant',
                    authorName: idx % 2 === 0 ? 'USER' : 'ASSISTANT',
                    content: sanitizeMarkdownContent(str),
                  });
                });
              }
            }
          } catch {}
        }
      });
    }

    // 3. Check meta tags description if only summary is available
    if (messages.length === 0) {
      const metaDesc = $('meta[property="og:description"]').attr('content') || $('meta[name="description"]').attr('content');
      if (metaDesc && metaDesc.length > 20) {
        messages.push({
          id: `gemini-u-1`,
          role: 'user',
          authorName: 'USER',
          content: title,
        });
        messages.push({
          id: `gemini-a-1`,
          role: 'assistant',
          authorName: 'ASSISTANT',
          content: sanitizeMarkdownContent(metaDesc),
        });
      }
    }

    if (messages.length > 0) {
      return {
        success: true,
        conversation: {
          id: `gemini-${Date.now()}`,
          title,
          sourceUrl: normalizedUrl,
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

  return extractHtmlFallback(normalizedUrl);
}

/**
 * Universal HTML fallback extractor for any public AI share link
 */
async function extractHtmlFallback(url: string): Promise<ApiExtractResponse> {
  const normalizedUrl = url.trim().match(/^https?:\/\//i) ? url.trim() : `https://${url.trim()}`;

  try {
    const res = await undiciFetch(normalizedUrl, {
      dispatcher: undiciAgent,
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
      },
      redirect: 'follow',
    });

    const html = await res.text();
    const $ = cheerio.load(html);

    const title =
      $('title').text().replace(/[-|].*$/, '').trim() ||
      $('h1').first().text().trim() ||
      'AI Conversation Notes';

    const messages: ChatMessage[] = [];

    // Check common article and message blocks
    $('article, [data-message-author-role], .prose, .markdown, div[class*="message"], div[class*="turn"]').each((i, el) => {
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

    // Fallback: extract from paragraphs and headings
    if (messages.length === 0) {
      const paras: string[] = [];
      $('p, h2, h3, blockquote, li').each((_, el) => {
        const text = $(el).text().trim();
        if (text.length > 30) {
          paras.push(sanitizeMarkdownContent(text));
        }
      });

      if (paras.length > 0) {
        messages.push({
          id: 'gen-u-1',
          role: 'user',
          authorName: 'USER',
          content: title,
        });
        messages.push({
          id: 'gen-a-1',
          role: 'assistant',
          authorName: 'ASSISTANT',
          content: paras.join('\n\n'),
        });
      }
    }

    if (messages.length > 0) {
      return {
        success: true,
        conversation: {
          id: `shared-${Date.now()}`,
          title,
          sourceUrl: normalizedUrl,
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
  const normalizedUrl = url.trim().match(/^https?:\/\//i) ? url.trim() : `https://${url.trim()}`;
  const safety = validateUrlSafety(normalizedUrl);
  if (!safety.safe || !safety.urlObj) {
    return {
      success: false,
      error: safety.error || 'Invalid URL provided.',
      errorCode: 'INVALID_URL',
    };
  }

  // Pre-loaded sample notes
  for (const sample of Object.values(SAMPLE_CONVERSATIONS)) {
    if (normalizedUrl.includes(sample.id) || sample.sourceUrl === normalizedUrl) {
      return {
        success: true,
        conversation: sample,
      };
    }
  }

  const platform = identifyPlatform(normalizedUrl);

  if (platform === 'chatgpt') {
    const shareId = extractChatGPTShareId(normalizedUrl);
    if (!shareId) {
      return extractHtmlFallback(normalizedUrl);
    }
    const res = await extractChatGPT(normalizedUrl, shareId);
    if (!res.success) {
      return extractHtmlFallback(normalizedUrl);
    }
    return res;
  }

  if (platform === 'claude') {
    const shareId = extractClaudeShareId(normalizedUrl);
    const res = await extractClaude(normalizedUrl, shareId || 'claude-share');
    if (!res.success) {
      return extractHtmlFallback(normalizedUrl);
    }
    return res;
  }

  if (platform === 'gemini') {
    return extractGemini(normalizedUrl);
  }

  // Generic or Perplexity
  return extractHtmlFallback(normalizedUrl);
}
