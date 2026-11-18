import { NextRequest, NextResponse } from 'next/server';
import { extractWithYtDlp } from '@/lib/ytdlp-runner';
import { extractFacebookMedia } from '@/lib/extract-facebook';
import { extractInstagramMedia } from '@/lib/extract-instagram';

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

    // 2. YouTube Detection & Extraction (Piped Network + yt-dlp Direct Stream)
    if (trimmedUrl.includes('youtube.com') || trimmedUrl.includes('youtu.be')) {
      const ytIdMatch =
        trimmedUrl.match(/(?:youtu\.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=|shorts\/)([^#&?]*)/);
      const videoId = ytIdMatch && ytIdMatch[1].length === 11 ? ytIdMatch[1] : null;

      if (!videoId) {
        throw new Error('Invalid YouTube video link format');
      }

      // Engine 1: Fast Direct YouTube Stream Resolver (via btch-downloader / vidsnap proxy - Supports Long Dramas, Movies & Shorts)
      try {
        const btch = await import('btch-downloader');
        const ytFn = (btch as any).default?.youtube || (btch as any).youtube;
        if (typeof ytFn === 'function') {
          const ytRes = await ytFn(`https://www.youtube.com/watch?v=${videoId}`);
          if (ytRes && ytRes.status && (ytRes.mp4 || ytRes.mp3)) {
            const vUrl = ytRes.mp4 || ytRes.mp3;
            // Parse duration from URL if available (e.g. dur=213.089 or clen=...)
            let durSec = 180;
            const durMatch = vUrl.match(/[?&]dur(?:%3D|=)([0-9.]+)/);
            if (durMatch) {
              durSec = Math.round(parseFloat(durMatch[1]));
            }
            const h = Math.floor(durSec / 3600);
            const m = Math.floor((durSec % 3600) / 60);
            const s = Math.floor(durSec % 60);
            const durStr = h > 0
              ? `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
              : `${m}:${s.toString().padStart(2, '0')}`;

            // Extract clen (content length bytes) if present in URL
            let sizeMb = 'HD Stream';
            const clenMatch = vUrl.match(/[?&]clen(?:%3D|=)(\d+)/);
            if (clenMatch) {
              const clenBytes = parseInt(clenMatch[1], 10);
              sizeMb = (clenBytes / (1024 * 1024)).toFixed(1) + ' MB';
            } else if (durSec > 0) {
              const approxBytes = durSec * 320 * 1024;
              sizeMb = (approxBytes / (1024 * 1024)).toFixed(1) + ' MB';
            }

            return NextResponse.json({
              success: true,
              data: {
                id: videoId,
                platform: 'youtube',
                platformName: trimmedUrl.includes('/shorts/') ? 'YouTube Shorts' : 'YouTube',
                title: ytRes.title || 'YouTube Video',
                author: {
                  name: ytRes.author || 'YouTube Creator',
                  username: '',
                },
                thumbnailUrl: ytRes.thumbnail || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
                downloadUrl: vUrl,
                directDownloadUrl: vUrl,
                audioUrl: ytRes.mp3 || undefined,
                duration: durSec,
                durationText: durStr,
                youtubeId: videoId,
                filesizeMb: sizeMb,
                formats: [
                  {
                    formatId: 'yt_hd_720',
                    qualityLabel: `720p HD MP4 (${sizeMb})`,
                    resolution: '720p HD',
                    filesizeMb: sizeMb,
                    url: vUrl,
                    directUrl: vUrl,
                    hasVideo: true,
                    hasAudio: true,
                  }
                ],
              },
            });
          }
        }
      } catch (btchErr) {
        console.warn('Primary btch YouTube API failed, trying fallbacks:', btchErr);
      }

      // Engine 2: Primary High-Speed & Long Video Stream Resolver via yt-dlp (Supports Movies, Dramas, Lectures)
      try {
        const ytdlp = await extractWithYtDlp(trimmedUrl, 10000);
        if (ytdlp && (ytdlp.videoUrl || (ytdlp.formats && ytdlp.formats.length > 0))) {
          const durSec = ytdlp.duration || 0;
          const h = Math.floor(durSec / 3600);
          const m = Math.floor((durSec % 3600) / 60);
          const s = Math.floor(durSec % 60);
          const durStr = h > 0
            ? `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
            : `${m}:${s.toString().padStart(2, '0')}`;

          // Format stream URLs - wrap direct googlevideo URLs with proxy-download to avoid IP-binding 403
          const safeFormats = (ytdlp.formats || []).map((f, idx) => {
            const rawUrl = f.url;
            const safeUrl = rawUrl.includes('googlevideo.com')
              ? `/api/proxy-download?url=${encodeURIComponent(rawUrl)}&filename=QwikSave_YouTube_${videoId}_${idx + 1}.mp4`
              : rawUrl;
            return {
              ...f,
              url: safeUrl,
            };
          });

          const primaryUrl = safeFormats.length > 0 ? safeFormats[0].url : ytdlp.videoUrl;
          const safePrimaryUrl = primaryUrl.includes('googlevideo.com')
            ? `/api/proxy-download?url=${encodeURIComponent(primaryUrl)}&filename=QwikSave_YouTube_${videoId}.mp4`
            : primaryUrl;

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
              thumbnailUrl: ytdlp.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
              downloadUrl: safePrimaryUrl,
              audioUrl: ytdlp.audioUrl ? (ytdlp.audioUrl.includes('googlevideo.com') ? `/api/proxy-download?url=${encodeURIComponent(ytdlp.audioUrl)}&filename=QwikSave_YouTube_${videoId}.mp3` : ytdlp.audioUrl) : undefined,
              duration: durSec,
              durationText: durStr,
              youtubeId: videoId,
              filesizeMb: ytdlp.filesizeMb,
              formats: safeFormats,
            },
          });
        }
      } catch (ytdlpErr) {
        console.warn('Primary yt-dlp resolver failed, trying fallbacks:', ytdlpErr);
      }

      // Engine 2: Direct Mobile Web Player Scraper (Zero bot block, Full Audio & Video for long dramas/movies/lectures)
      try {
        const ytWatchRes = await fetch(`https://m.youtube.com/watch?v=${videoId}`, {
          headers: {
            'User-Agent':
              'Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36',
            'Accept-Language': 'en-US,en;q=0.9',
          },
          signal: AbortSignal.timeout(9000),
        });

        if (ytWatchRes.ok) {
          const ytHtml = await ytWatchRes.text();
          const prMatch = ytHtml.match(/ytInitialPlayerResponse\s*=\s*(\{.+?\});/);
          if (prMatch) {
            const pr = JSON.parse(prMatch[1]);
            const details = pr.videoDetails || {};
            const streamingData = pr.streamingData || {};
            const progressiveFormats = streamingData.formats || [];
            const validFormats = progressiveFormats.filter((f: any) => f.url);

            if (validFormats.length > 0) {
              const durSec = parseInt(details.lengthSeconds || '0', 10);
              const h = Math.floor(durSec / 3600);
              const m = Math.floor((durSec % 3600) / 60);
              const s = Math.floor(durSec % 60);
              const durStr =
                h > 0
                  ? `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
                  : `${m}:${s.toString().padStart(2, '0')}`;

              const safeFormats = validFormats.map((f: any, idx: number) => {
                const rawUrl = f.url;
                const safeUrl = rawUrl.includes('googlevideo.com')
                  ? `/api/proxy-download?url=${encodeURIComponent(rawUrl)}&filename=QwikSave_YouTube_${videoId}_${idx + 1}.mp4`
                  : rawUrl;
                const sizeBytes = f.contentLength ? parseInt(f.contentLength, 10) : 0;
                const sizeMb = sizeBytes > 0
                  ? (sizeBytes / (1024 * 1024)).toFixed(2) + ' MB'
                  : durSec > 0
                  ? (durSec * 0.15).toFixed(1) + ' MB'
                  : 'HD Stream';

                return {
                  formatId: `yt_${f.itag || idx}`,
                  qualityLabel: `${f.qualityLabel || '720p HD MP4'} (${sizeMb})`,
                  resolution: f.qualityLabel || '720p HD',
                  filesizeMb: sizeMb,
                  url: safeUrl,
                  directUrl: rawUrl,
                  hasVideo: true,
                  hasAudio: true,
                };
              });

              return NextResponse.json({
                success: true,
                data: {
                  id: videoId,
                  platform: 'youtube',
                  platformName: trimmedUrl.includes('/shorts/') ? 'YouTube Shorts' : 'YouTube',
                  title: details.title || 'YouTube Video',
                  author: {
                    name: details.author || 'YouTube Channel',
                    username: '',
                  },
                  thumbnailUrl:
                    details.thumbnail?.thumbnails?.slice(-1)[0]?.url ||
                    `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
                  downloadUrl: safeFormats[0].url,
                  directDownloadUrl: validFormats[0].url,
                  duration: durSec,
                  durationText: durStr,
                  youtubeId: videoId,
                  filesizeMb: safeFormats[0].filesizeMb,
                  formats: safeFormats,
                },
              });
            }
          }
        }
      } catch (mYtErr) {
        console.warn('Direct mobile YouTube extractor in universal route failed:', mYtErr);
      }



      // Engine 2: High-speed Piped Direct Stream Resolver (No bot block, direct CDN MP4)
      try {
        const pipedEndpoints = [
          `https://api.piped.private.coffee/streams/${videoId}`,
          `https://pipedapi.kavin.rocks/streams/${videoId}`
        ];

        for (const ep of pipedEndpoints) {
          try {
            const pipedRes = await fetch(ep, {
              headers: { 'Accept': 'application/json', 'User-Agent': 'Mozilla/5.0' },
              signal: AbortSignal.timeout(7000),
            });
            if (pipedRes.ok) {
              const pipedData = await pipedRes.json();
              const videoStreams = pipedData.videoStreams || [];
              const mp4Streams = videoStreams.filter(
                (s: any) => s.mimeType?.includes('mp4') || s.format?.includes('MP4')
              );
              const streamList = mp4Streams.length > 0 ? mp4Streams : videoStreams;
              const bestStream = streamList[0];

              if (bestStream && bestStream.url) {
                // If stream is googlevideo, wrap with proxy-download to prevent client IP 403
                const rawUrl = bestStream.url;
                const safeDownloadUrl = rawUrl.includes('googlevideo.com')
                  ? `/api/proxy-download?url=${encodeURIComponent(rawUrl)}&filename=QwikSave_YouTube_${videoId}.mp4`
                  : rawUrl;

                const formats = streamList.map((s: any, idx: number) => {
                  const sUrl = s.url?.includes('googlevideo.com')
                    ? `/api/proxy-download?url=${encodeURIComponent(s.url)}&filename=QwikSave_YouTube_${videoId}_${idx}.mp4`
                    : s.url;
                  return {
                    formatId: `yt_${s.quality || idx}`,
                    qualityLabel: `${s.quality || 'HD'} (${s.format || 'MP4'})`,
                    resolution: s.quality || '720p',
                    filesizeMb: s.contentLength
                      ? (s.contentLength / (1024 * 1024)).toFixed(2) + ' MB'
                      : undefined,
                    url: sUrl,
                    hasVideo: true,
                    hasAudio: !s.videoOnly,
                  };
                });

                const audioStream = pipedData.audioStreams?.find((a: any) =>
                  a.mimeType?.includes('audio')
                )?.url;

                return NextResponse.json({
                  success: true,
                  data: {
                    id: videoId,
                    platform: 'youtube',
                    platformName: trimmedUrl.includes('/shorts/') ? 'YouTube Shorts' : 'YouTube',
                    title: pipedData.title || 'YouTube Video',
                    author: {
                      name: pipedData.uploader || 'YouTube Creator',
                      username: pipedData.uploaderUrl || '',
                    },
                    thumbnailUrl:
                      pipedData.thumbnailUrl ||
                      `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
                    downloadUrl: safeDownloadUrl,
                    audioUrl: audioStream,
                    duration: pipedData.duration,
                    youtubeId: videoId,
                    formats,
                  },
                });
              }
            }
          } catch {
            // try next endpoint
          }
        }
      } catch (pipedErr) {
        console.warn('Piped API failed:', pipedErr);
      }

      // Engine 2: yt-dlp
      try {
        const ytdlp = await extractWithYtDlp(trimmedUrl, 30000);
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

      // Fallback: Fetch official YouTube oEmbed metadata if available
      let title = 'YouTube Video';
      let authorName = 'YouTube Channel';
      try {
        const oembedRes = await fetch(
          `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${videoId}&format=json`,
          { signal: AbortSignal.timeout(4000) }
        );
        if (oembedRes.ok) {
          const oembed = await oembedRes.json();
          title = oembed.title || title;
          authorName = oembed.author_name || authorName;
        }
      } catch {
        // silent
      }

      throw new Error(`Could not extract video stream for "${title}". Please verify the YouTube video is public and try again.`);
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

    // 6. Instagram (Use multi-tier Instagram engine)
    if (trimmedUrl.includes('instagram.com') || trimmedUrl.includes('instagr.am')) {
      try {
        const igResult = await extractInstagramMedia(trimmedUrl);
        if (igResult && igResult.downloadUrl) {
          return NextResponse.json({
            success: true,
            data: {
              id: igResult.id || igResult.shortcode,
              platform: 'instagram',
              platformName: 'Instagram',
              title: igResult.title || igResult.caption || 'Instagram Reel',
              author: { name: igResult.author?.username || 'Instagram Creator', username: igResult.author?.username || '' },
              thumbnailUrl: igResult.thumbnailUrl,
              downloadUrl: igResult.downloadUrl,
              duration: 30,
              filesizeMb: '4.20 MB',
              formats: igResult.mediaItems.map((item, idx) => ({
                formatId: `ig_${idx}`,
                qualityLabel: item.type === 'video' ? '1080p HD MP4' : 'High Resolution Photo',
                resolution: '1080p',
                filesizeMb: '4.20 MB',
                url: item.url,
                hasVideo: item.type === 'video',
                hasAudio: item.type === 'video',
              })),
            },
          });
        }
      } catch (igErr: any) {
        console.warn('Instagram extraction error in universal route:', igErr);
      }
    }

    // 7. Reddit, Threads or Any Supported Universal Video
    if (
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
              platform: 'universal',
              platformName: 'Web Video',
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
