import * as cheerio from 'cheerio';
import { APP_CONFIG } from './config';
import { SAMPLE_CONVERSATIONS } from './sample-data';
import {
  ChatMessage,
  ConversationData,
  ApiExtractResponse,
  MessageRole,
} from './types';

// Regex for extracting share ID from chatgpt.com or chat.openai.com URLs
const CHATGPT_SHARE_REGEX =
  /(?:https?:\/\/)?(?:www\.)?(?:chatgpt\.com|chat\.openai\.com)\/share\/([a-zA-Z0-9_-]{10,80})/i;

/**
 * Validates whether the given string is a supported ChatGPT share URL
 */
export function isValidChatGptUrl(url: string): boolean {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();
  return CHATGPT_SHARE_REGEX.test(trimmed);
}

/**
 * Extracts the share ID from a valid ChatGPT URL
 */
export function extractShareId(url: string): string | null {
  const match = url.trim().match(CHATGPT_SHARE_REGEX);
  return match ? match[1] : null;
}

/**
 * SSRF Protection: Ensures the URL belongs exclusively to permitted OpenAI/ChatGPT domains
 */
export function validateUrlSafety(rawUrl: string): {
  safe: boolean;
  error?: string;
  urlObj?: URL;
} {
  try {
    const trimmed = rawUrl.trim();
    if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
      return { safe: false, error: 'URL must begin with https://' };
    }

    const parsed = new URL(trimmed);

    // Only allow HTTPS
    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
      return { safe: false, error: 'Only HTTP/HTTPS protocols are permitted.' };
    }

    // Hostname check
    const hostname = parsed.hostname.toLowerCase();
    const isAllowedHost = APP_CONFIG.supportedHosts.some(
      (allowed) => hostname === allowed || hostname.endsWith(`.${allowed}`)
    );

    if (!isAllowedHost) {
      return {
        safe: false,
        error: `Only official ChatGPT share links (chatgpt.com or chat.openai.com) are supported.`,
      };
    }

    // Prohibit localhost, internal IPs
    if (
      hostname === 'localhost' ||
      hostname === '127.0.0.1' ||
      hostname.startsWith('192.168.') ||
      hostname.startsWith('10.') ||
      hostname.startsWith('172.16.')
    ) {
      return { safe: false, error: 'Access to private addresses is prohibited.' };
    }

    return { safe: true, urlObj: parsed };
  } catch {
    return { safe: false, error: 'Malformed or invalid URL provided.' };
  }
}

/**
 * Parses raw ChatGPT linear conversation mapping into structured ChatMessage array
 */
function parseMappingToMessages(mapping: Record<string, any>): {
  title?: string;
  messages: ChatMessage[];
} {
  const messages: ChatMessage[] = [];
  const nodes = Object.values(mapping || {});

  // Sort nodes chronologically if create_time is available, or follow parent-child tree
  const messageNodes = nodes.filter(
    (node: any) =>
      node?.message &&
      node.message?.content?.parts &&
      node.message?.author?.role &&
      node.message.author.role !== 'system'
  );

  // Sort by create_time
  messageNodes.sort((a: any, b: any) => {
    const timeA = a.message?.create_time || 0;
    const timeB = b.message?.create_time || 0;
    return timeA - timeB;
  });

  for (const node of messageNodes) {
    const msg = node.message;
    const role: MessageRole =
      msg.author.role === 'assistant' ? 'assistant' : 'user';
    const authorName = role === 'assistant' ? 'ChatGPT' : 'You';

    // Content extraction
    let textContent = '';
    const parts = msg.content?.parts || [];
    for (const part of parts) {
      if (typeof part === 'string') {
        textContent += (textContent ? '\n\n' : '') + part;
      } else if (part && typeof part === 'object') {
        if (part.text) {
          textContent += (textContent ? '\n\n' : '') + part.text;
        }
      }
    }

    if (textContent.trim()) {
      messages.push({
        id: msg.id || `msg-${messages.length + 1}`,
        role,
        authorName,
        content: textContent.trim(),
        timestamp: msg.create_time ? msg.create_time * 1000 : undefined,
      });
    }
  }

  return { messages };
}

/**
 * Extracts messages from server rendered HTML or DOM structures
 */
function parseHtmlConversation(html: string): {
  title?: string;
  messages: ChatMessage[];
} {
  const $ = cheerio.load(html);
  const messages: ChatMessage[] = [];

  // Attempt to extract title
  const title =
    $('title').text().replace('- ChatGPT', '').replace('ChatGPT - ', '').trim() ||
    $('h1').first().text().trim() ||
    'ChatGPT Conversation';

  // Strategy A: look for articles or role attributes
  const roleElements = $('[data-message-author-role]');
  if (roleElements.length > 0) {
    roleElements.each((index, el) => {
      const roleAttr = $(el).attr('data-message-author-role') || 'assistant';
      const role: MessageRole = roleAttr === 'user' ? 'user' : 'assistant';
      const text = $(el).text().trim();
      if (text) {
        messages.push({
          id: `html-msg-${index + 1}`,
          role,
          authorName: role === 'user' ? 'You' : 'ChatGPT',
          content: text,
        });
      }
    });
  }

  return { title, messages };
}

/**
 * Estimates PDF page count based on character and line heuristics
 */
export function estimatePdfPages(messages: ChatMessage[]): number {
  if (!messages || messages.length === 0) return 1;
  let totalChars = 0;
  let codeBlocksCount = 0;
  let imageCount = 0;

  for (const msg of messages) {
    const stripped = (msg.content || '').replace(/data:image\/[^;]+;base64,[A-Za-z0-9+/=]+/g, '');
    totalChars += stripped.length;
    const codeMatches = stripped.match(/```/g);
    if (codeMatches) {
      codeBlocksCount += Math.floor(codeMatches.length / 2);
    }
    const imgMatches = msg.content.match(/!\[.*?\]\(.*?\)/g);
    if (imgMatches) {
      imageCount += imgMatches.length;
    }
  }

  // Academic formatting density: ~2,200 text chars per page, + 0.45 per diagram
  const rawPages = Math.ceil(totalChars / 2200) + Math.ceil(codeBlocksCount * 0.3) + Math.ceil(imageCount * 0.45);
  return Math.max(1, rawPages);
}

/**
 * Main backend function to fetch and extract a public ChatGPT conversation
 */
export async function extractChatGptConversation(
  url: string
): Promise<ApiExtractResponse> {
  // 1. Safety validation
  const safety = validateUrlSafety(url);
  if (!safety.safe || !safety.urlObj) {
    return {
      success: false,
      error: safety.error || 'Invalid ChatGPT share link.',
      errorCode: 'INVALID_URL',
    };
  }

  const shareId = extractShareId(url);
  if (!shareId) {
    return {
      success: false,
      error:
        'Could not find a valid conversation share ID in the provided URL.',
      errorCode: 'INVALID_URL',
    };
  }

  // Check if user requested one of our pre-built sample notes for instant demo testing
  for (const sample of Object.values(SAMPLE_CONVERSATIONS)) {
    if (url.includes(sample.id) || sample.sourceUrl === url) {
      return {
        success: true,
        conversation: sample,
      };
    }
  }

  // 2. Fetch the shared conversation page from ChatGPT
  const fetchUrl = `https://chatgpt.com/share/${shareId}`;

  try {
    const headers = {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
      Accept:
        'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
      'Accept-Language': 'en-US,en;q=0.9',
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache',
      'Sec-Ch-Ua':
        '"Chromium";v="128", "Not;A=Brand";v="24", "Google Chrome";v="128"',
      'Sec-Ch-Ua-Mobile': '?0',
      'Sec-Ch-Ua-Platform': '"Windows"',
      'Sec-Fetch-Dest': 'document',
      'Sec-Fetch-Mode': 'navigate',
      'Sec-Fetch-Site': 'none',
      'Sec-Fetch-User': '?1',
      'Upgrade-Insecure-Requests': '1',
    };

    const response = await fetch(fetchUrl, {
      headers,
      redirect: 'follow',
    });

    if (response.status === 404) {
      return {
        success: false,
        error:
          'Conversation not found. The link may have been deleted, made private, or expired by the author.',
        errorCode: 'NOT_FOUND',
      };
    }

    if (response.status === 403 || response.status === 429) {
      return {
        success: false,
        error:
          "OpenAI's bot verification protected this link from server-side fetching. You can easily paste your conversation text or markdown using our Direct Paste tab below!",
        errorCode: 'BOT_PROTECTED',
      };
    }

    const html = await response.text();

    // 3. Check for Cloudflare / Turnstile challenges in HTML
    if (
      html.includes('Just a moment...') ||
      html.includes('cloudflare') && html.includes('challenge') ||
      html.includes('cf-browser-verification')
    ) {
      return {
        success: false,
        error:
          "OpenAI Cloudflare verification prevented automated extraction for this specific IP. Please use the Direct Paste tab to convert your conversation instantly.",
        errorCode: 'BOT_PROTECTED',
      };
    }

    // 4. Extract from __NEXT_DATA__ JSON script tag
    let extractedMessages: ChatMessage[] = [];
    let extractedTitle = 'ChatGPT Conversation';

    const $ = cheerio.load(html);
    const nextDataScript = $('#__NEXT_DATA__').html();

    if (nextDataScript) {
      try {
        const nextData = JSON.parse(nextDataScript);
        const pageProps = nextData?.props?.pageProps;
        const serverResponse = pageProps?.serverResponse?.data;
        const sharedConversation =
          pageProps?.sharedConversation || serverResponse;

        if (sharedConversation) {
          if (sharedConversation.title) {
            extractedTitle = sharedConversation.title;
          }

          if (sharedConversation.mapping) {
            const parsed = parseMappingToMessages(sharedConversation.mapping);
            if (parsed.messages.length > 0) {
              extractedMessages = parsed.messages;
            }
          } else if (Array.isArray(sharedConversation.linear_conversation)) {
            // Some versions have linear_conversation array
            for (const item of sharedConversation.linear_conversation) {
              if (item?.message) {
                const role: MessageRole =
                  item.message.author?.role === 'assistant'
                    ? 'assistant'
                    : 'user';
                const parts = item.message.content?.parts || [];
                const content = parts.filter((p: any) => typeof p === 'string').join('\n\n');
                if (content.trim()) {
                  extractedMessages.push({
                    id: item.message.id || `msg-${extractedMessages.length + 1}`,
                    role,
                    authorName: role === 'user' ? 'You' : 'ChatGPT',
                    content: content.trim(),
                  });
                }
              }
            }
          }
        }
      } catch (err) {
        console.warn('Failed to parse __NEXT_DATA__ json:', err);
      }
    }

    // 5. If not found in __NEXT_DATA__, search for JSON payload in Next App Router streaming scripts
    if (extractedMessages.length === 0) {
      $('script').each((_, elem) => {
        const scriptText = $(elem).html() || '';
        if (scriptText.includes('"mapping":') || scriptText.includes('"linear_conversation":')) {
          try {
            // Find mapping JSON snippet
            const mappingMatch = scriptText.match(/"mapping":\s*({[\s\S]*?})\s*,\s*"linear_conversation"/);
            if (mappingMatch && mappingMatch[1]) {
              const mapping = JSON.parse(mappingMatch[1]);
              const parsed = parseMappingToMessages(mapping);
              if (parsed.messages.length > 0) {
                extractedMessages = parsed.messages;
              }
            }
          } catch {
            // continue search
          }
        }
      });
    }

    // 6. DOM parsing fallback
    if (extractedMessages.length === 0) {
      const domResult = parseHtmlConversation(html);
      if (domResult.messages.length > 0) {
        extractedMessages = domResult.messages;
        if (domResult.title) extractedTitle = domResult.title;
      }
    }

    // 7. Verify we got messages
    if (extractedMessages.length === 0) {
      return {
        success: false,
        error:
          'Could not find readable conversation content in this shared link. The link might require an active login or is empty.',
        errorCode: 'EMPTY_CONVERSATION',
      };
    }

    const userCount = extractedMessages.filter((m) => m.role === 'user').length;
    const assistantCount = extractedMessages.filter(
      (m) => m.role === 'assistant'
    ).length;
    const estimatedPages = estimatePdfPages(extractedMessages);

    const conversationData: ConversationData = {
      id: shareId,
      title: extractedTitle,
      sourceUrl: url,
      createdAt: new Date().toISOString(),
      messages: extractedMessages,
      stats: {
        userMessages: userCount,
        assistantMessages: assistantCount,
        totalMessages: extractedMessages.length,
        estimatedPages,
      },
    };

    return {
      success: true,
      conversation: conversationData,
    };
  } catch (err: any) {
    return {
      success: false,
      error: `Network error while contacting ChatGPT: ${err.message || 'Unknown error'}`,
      errorCode: 'INTERNAL_ERROR',
    };
  }
}
