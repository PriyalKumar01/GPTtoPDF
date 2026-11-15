import { NextRequest, NextResponse } from 'next/server';
import { extractFacebookMedia } from '@/lib/extract-facebook';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json().catch(() => ({}));
    const { url } = body;

    if (!url || typeof url !== 'string' || !url.trim()) {
      return NextResponse.json(
        { error: 'Please provide a valid Facebook URL' },
        { status: 400 }
      );
    }

    const trimmedUrl = url.trim();

    if (
      !trimmedUrl.includes('facebook.com') &&
      !trimmedUrl.includes('fb.watch') &&
      !trimmedUrl.includes('fb.gg')
    ) {
      return NextResponse.json(
        { error: 'The link must be from facebook.com or fb.watch' },
        { status: 400 }
      );
    }

    const result = await extractFacebookMedia(trimmedUrl);

    return NextResponse.json({
      success: true,
      data: result,
    });
  } catch (error: any) {
    console.error('Facebook extraction error:', error);
    return NextResponse.json(
      {
        success: false,
        error:
          error?.message ||
          'Failed to extract Facebook video. Please verify the video is public and try again.',
      },
      { status: 422 }
    );
  }
}
