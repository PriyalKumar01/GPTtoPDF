import { NextRequest, NextResponse } from 'next/server';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/**
 * Universal Proxy Download Route
 * Bypasses CORS and CDN hotlink protections (Instagram/Facebook/YouTube/etc.)
 * by fetching media upstream and piping it with Content-Disposition: attachment.
 */
export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const mediaUrl = searchParams.get('url');
    const rawFilename = searchParams.get('filename') || 'download_media.mp4';

    if (!mediaUrl) {
      return NextResponse.json({ error: 'Missing media "url" query parameter' }, { status: 400 });
    }

    // Sanitize filename
    const filename = rawFilename.replace(/[^a-zA-Z0-9._-]/g, '_');

    // Parse URL to ensure it's a valid HTTP(S) URL
    let parsedUrl: URL;
    try {
      parsedUrl = new URL(mediaUrl);
      if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
        throw new Error('Invalid protocol');
      }
    } catch {
      return NextResponse.json({ error: 'Invalid URL provided' }, { status: 400 });
    }

    // Set proper headers based on upstream host
    const headers: Record<string, string> = {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      Accept: '*/*',
      'Accept-Language': 'en-US,en;q=0.9',
    };

    if (parsedUrl.hostname.includes('cdninstagram') || parsedUrl.hostname.includes('instagram')) {
      headers['Referer'] = 'https://www.instagram.com/';
      headers['Origin'] = 'https://www.instagram.com';
    } else if (parsedUrl.hostname.includes('fbcdn') || parsedUrl.hostname.includes('facebook')) {
      headers['Referer'] = 'https://www.facebook.com/';
      headers['Origin'] = 'https://www.facebook.com';
    } else if (parsedUrl.hostname.includes('rapidcdn') || parsedUrl.hostname.includes('snapsave')) {
      headers['Referer'] = 'https://snapsave.app/';
      headers['Origin'] = 'https://snapsave.app';
    } else if (parsedUrl.hostname.includes('googlevideo') || parsedUrl.hostname.includes('youtube')) {
      headers['Referer'] = 'https://www.youtube.com/';
    }

    const response = await fetch(mediaUrl, {
      headers,
      redirect: 'follow',
    });

    if (!response.ok || !response.body) {
      return NextResponse.json(
        { error: `Upstream media server returned error: ${response.status} ${response.statusText}` },
        { status: response.status || 502 }
      );
    }

    // Ensure proper media content type for media players
    let contentType = response.headers.get('content-type') || '';
    if (filename.endsWith('.mp4') || (!contentType || contentType === 'application/octet-stream')) {
      if (filename.endsWith('.mp4')) contentType = 'video/mp4';
      else if (filename.endsWith('.mp3')) contentType = 'audio/mpeg';
      else if (filename.endsWith('.jpg') || filename.endsWith('.jpeg')) contentType = 'image/jpeg';
      else if (filename.endsWith('.png')) contentType = 'image/png';
      else if (filename.endsWith('.pdf')) contentType = 'application/pdf';
      else contentType = 'video/mp4';
    }

    const contentLength = response.headers.get('content-length');

    const responseHeaders: Record<string, string> = {
      'Content-Type': contentType,
      'Content-Disposition': `attachment; filename="${filename}"`,
      'Cache-Control': 'public, max-age=3600',
    };

    if (contentLength) {
      responseHeaders['Content-Length'] = contentLength;
    }

    // Return piped readable stream
    return new NextResponse(response.body as unknown as ReadableStream, {
      status: 200,
      headers: responseHeaders,
    });
  } catch (error: any) {
    console.error('Proxy download error:', error);
    return NextResponse.json(
      { error: error?.message || 'Failed to stream media file' },
      { status: 500 }
    );
  }
}
