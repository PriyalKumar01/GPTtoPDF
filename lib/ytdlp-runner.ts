import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);

export interface YtDlpMediaFormat {
  formatId: string;
  url: string;
  ext: string;
  resolution?: string;
  width?: number;
  height?: number;
  hasVideo: boolean;
  hasAudio: boolean;
  qualityLabel: string;
  filesizeMb: string;
}

export interface YtDlpExtractionResult {
  id: string;
  title: string;
  description?: string;
  uploader?: string;
  channel?: string;
  thumbnailUrl: string;
  duration?: number;
  likeCount?: number;
  commentCount?: number;
  videoUrl: string;
  audioUrl?: string;
  filesizeMb?: string;
  formats: YtDlpMediaFormat[];
}

function calculateMb(f: any, duration?: number): string {
  if (f?.filesize && f.filesize > 0) {
    return `${(f.filesize / (1024 * 1024)).toFixed(2)} MB`;
  }
  if (f?.filesize_approx && f.filesize_approx > 0) {
    return `${(f.filesize_approx / (1024 * 1024)).toFixed(2)} MB`;
  }
  const bitrate = f?.tbr || (f?.vbr ? f.vbr + (f?.abr || 128) : 0);
  if (bitrate > 0 && duration && duration > 0) {
    const bytes = (bitrate * 1000 * duration) / 8;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  }

  // Sensible heuristics based on resolution or audio
  const h = f?.height || (f?.width && f.width >= 1080 ? 1080 : f?.width && f.width >= 720 ? 720 : 480);
  if (h >= 1080) {
    return duration && duration > 0
      ? `${Math.max(1.5, (duration * 0.22)).toFixed(2)} MB`
      : '5.85 MB';
  }
  if (h >= 720) {
    return duration && duration > 0
      ? `${Math.max(0.9, (duration * 0.14)).toFixed(2)} MB`
      : '3.58 MB';
  }
  if (h >= 480) {
    return duration && duration > 0
      ? `${Math.max(0.5, (duration * 0.08)).toFixed(2)} MB`
      : '2.10 MB';
  }
  return duration && duration > 0
    ? `${Math.max(0.3, (duration * 0.05)).toFixed(2)} MB`
    : '1.20 MB';
}

export async function extractWithYtDlp(url: string, timeoutMs = 45000): Promise<YtDlpExtractionResult | null> {
  try {
    const { stdout } = await execFileAsync(
      'python',
      [
        '-m',
        'yt_dlp',
        '--extractor-args',
        'youtube:player_client=android,ios,web',
        '--dump-json',
        '--no-warnings',
        '--no-playlist',
        url.trim(),
      ],
      {
        timeout: timeoutMs,
        maxBuffer: 60 * 1024 * 1024,
      }
    );

    if (!stdout || !stdout.trim()) {
      return null;
    }

    const lines = stdout.split('\n');
    let parsedData: any = null;

    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
        try {
          parsedData = JSON.parse(trimmed);
          break;
        } catch {
          continue;
        }
      }
    }

    if (!parsedData) {
      return null;
    }

    const duration = parsedData.duration || undefined;
    const allFormats: any[] = Array.isArray(parsedData.formats) ? parsedData.formats : [];
    let bestAudioUrl: string | undefined = undefined;
    let bestAudioMb: string | undefined = undefined;

    // Map by distinct quality tiers to eliminate duplicates
    // Tiers: 1080p, 720p, 480p, 360p
    const tierMap = new Map<string, { f: any; priority: number }>();

    for (const f of allFormats) {
      if (!f.url) continue;

      const hasVideo = f.vcodec && f.vcodec !== 'none';
      const hasAudio = f.acodec && f.acodec !== 'none';

      if (!hasVideo && hasAudio && !bestAudioUrl) {
        bestAudioUrl = f.url;
        bestAudioMb = calculateMb(f, duration);
      }

      if (hasVideo) {
        const height = f.height || 0;
        const width = f.width || 0;
        const maxDim = Math.max(height, width);

        let tierKey = '480p SD';
        let qualityLabel = '480p Standard';
        let rank = 1;

        if (maxDim >= 1920 || height >= 1080 || width >= 1080) {
          tierKey = '1080p Full HD';
          qualityLabel = '1080p Full HD';
          rank = 4;
        } else if (maxDim >= 1280 || height >= 720 || width >= 720) {
          tierKey = '720p HD';
          qualityLabel = '720p HD';
          rank = 3;
        } else if (height >= 480 || width >= 480) {
          tierKey = '480p SD';
          qualityLabel = '480p SD';
          rank = 2;
        } else {
          tierKey = '360p Mobile';
          qualityLabel = '360p Mobile';
          rank = 1;
        }

        // Score this format: progressive (has audio) gets higher score
        const priorityScore = (hasAudio ? 1000 : 0) + (f.tbr || f.vbr || 0);

        if (!tierMap.has(tierKey) || tierMap.get(tierKey)!.priority < priorityScore) {
          tierMap.set(tierKey, {
            f: {
              ...f,
              qualityLabel,
              rank,
            },
            priority: priorityScore,
          });
        }
      }
    }

    // Convert tierMap to sorted array descending by rank
    const sortedFormats = Array.from(tierMap.values())
      .map(({ f }) => {
        const resStr = f.resolution || (f.width && f.height ? `${f.width}x${f.height}` : f.qualityLabel);
        return {
          formatId: f.format_id || 'mp4',
          url: f.url,
          ext: f.ext || 'mp4',
          resolution: resStr,
          width: f.width,
          height: f.height,
          hasVideo: true,
          hasAudio: Boolean(f.acodec && f.acodec !== 'none'),
          qualityLabel: f.qualityLabel,
          filesizeMb: calculateMb(f, duration),
          rank: f.rank,
        };
      })
      .sort((a, b) => b.rank - a.rank);

    // If no tiered formats found but parsedData.url is available
    if (sortedFormats.length === 0 && parsedData.url) {
      sortedFormats.push({
        formatId: 'mp4',
        url: parsedData.url,
        ext: 'mp4',
        resolution: 'HD',
        width: 1080,
        height: 1920,
        hasVideo: true,
        hasAudio: true,
        qualityLabel: '720p HD',
        filesizeMb: duration ? `${Math.max(1.2, duration * 0.12).toFixed(2)} MB` : '3.58 MB',
        rank: 3,
      });
    }

    // Best primary video URL
    const bestVideoUrl = parsedData.url || (sortedFormats.length > 0 ? sortedFormats[0].url : '');
    const topFilesizeMb = sortedFormats.length > 0 ? sortedFormats[0].filesizeMb : '3.58 MB';

    if (!bestVideoUrl) {
      return null;
    }

    return {
      id: parsedData.id || `media_${Date.now()}`,
      title: parsedData.title || parsedData.description?.slice(0, 80) || 'Video',
      description: parsedData.description || parsedData.title || '',
      uploader: parsedData.uploader || parsedData.channel || 'Creator',
      channel: parsedData.channel || parsedData.uploader || '',
      thumbnailUrl: parsedData.thumbnail || '',
      duration,
      likeCount: parsedData.like_count,
      commentCount: parsedData.comment_count,
      videoUrl: bestVideoUrl,
      audioUrl: bestAudioUrl,
      filesizeMb: topFilesizeMb,
      formats: sortedFormats,
    };
  } catch {
    return null;
  }
}
