import * as cheerio from 'cheerio';
import { extractWithYtDlp } from './ytdlp-runner';

export interface InstagramMediaItem {
  type: 'video' | 'photo';
  url: string;
  thumbnailUrl?: string;
  width?: number;
  height?: number;
}

export interface InstagramResult {
  id: string;
  shortcode: string;
  type: 'reel' | 'post' | 'carousel' | 'story' | 'video';
  title?: string;
  caption?: string;
  author: {
    username: string;
    fullName?: string;
    avatarUrl?: string;
    isVerified?: boolean;
  };
  metrics?: {
    likes?: number;
    views?: number;
    comments?: number;
  };
  thumbnailUrl: string;
  mediaItems: InstagramMediaItem[];
  downloadUrl: string;
  requiresAuth?: boolean;
  notice?: string;
}

const USER_AGENT_DESKTOP =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

export function extractInstagramShortcode(rawUrl: string): { shortcode: string; type: string } | null {
  try {
    const url = new URL(rawUrl.trim());
    const pathname = url.pathname;

    const reelMatch = pathname.match(/\/(?:reel|reels)\/([A-Za-z0-9_-]+)/);
    if (reelMatch) return { shortcode: reelMatch[1], type: 'reel' };

    const postMatch = pathname.match(/\/p\/([A-Za-z0-9_-]+)/);
    if (postMatch) return { shortcode: postMatch[1], type: 'post' };

    const tvMatch = pathname.match(/\/tv\/([A-Za-z0-9_-]+)/);
    if (tvMatch) return { shortcode: tvMatch[1], type: 'video' };

    const storyMatch = pathname.match(/\/stories\/[^/]+\/([0-9]+)/);
    if (storyMatch) return { shortcode: storyMatch[1], type: 'story' };

    const parts = pathname.split('/').filter(Boolean);
    if (parts.length >= 2 && ['p', 'reel', 'reels', 'tv'].includes(parts[0])) {
      return { shortcode: parts[1], type: parts[0] };
    }

    return null;
  } catch {
    return null;
  }
}

/**
 * Engine 1: instagram-url-direct package (most reliable, no login needed)
 */
async function extractViaInstagramUrlDirect(url: string): Promise<InstagramResult | null> {
  try {
    const pkg = await import('instagram-url-direct');
    const fn = (pkg as any).default?.instagramGetUrl || (pkg as any).instagramGetUrl || (pkg as any).default;
    if (typeof fn === 'function') {
      const result = await fn(url);
      const urlList: string[] = result?.url_list || (Array.isArray(result) ? result.map((r: any) => r.url || r) : []);
      if (urlList.length > 0) {
        const shortcode = extractInstagramShortcode(url)?.shortcode || `ig_${Date.now()}`;
        const firstUrl = urlList[0];

        const mediaItems: InstagramMediaItem[] = urlList.map((mediaUrl: string) => ({
          type: (mediaUrl.includes('.mp4') ? 'video' : 'photo') as 'video' | 'photo',
          url: mediaUrl,
          thumbnailUrl: mediaUrl,
        }));

        return {
          id: shortcode,
          shortcode,
          type: 'reel',
          caption: 'Instagram Reel',
          author: { username: 'instagram_creator' },
          thumbnailUrl: firstUrl,
          mediaItems,
          downloadUrl: firstUrl,
        };
      }
    }
  } catch (err) {
    console.warn('instagram-url-direct failed:', err);
  }
  return null;
}

/**
 * Engine 2: SnapSave API (handles Reels, Videos, and Carousel Photos)
 */
async function extractViaSnapSave(url: string, shortcode: string): Promise<InstagramResult | null> {
  try {
    const { snapsave } = await import('snapsave-media-downloader');
    const snapData = (await snapsave(url)) as any;

    if (snapData?.success && snapData?.data?.media?.length > 0) {
      const mediaList = snapData.data.media;
      const mediaItems: InstagramMediaItem[] = [];

      for (const item of mediaList) {
        if (!item?.url) continue;
        const isVid = item.url.includes('.mp4') || item.type === 'video';
        mediaItems.push({
          type: isVid ? 'video' : 'photo',
          url: item.url,
          thumbnailUrl: item.thumbnail || snapData.data.preview || item.url,
        });
      }

      if (mediaItems.length > 0) {
        const thumb = snapData.data.preview || mediaItems[0].thumbnailUrl || '';
        const isMultiple = mediaItems.length > 1;
        const firstIsVideo = mediaItems[0].type === 'video';

        return {
          id: shortcode,
          shortcode,
          type: isMultiple ? 'carousel' : (firstIsVideo ? 'reel' : 'post'),
          caption: snapData.data.description || (isMultiple ? 'Instagram Carousel' : 'Instagram Media'),
          author: { username: 'instagram_creator' },
          thumbnailUrl: thumb,
          mediaItems,
          downloadUrl: mediaItems[0].url,
        };
      }
    }
  } catch {
    // silent
  }
  return null;
}

/**
 * Engine 3: GraphQL with optional env cookie
 */
async function extractViaGraphQL(shortcode: string): Promise<InstagramResult | null> {
  try {
    const cookieHeader = process.env.INSTAGRAM_COOKIE || '';
    const csrf = cookieHeader.match(/csrftoken=([^;]+)/)?.[1] || 'missing';

    const docIds = ['8845758582119845', '10015901848480474'];
    for (const docId of docIds) {
      const apiUrl = `https://www.instagram.com/graphql/query/?doc_id=${docId}&variables=${encodeURIComponent(JSON.stringify({ shortcode }))}`;
      const res = await fetch(apiUrl, {
        headers: {
          'User-Agent': USER_AGENT_DESKTOP,
          'X-IG-App-ID': '936619743392459',
          'X-CSRFToken': csrf,
          Cookie: cookieHeader,
          Accept: '*/*',
        },
        signal: AbortSignal.timeout(8000),
      });

      if (!res.ok) continue;
      const json = await res.json();
      const media = json?.data?.xdt_shortcode_media || json?.data?.shortcode_media;
      if (!media) continue;

      const isVideo = media.is_video;
      const videoUrl = media.video_url || '';
      const displayUrl = media.display_url || '';
      const thumbnail = media.thumbnail_src || displayUrl;
      const captionEdge = media.edge_media_to_caption?.edges?.[0]?.node?.text || '';
      const ownerUsername = media.owner?.username || 'instagram_creator';

      if (isVideo && videoUrl) {
        return {
          id: shortcode,
          shortcode,
          type: 'reel',
          caption: captionEdge,
          author: { username: ownerUsername },
          thumbnailUrl: thumbnail,
          mediaItems: [{ type: 'video', url: videoUrl, thumbnailUrl: thumbnail }],
          downloadUrl: videoUrl,
        };
      } else if (displayUrl) {
        // Photo or carousel post
        const sidecarEdges = media.edge_sidecar_to_children?.edges || [];
        const mediaItems: InstagramMediaItem[] = sidecarEdges.length > 0
          ? sidecarEdges.map((e: any) => ({
              type: (e.node?.is_video ? 'video' : 'photo') as 'video' | 'photo',
              url: e.node?.video_url || e.node?.display_url || '',
              thumbnailUrl: e.node?.display_url || '',
            })).filter((m: any) => m.url)
          : [{ type: 'photo', url: displayUrl, thumbnailUrl: displayUrl }];

        return {
          id: shortcode,
          shortcode,
          type: sidecarEdges.length > 1 ? 'carousel' : 'post',
          caption: captionEdge,
          author: { username: ownerUsername },
          thumbnailUrl: thumbnail,
          mediaItems,
          downloadUrl: mediaItems[0]?.url || displayUrl,
        };
      }
    }
  } catch {
    // silent
  }
  return null;
}

/**
 * Engine 4: Embed endpoint approach (supports both /p/ posts and /reel/)
 */
async function extractViaEmbed(shortcode: string, isPost = false): Promise<InstagramResult | null> {
  try {
    const embedUrls = isPost
      ? [
          `https://www.instagram.com/p/${shortcode}/embed/captioned/`,
          `https://www.instagram.com/reel/${shortcode}/embed/captioned/`
        ]
      : [
          `https://www.instagram.com/reel/${shortcode}/embed/captioned/`,
          `https://www.instagram.com/p/${shortcode}/embed/captioned/`
        ];

    for (const embedUrl of embedUrls) {
      const res = await fetch(embedUrl, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram/303.0.0.11.111',
          Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          Referer: 'https://www.instagram.com/',
        },
        signal: AbortSignal.timeout(8000),
      });

      if (!res.ok) continue;
      const html = await res.text();
      const $ = cheerio.load(html);

      const videoSrc = $('video source').first().attr('src') || $('video').first().attr('src');
      const posterUrl = $('video').first().attr('poster') || $('img').first().attr('src') || '';
      const username = $('a.UsernameText').text().trim() || 'instagram_creator';
      const caption = $('.Caption').text().trim() || '';

      if (videoSrc) {
        return {
          id: shortcode,
          shortcode,
          type: 'reel',
          caption,
          author: { username },
          thumbnailUrl: posterUrl,
          mediaItems: [{ type: 'video', url: videoSrc, thumbnailUrl: posterUrl }],
          downloadUrl: videoSrc,
        };
      }

      // Check JSON data in script tags for video_url or display_url
      const videoUrlMatches = [...html.matchAll(/"video_url"\s*:\s*"([^"]+)"/g)];
      if (videoUrlMatches.length > 0) {
        const cleanedUrl = videoUrlMatches[0][1].replace(/\\u0026/g, '&').replace(/\\/g, '');
        return {
          id: shortcode,
          shortcode,
          type: 'reel',
          caption,
          author: { username },
          thumbnailUrl: posterUrl,
          mediaItems: [{ type: 'video', url: cleanedUrl, thumbnailUrl: posterUrl }],
          downloadUrl: cleanedUrl,
        };
      }

      // Extract all embedded images (for single photo or carousel)
      const foundImages: string[] = [];
      $('img.EmbeddedMediaImage, img').each((_, el) => {
        const src = $(el).attr('src');
        if (src && src.startsWith('http') && !src.includes('rsrc.php') && !src.includes('150x150') && !foundImages.includes(src)) {
          foundImages.push(src);
        }
      });

      if (foundImages.length > 0) {
        const mediaItems: InstagramMediaItem[] = foundImages.map((u) => ({
          type: 'photo',
          url: u,
          thumbnailUrl: u,
        }));

        return {
          id: shortcode,
          shortcode,
          type: mediaItems.length > 1 ? 'carousel' : 'post',
          caption,
          author: { username },
          thumbnailUrl: foundImages[0],
          mediaItems,
          downloadUrl: foundImages[0],
        };
      }
    }
  } catch {
    // silent
  }
  return null;
}

export async function extractInstagramMedia(inputUrl: string): Promise<InstagramResult> {
  const match = extractInstagramShortcode(inputUrl);
  if (!match) {
    throw new Error('Please enter a valid Instagram reel, post, video, or story link.');
  }

  const { shortcode, type } = match;
  const isPost = type === 'post';

  // Engine 1: instagram-url-direct (most reliable for Reels)
  const directResult = await extractViaInstagramUrlDirect(inputUrl);
  if (directResult) return directResult;

  // Engine 2: yt-dlp (server must have yt-dlp installed)
  try {
    const ytdlp = await extractWithYtDlp(inputUrl, 20000);
    if (ytdlp?.videoUrl) {
      return {
        id: ytdlp.id || shortcode,
        shortcode,
        type: 'reel',
        title: ytdlp.title,
        caption: ytdlp.description,
        author: {
          username: ytdlp.channel || ytdlp.uploader || 'instagram_creator',
          fullName: ytdlp.uploader,
        },
        metrics: { likes: ytdlp.likeCount, comments: ytdlp.commentCount },
        thumbnailUrl: ytdlp.thumbnailUrl,
        mediaItems: [{ type: 'video', url: ytdlp.videoUrl, thumbnailUrl: ytdlp.thumbnailUrl }],
        downloadUrl: ytdlp.videoUrl,
      };
    }
  } catch {
    // continue
  }

  // Engine 3: snapsave (supports Reels, Videos, and Carousel Photos)
  const snapResult = await extractViaSnapSave(inputUrl, shortcode);
  if (snapResult) return snapResult;

  // Engine 4: GraphQL
  const gqlResult = await extractViaGraphQL(shortcode);
  if (gqlResult) return gqlResult;

  // Engine 5: Embed (with /p/ and /reel/ support)
  const embedResult = await extractViaEmbed(shortcode, isPost);
  if (embedResult) return embedResult;

  throw new Error(
    'Instagram bot protection blocked this request. The post might be private or Instagram temporarily restricted access from this server. Try again in a few seconds.'
  );
}
