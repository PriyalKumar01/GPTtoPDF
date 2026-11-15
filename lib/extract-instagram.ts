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
const USER_AGENT_MOBILE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1';

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

async function getGuestCookies(): Promise<{ cookieHeader: string; csrf: string }> {
  try {
    const res = await fetch('https://www.instagram.com/', {
      headers: {
        'User-Agent': USER_AGENT_DESKTOP,
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      },
      next: { revalidate: 3600 },
    });
    const rawCookies = res.headers.getSetCookie ? res.headers.getSetCookie() : [];
    const cookieHeader = rawCookies.map((c) => c.split(';')[0]).join('; ');
    const csrfMatch = cookieHeader.match(/csrftoken=([^;]+)/);
    const csrf = csrfMatch ? csrfMatch[1] : '';
    return { cookieHeader, csrf };
  } catch {
    return { cookieHeader: '', csrf: '' };
  }
}

/**
 * Strategy 1: GraphQL Query with user or guest session
 */
async function extractViaGraphQL(shortcode: string, cookieHeader: string, csrf: string): Promise<InstagramResult | null> {
  try {
    const docIds = ['8845758582119845', '10015901848480474', '25531498899829322'];
    const customCookie = process.env.INSTAGRAM_COOKIE || cookieHeader;

    for (const docId of docIds) {
      const url = `https://www.instagram.com/graphql/query/?doc_id=${docId}&variables=${encodeURIComponent(
        JSON.stringify({ shortcode })
      )}`;

      const res = await fetch(url, {
        headers: {
          'User-Agent': USER_AGENT_DESKTOP,
          'X-IG-App-ID': '936619743392459',
          'X-ASBD-ID': '129477',
          'X-CSRFToken': csrf,
          Cookie: customCookie,
          Accept: '*/*',
        },
        next: { revalidate: 0 },
      });

      if (!res.ok) continue;
      const json = await res.json();
      const media = json?.data?.xdt_shortcode_media || json?.data?.shortcode_media;
      if (!media) continue;

      const isVideo = media.is_video;
      const primaryVideoUrl = media.video_url;
      const displayUrl = media.display_url;
      const mediaItems: InstagramMediaItem[] = [];

      const children = media.edge_sidecar_to_children?.edges;
      if (Array.isArray(children) && children.length > 0) {
        children.forEach((edge: any) => {
          const node = edge.node;
          if (node.is_video && node.video_url) {
            mediaItems.push({
              type: 'video',
              url: node.video_url,
              thumbnailUrl: node.display_url,
              width: node.dimensions?.width,
              height: node.dimensions?.height,
            });
          } else if (node.display_url) {
            mediaItems.push({
              type: 'photo',
              url: node.display_url,
              thumbnailUrl: node.display_url,
            });
          }
        });
      } else {
        if (isVideo && primaryVideoUrl) {
          mediaItems.push({
            type: 'video',
            url: primaryVideoUrl,
            thumbnailUrl: displayUrl,
          });
        } else if (displayUrl) {
          mediaItems.push({
            type: 'photo',
            url: displayUrl,
            thumbnailUrl: displayUrl,
          });
        }
      }

      if (mediaItems.length > 0) {
        return {
          id: media.id || shortcode,
          shortcode,
          type: mediaItems.length > 1 ? 'carousel' : isVideo ? 'reel' : 'post',
          caption: media.edge_media_to_caption?.edges?.[0]?.node?.text,
          author: {
            username: media.owner?.username || 'instagram_user',
            fullName: media.owner?.full_name,
            avatarUrl: media.owner?.profile_pic_url,
            isVerified: media.owner?.is_verified,
          },
          metrics: {
            likes: media.edge_media_preview_like?.count,
            views: media.video_view_count,
            comments: media.edge_media_to_comment?.count,
          },
          thumbnailUrl: displayUrl || mediaItems[0].thumbnailUrl || '',
          mediaItems,
          downloadUrl: mediaItems[0].url,
        };
      }
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Strategy 2: Embed Scraping
 */
async function extractViaEmbed(shortcode: string): Promise<InstagramResult | null> {
  try {
    const embedUrl = `https://www.instagram.com/p/${shortcode}/embed/captioned/`;
    const res = await fetch(embedUrl, {
      headers: {
        'User-Agent': USER_AGENT_DESKTOP,
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) return null;
    const html = await res.text();
    const $ = cheerio.load(html);

    const directVideoUrl = $('video.EmbeddedMediaVideo, video').attr('src');
    const posterUrl = $('video').attr('poster') || $('img.EmbeddedMediaImage').attr('src') || '';
    const caption = $('.Caption').text().trim() || $('div[class*="Caption"]').text().trim();
    const username = $('.UsernameText').text().trim() || 'instagram_creator';
    const avatar = $('img.Avatar').attr('src') || '';

    if (directVideoUrl) {
      return {
        id: shortcode,
        shortcode,
        type: 'reel',
        caption: caption || undefined,
        author: { username, avatarUrl: avatar || undefined },
        thumbnailUrl: posterUrl,
        mediaItems: [{ type: 'video', url: directVideoUrl, thumbnailUrl: posterUrl }],
        downloadUrl: directVideoUrl,
      };
    }

    const videoUrlMatches = [
      ...html.matchAll(/"video_url"\s*:\s*"([^"]+)"/g),
      ...html.matchAll(/\\\"video_url\\\"\s*:\s*\\\"([^\\\"]+)\\\"/g),
    ];
    if (videoUrlMatches.length > 0) {
      const rawMatch = videoUrlMatches[0][1];
      const cleanedUrl = rawMatch.replace(/\\u0026/g, '&').replace(/\\/g, '');
      return {
        id: shortcode,
        shortcode,
        type: 'reel',
        caption: caption || undefined,
        author: { username, avatarUrl: avatar || undefined },
        thumbnailUrl: posterUrl,
        mediaItems: [{ type: 'video', url: cleanedUrl, thumbnailUrl: posterUrl }],
        downloadUrl: cleanedUrl,
      };
    }

    return null;
  } catch {
    return null;
  }
}

/**
 * Strategy 3: OpenGraph & Social Crawler Unfurling
 */
async function extractViaOpenGraph(shortcode: string): Promise<InstagramResult | null> {
  try {
    const url = `https://www.instagram.com/reel/${shortcode}/`;
    const res = await fetch(url, {
      headers: {
        'User-Agent': 'facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)',
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) return null;
    const html = await res.text();
    const $ = cheerio.load(html);

    const ogVideo =
      $('meta[property="og:video"]').attr('content') ||
      $('meta[property="og:video:secure_url"]').attr('content');
    const ogImage = $('meta[property="og:image"]').attr('content') || '';
    const ogTitle = $('meta[property="og:title"]').attr('content') || 'Instagram Reel';
    const ogDesc = $('meta[property="og:description"]').attr('content') || '';

    // If og:video exists, return complete result
    if (ogVideo) {
      return {
        id: shortcode,
        shortcode,
        type: 'reel',
        title: ogTitle,
        caption: ogDesc,
        author: { username: ogTitle.split('•')?.[0]?.trim() || 'instagram_creator' },
        thumbnailUrl: ogImage,
        mediaItems: [{ type: 'video', url: ogVideo, thumbnailUrl: ogImage }],
        downloadUrl: ogVideo,
      };
    }

    // If at least og:image and ogTitle were retrieved, we can provide post metadata
    if (ogImage && ogTitle) {
      return {
        id: shortcode,
        shortcode,
        type: 'reel',
        title: ogTitle,
        caption: ogDesc,
        author: { username: ogTitle.split('on Instagram')?.[0]?.trim() || 'instagram_creator' },
        thumbnailUrl: ogImage,
        mediaItems: [{ type: 'photo', url: ogImage, thumbnailUrl: ogImage }],
        downloadUrl: ogImage,
        requiresAuth: true,
        notice:
          'Instagram has activated temporary rate-limiting for automated video streaming on this post. You can download the high-resolution cover photo below or open the post directly.',
      };
    }

    return null;
  } catch {
    return null;
  }
}

export async function extractInstagramMedia(inputUrl: string): Promise<InstagramResult> {
  const match = extractInstagramShortcode(inputUrl);
  if (!match) {
    throw new Error('Please enter a valid Instagram reel, post, video, or story link.');
  }

  const { shortcode } = match;

  // Tier 1: High-reliability direct media engine (yt-dlp)
  try {
    const ytdlp = await extractWithYtDlp(inputUrl, 25000);
    if (ytdlp && ytdlp.videoUrl) {
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
        metrics: {
          likes: ytdlp.likeCount,
          comments: ytdlp.commentCount,
        },
        thumbnailUrl: ytdlp.thumbnailUrl,
        mediaItems: [
          {
            type: 'video',
            url: ytdlp.videoUrl,
            thumbnailUrl: ytdlp.thumbnailUrl,
          },
        ],
        downloadUrl: ytdlp.videoUrl,
      };
    }
  } catch {
    // continue to fallbacks
  }

  // Tier 2: GraphQL query with cookies
  const { cookieHeader, csrf } = await getGuestCookies();
  let result = await extractViaGraphQL(shortcode, cookieHeader, csrf);
  if (!result) {
    result = await extractViaEmbed(shortcode);
  }
  if (!result) {
    result = await extractViaOpenGraph(shortcode);
  }

  if (!result) {
    throw new Error(
      'Instagram bot protection blocked this request. The post might be private or Instagram temporarily restricted public access from this server IP.'
    );
  }

  return result;
}
