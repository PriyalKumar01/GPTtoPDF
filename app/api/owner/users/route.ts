import { NextRequest, NextResponse } from 'next/server';
import {
  readAllUsers,
  fetchUsersFromSupabase,
  banUser,
  deleteUser,
  getUserByEmail,
  OWNER_EMAIL,
} from '@/lib/users-db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const OWNER_SECRET_KEY =
  process.env.OWNER_SECRET_KEY || 'qwiksave-owner-2026';

function verifyOwnerAccess(request: NextRequest): boolean {
  const authHeader = request.headers.get('authorization') || '';
  const token = authHeader.replace(/^Bearer\s+/i, '').trim();
  const customHeader = request.headers.get('x-owner-key') || '';
  const queryKey = new URL(request.url).searchParams.get('key') || '';

  return (
    token === OWNER_SECRET_KEY ||
    customHeader === OWNER_SECRET_KEY ||
    queryKey === OWNER_SECRET_KEY
  );
}

/**
 * GET /api/owner/users
 * Returns all user records for the owner dashboard
 */
export async function GET(request: NextRequest) {
  if (!verifyOwnerAccess(request)) {
    return NextResponse.json(
      { success: false, error: 'Unauthorized: Invalid or missing Owner Secret Key.' },
      { status: 401 }
    );
  }

  // Fetch cloud users from Supabase if table is created; fallback to local records
  const supabaseUsers = await fetchUsersFromSupabase();
  const localUsers = readAllUsers();

  // Merge records prioritizing Supabase for cloud persistence
  const emailMap = new Map<string, typeof localUsers[0]>();
  for (const u of localUsers) {
    emailMap.set(u.email.toLowerCase().trim(), u);
  }
  for (const u of supabaseUsers) {
    emailMap.set(u.email.toLowerCase().trim(), u);
  }
  const users = Array.from(emailMap.values());

  const stats = {
    totalUsers: users.length,
    activeUsers: users.filter((u) => !u.isBanned).length,
    bannedUsers: users.filter((u) => Boolean(u.isBanned)).length,
    verifiedUsers: users.filter((u) => u.verified).length,
    providers: {
      google: users.filter((u) => u.provider === 'google').length,
      github: users.filter((u) => u.provider === 'github').length,
      facebook: users.filter((u) => u.provider === 'facebook').length,
    },
  };

  return NextResponse.json({
    success: true,
    stats,
    users,
  });
}

/**
 * POST /api/owner/users
 * Performs owner actions: ban, unban, delete
 */
export async function POST(request: NextRequest) {
  if (!verifyOwnerAccess(request)) {
    return NextResponse.json(
      { success: false, error: 'Unauthorized: Invalid or missing Owner Secret Key.' },
      { status: 401 }
    );
  }

  try {
    const body = await request.json().catch(() => ({}));
    const { action, email, reason } = body;

    if (!email || typeof email !== 'string') {
      return NextResponse.json(
        { success: false, error: 'User email is required.' },
        { status: 400 }
      );
    }

    const targetUser = getUserByEmail(email);
    if (!targetUser) {
      return NextResponse.json(
        { success: false, error: 'User does not exist in database.' },
        { status: 404 }
      );
    }

    if (action === 'ban') {
      const res = banUser(email, true, reason || 'Suspended by Owner');
      if (!res.success) {
        return NextResponse.json({ success: false, error: res.error }, { status: 400 });
      }
      return NextResponse.json({
        success: true,
        message: `User ${email} has been suspended/banned successfully.`,
        user: res.user,
      });
    }

    if (action === 'unban') {
      const res = banUser(email, false);
      if (!res.success) {
        return NextResponse.json({ success: false, error: res.error }, { status: 400 });
      }
      return NextResponse.json({
        success: true,
        message: `User ${email} has been reinstated/unbanned successfully.`,
        user: res.user,
      });
    }

    if (action === 'delete') {
      const res = deleteUser(email);
      if (!res.success) {
        return NextResponse.json({ success: false, error: res.error }, { status: 400 });
      }
      return NextResponse.json({
        success: true,
        message: `User ${email} permanently deleted from records.`,
      });
    }

    return NextResponse.json(
      { success: false, error: `Invalid action '${action}'. Allowed: ban, unban, delete.` },
      { status: 400 }
    );
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err.message || 'Owner action failed' },
      { status: 500 }
    );
  }
}
