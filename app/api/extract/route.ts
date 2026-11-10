import { NextRequest, NextResponse } from 'next/server';
import { extractAiConversation, isValidAiShareUrl } from '@/lib/extract-ai';
import { ApiExtractResponse } from '@/lib/types';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(req: NextRequest): Promise<NextResponse<ApiExtractResponse>> {
  try {
    const body = await req.json();
    const { url } = body;

    if (!url || typeof url !== 'string') {
      return NextResponse.json(
        {
          success: false,
          error: 'Please provide a shared AI conversation link (ChatGPT, Claude, Gemini, etc.).',
          errorCode: 'INVALID_URL',
        },
        { status: 400 }
      );
    }

    const trimmedUrl = url.trim();

    if (!isValidAiShareUrl(trimmedUrl)) {
      return NextResponse.json(
        {
          success: false,
          error:
            'Invalid or unsupported link. Please provide a public share link from ChatGPT, Claude, Google Gemini, or Perplexity.',
          errorCode: 'INVALID_URL',
        },
        { status: 400 }
      );
    }

    const result = await extractAiConversation(trimmedUrl);

    if (!result.success) {
      const statusCode =
        result.errorCode === 'INVALID_URL'
          ? 400
          : result.errorCode === 'NOT_FOUND'
          ? 404
          : result.errorCode === 'BOT_PROTECTED'
          ? 403
          : 500;

      return NextResponse.json(result, { status: statusCode });
    }

    return NextResponse.json(result, { status: 200 });
  } catch (error: any) {
    return NextResponse.json(
      {
        success: false,
        error: error.message || 'An unexpected error occurred during extraction.',
        errorCode: 'INTERNAL_ERROR',
      },
      { status: 500 }
    );
  }
}
