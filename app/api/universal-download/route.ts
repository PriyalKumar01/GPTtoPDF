import { NextRequest, NextResponse } from 'next/server';
import { extractWithYtDlp } from '@/lib/ytdlp-runner';
import { extractFacebookMedia } from '@/lib/extract-facebook';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(request: NextRequest) {
  try {
    const { url } = await request.json().catch(() => ({}));

    if (!url || typeof url !== 'string' || !url.trim()) {
      return NextResponse.json({ error: 'Please provide a valid video link' }, { status: 400 });
    }

    const trimmedUrl = url.trim();

    // 1. TikTok Detection & Extraction (via TikWM API - High-speed, No watermark)
    if (trimmedUrl.includes('tiktok.com')) {
      const tikRes = await fetch(`https://tikwm.com/api/?url=${encodeURIComponent(trimmedUrl)}`);
      const tikJson = await tikRes.json();

      if (tikJson.code === 0 && tikJson.data) {
        const d = tikJson.data;
        return NextResponse.json({
          success: true,
          data: {
            id: d.id || `tiktok_${Date.now()}`,
            platform: 'tiktok',
            platformName: 'TikTok',
            title: d.title || 'TikTok Video',
            author: {
              name: d.author?.nickname || 'TikTok Creator',
              username: d.author?.unique_id || '',
              avatarUrl: d.author?.avatar,
            },
            thumbnailUrl: d.cover || d.origin_cover,
            downloadUrl: d.play, // Direct HD without watermark!
            hdDownloadUrl: d.hdplay || d.play,
            audioUrl: d.music,
            duration: d.duration,
            metrics: {
              views: d.play_count,
              likes: d.digg_count,
              comments: d.comment_count,
              shares: d.share_count,
            },
          },
        });
      } else {
        throw new Error(tikJson.msg || 'Unable to fetch this TikTok video.');
      }
    }

    // 2. YouTube Detection & Extraction
    if (trimmedUrl.includes('youtube.com') || trimmedUrl.includes('youtu.be')) {
      const ytIdMatch =
        trimmedUrl.match(/(?:youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=|shorts\/)([^#&?]*)/);
      const videoId = ytIdMatch && ytIdMatch[1].length === 11 ? ytIdMatch[1] : null;

      if (!videoId) {
        throw new Error('Invalid YouTube video link format');
      }

      // Try yt-dlp first for direct stream
      try {
        const ytdlp = await extractWithYtDlp(trimmedUrl, 45000);
        if (ytdlp && ytdlp.videoUrl) {
          return NextResponse.json({
            success: true,
            data: {
              id: videoId,
              platform: 'youtube',
              platformName: trimmedUrl.includes('/shorts/') ? 'YouTube Shorts' : 'YouTube',
              title: ytdlp.title,
              author: {
                name: ytdlp.uploader || ytdlp.channel || 'YouTube Channel',
                username: ytdlp.channel || '',
              },
              thumbnailUrl: ytdlp.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/maxresdefault.jpg`,
              downloadUrl: ytdlp.videoUrl,
              audioUrl: ytdlp.audioUrl,
              duration: ytdlp.duration,
              youtubeId: videoId,
              filesizeMb: ytdlp.filesizeMb,
              formats: ytdlp.formats || [],
            },
          });
        }
      } catch {
        // Fallback to oEmbed metadata
      }

      // Fallback: Fetch official YouTube oEmbed metadata
      const oembedRes = await fetch(
        `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${videoId}&format=json`
      );

      let title = 'YouTube Video';
      let authorName = 'YouTube Channel';
      if (oembedRes.ok) {
        const oembed = await oembedRes.json();
        title = oembed.title || title;
        authorName = oembed.author_name || authorName;
      }

      const hqThumbnail = `https://i.ytimg.com/vi/${videoId}/maxresdefault.jpg`;

      return NextResponse.json({
        success: true,
        data: {
          id: videoId,
          platform: 'youtube',
          platformName: trimmedUrl.includes('/shorts/') ? 'YouTube Shorts' : 'YouTube',
          title,
          author: {
            name: authorName,
            username: '',
          },
          thumbnailUrl: hqThumbnail,
          downloadUrl: `https://www.youtube.com/watch?v=${videoId}`,
          youtubeId: videoId,
        },
      });
    }

    // 3. Twitter / X Detection & Extraction
    if (trimmedUrl.includes('twitter.com') || trimmedUrl.includes('x.com')) {
      try {
        const ytdlp = await extractWithYtDlp(trimmedUrl, 20000);
        if (ytdlp && ytdlp.videoUrl) {
          return NextResponse.json({
            success: true,
            data: {
              id: ytdlp.id,
              platform: 'twitter',
              platformName: 'X (Twitter)',
              title: ytdlp.title,
              author: { name: ytdlp.uploader || 'X Creator', username: ytdlp.channel || '' },
              thumbnailUrl: ytdlp.thumbnailUrl,
              downloadUrl: ytdlp.videoUrl,
              audioUrl: ytdlp.audioUrl,
              duration: ytdlp.duration,
              filesizeMb: ytdlp.filesizeMb,
              formats: ytdlp.formats || [],
            },
          });
        }
      } catch {
        // Fallback
      }

      return NextResponse.json({
        success: true,
        data: {
          id: `x_${Date.now()}`,
          platform: 'twitter',
          platformName: 'X (Twitter)',
          title: 'X / Twitter Video Post',
          author: { name: 'X Creator', username: '' },
          thumbnailUrl: '',
          downloadUrl: trimmedUrl,
          notice: 'Twitter/X media URL received and parsed.',
        },
      });
    }

    // 4. Pinterest Detection & Extraction
    if (trimmedUrl.includes('pinterest.com') || trimmedUrl.includes('pin.it')) {
      try {
        const ytdlp = await extractWithYtDlp(trimmedUrl, 20000);
        if (ytdlp && ytdlp.videoUrl) {
          return NextResponse.json({
            success: true,
            data: {
              id: ytdlp.id,
              platform: 'pinterest',
              platformName: 'Pinterest',
              title: ytdlp.title,
              author: { name: ytdlp.uploader || 'Pinterest Creator', username: '' },
              thumbnailUrl: ytdlp.thumbnailUrl,
              downloadUrl: ytdlp.videoUrl,
              duration: ytdlp.duration,
            },
          });
        }
      } catch {
        // Fallback
      }

      return NextResponse.json({
        success: true,
        data: {
          id: `pin_${Date.now()}`,
          platform: 'pinterest',
          platformName: 'Pinterest',
          title: 'Pinterest Video / Pin',
          author: { name: 'Pinterest Creator', username: '' },
          thumbnailUrl: '',
          downloadUrl: trimmedUrl,
        },
      });
    }

    // 5. Facebook Detection & Extraction
    if (
      trimmedUrl.includes('facebook.com') ||
      trimmedUrl.includes('fb.watch') ||
      trimmedUrl.includes('fb.gg')
    ) {
      try {
        const fbResult = await extractFacebookMedia(trimmedUrl);
        return NextResponse.json({
          success: true,
          data: {
            id: fbResult.id,
            platform: 'facebook',
            platformName: 'Facebook',
            title: fbResult.title,
            author: { name: fbResult.author?.name || 'Facebook Creator', username: '' },
            thumbnailUrl: fbResult.thumbnailUrl,
            downloadUrl: fbResult.downloadUrl,
            formats: fbResult.formats.map((f) => ({
              formatId: f.quality,
              qualityLabel: f.label,
              resolution: f.resolution,
              filesizeMb: f.filesizeMb,
              url: f.url,
              hasVideo: true,
              hasAudio: true,
            })),
          },
        });
      } catch (fbErr: any) {
        console.warn('Facebook extraction error in universal route:', fbErr);
      }
    }

    // 6. Instagram, Reddit, Threads or Any Supported Universal Video
    if (
      trimmedUrl.includes('instagram.com') ||
      trimmedUrl.includes('reddit.com') ||
      trimmedUrl.includes('threads.net')
    ) {
      try {
        const ytdlp = await extractWithYtDlp(trimmedUrl, 25000);
        if (ytdlp && ytdlp.videoUrl) {
          return NextResponse.json({
            success: true,
            data: {
              id: ytdlp.id,
              platform: trimmedUrl.includes('instagram.com')
                ? 'instagram'
                : trimmedUrl.includes('facebook.com') || trimmedUrl.includes('fb.watch')
                ? 'facebook'
                : 'universal',
              platformName: trimmedUrl.includes('instagram.com')
                ? 'Instagram'
                : trimmedUrl.includes('facebook.com') || trimmedUrl.includes('fb.watch')
                ? 'Facebook'
                : 'Web Video',
              title: ytdlp.title,
              author: { name: ytdlp.uploader || ytdlp.channel || 'Creator', username: '' },
              thumbnailUrl: ytdlp.thumbnailUrl,
              downloadUrl: ytdlp.videoUrl,
              audioUrl: ytdlp.audioUrl,
              duration: ytdlp.duration,
              filesizeMb: ytdlp.filesizeMb,
              formats: ytdlp.formats || [],
            },
          });
        }
      } catch {
        // Fallback
      }
    }

    // Generic fallback with yt-dlp for any valid URL
    try {
      const ytdlp = await extractWithYtDlp(trimmedUrl, 20000);
      if (ytdlp && ytdlp.videoUrl) {
        return NextResponse.json({
          success: true,
          data: {
            id: ytdlp.id,
            platform: 'universal',
            platformName: 'Web Video',
            title: ytdlp.title,
            author: { name: ytdlp.uploader || ytdlp.channel || 'Creator', username: '' },
            thumbnailUrl: ytdlp.thumbnailUrl,
            downloadUrl: ytdlp.videoUrl,
            audioUrl: ytdlp.audioUrl,
            duration: ytdlp.duration,
          },
        });
      }
    } catch {
      // continue
    }

    throw new Error('Unsupported video platform. Please enter a valid Instagram, Facebook, YouTube, TikTok, or X link.');
  } catch (error: any) {
    return NextResponse.json(
      { success: false, error: error?.message || 'Failed to download universal media' },
      { status: 422 }
    );
  }
}
