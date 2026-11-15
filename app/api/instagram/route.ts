import { NextRequest, NextResponse } from 'next/server';
import { extractInstagramMedia } from '@/lib/extract-instagram';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json().catch(() => ({}));
    const { url } = body;

    if (!url || typeof url !== 'string' || !url.trim()) {
      return NextResponse.json(
        { error: 'Please provide a valid Instagram URL' },
        { status: 400 }
      );
    }

    const trimmedUrl = url.trim();

    if (!trimmedUrl.includes('instagram.com') && !trimmedUrl.includes('instagr.am')) {
      return NextResponse.json(
        { error: 'The link must be from instagram.com' },
        { status: 400 }
      );
    }

    const result = await extractInstagramMedia(trimmedUrl);

    return NextResponse.json({
      success: true,
      data: result,
    });
  } catch (error: any) {
    console.error('Instagram extraction error:', error);
    return NextResponse.json(
      {
        success: false,
        error:
          error?.message ||
          'Failed to extract Instagram media. Please ensure the post is public and try again.',
      },
      { status: 422 }
    );
  }
}
