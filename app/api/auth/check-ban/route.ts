import { NextRequest, NextResponse } from 'next/server';
import { getUserByEmail, isUserBanned, fetchUsersFromSupabase } from '@/lib/users-db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const email = (searchParams.get('email') || '').trim().toLowerCase();

  if (!email) {
    return NextResponse.json(
      { isBanned: false, error: 'Email parameter required' },
      { status: 400, headers: { 'Cache-Control': 'no-store, must-revalidate' } }
    );
  }

  // First check Supabase live for real-time accuracy
  try {
    const supabaseUsers = await fetchUsersFromSupabase();
    const suUser = supabaseUsers.find((u) => u.email.toLowerCase() === email);
    if (suUser) {
      return NextResponse.json(
        {
          isBanned: Boolean(suUser.isBanned),
          reason:
            suUser.banReason ||
            'Violation of Qwik Save Terms & Conditions (such as providing false details, unauthorized automated scraping, or policy breaches).',
          email: suUser.email,
        },
        { headers: { 'Cache-Control': 'no-store, must-revalidate' } }
      );
    }
  } catch (err) {
    // Fall through to local cache
  }

  const localUser = getUserByEmail(email);
  if (localUser) {
    return NextResponse.json(
      {
        isBanned: Boolean(localUser.isBanned),
        reason:
          localUser.banReason ||
          'Violation of Qwik Save Terms & Conditions (such as providing false details, unauthorized automated scraping, or policy breaches).',
        email: localUser.email,
      },
      { headers: { 'Cache-Control': 'no-store, must-revalidate' } }
    );
  }

  return NextResponse.json(
    { isBanned: false, notFound: true, email },
    { headers: { 'Cache-Control': 'no-store, must-revalidate' } }
  );
}
