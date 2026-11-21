import fs from 'fs';
import path from 'path';

export interface UserRecord {
  id: string;
  name: string;
  age?: number;
  email: string;
  provider: 'google' | 'github' | 'facebook' | string;
  verified: boolean;
  avatarUrl?: string;
  isBanned?: boolean;
  banReason?: string;
  bannedAt?: string;
  role?: 'user' | 'owner' | 'admin';
  createdAt: string;
  lastLoginAt: string;
}

const DB_DIR = path.join(process.cwd(), 'data');
const DB_FILE = path.join(DB_DIR, 'users.json');

// Master owner email
export const OWNER_EMAIL = 'priyalkumar01@gmail.com';

function getSupabaseConfig() {
  const url = process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://trfpjmtmsmzljcssvxnn.supabase.co';
  const key =
    process.env.SUPABASE_SERVICE_ROLE_KEY ||
    process.env.SUPABASE_ANON_KEY ||
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
    'sb_publishable_PcEuGfpHy_1Hn7Ou-juSeA_VtvTjvCv';
  return { url, key };
}

function ensureDbFile() {
  try {
    if (!fs.existsSync(DB_DIR)) {
      fs.mkdirSync(DB_DIR, { recursive: true });
    }
    if (!fs.existsSync(DB_FILE)) {
      // Seed with initial master record if empty
      const initialUsers: UserRecord[] = [
        {
          id: 'usr_owner_priyal',
          name: 'Priyal Kumar',
          age: 21,
          email: OWNER_EMAIL,
          provider: 'google',
          verified: true,
          role: 'owner',
          isBanned: false,
          createdAt: new Date().toISOString(),
          lastLoginAt: new Date().toISOString(),
        },
      ];
      fs.writeFileSync(DB_FILE, JSON.stringify(initialUsers, null, 2), 'utf-8');
    }
  } catch (err) {
    console.error('Error initializing user database directory:', err);
  }
}

export function readAllUsers(): UserRecord[] {
  ensureDbFile();
  try {
    const raw = fs.readFileSync(DB_FILE, 'utf-8');
    const users: UserRecord[] = JSON.parse(raw);
    // Ensure owner record exists
    if (!users.some((u) => u.email.toLowerCase() === OWNER_EMAIL.toLowerCase())) {
      users.unshift({
        id: 'usr_owner_priyal',
        name: 'Priyal Kumar',
        age: 21,
        email: OWNER_EMAIL,
        provider: 'google',
        verified: true,
        role: 'owner',
        isBanned: false,
        createdAt: new Date().toISOString(),
        lastLoginAt: new Date().toISOString(),
      });
      try {
        fs.writeFileSync(DB_FILE, JSON.stringify(users, null, 2), 'utf-8');
      } catch {}
    }
    return users;
  } catch (err) {
    console.error('Failed to read users database:', err);
    return [];
  }
}

/**
 * Fetch all users from Supabase REST endpoint
 */
export async function fetchUsersFromSupabase(): Promise<UserRecord[]> {
  const { url, key } = getSupabaseConfig();
  if (!url || !key) return [];

  try {
    const res = await fetch(`${url}/rest/v1/users?select=*&order=created_at.desc`, {
      headers: {
        apikey: key,
        Authorization: `Bearer ${key}`,
      },
      cache: 'no-store',
    });

    if (!res.ok) return [];

    const data = await res.json();
    if (!Array.isArray(data)) return [];

    return data.map((row: any) => ({
      id: row.id,
      name: row.name,
      age: row.age || undefined,
      email: row.email,
      provider: row.provider,
      verified: Boolean(row.verified),
      role: row.role || 'user',
      isBanned: Boolean(row.is_banned),
      banReason: row.ban_reason || undefined,
      createdAt: row.created_at,
      lastLoginAt: row.last_login_at,
    }));
  } catch (err) {
    return [];
  }
}

/**
 * Sync user to Supabase in the background
 */
export async function syncUserToSupabase(user: UserRecord): Promise<void> {
  const { url, key } = getSupabaseConfig();
  if (!url || !key) return;

  try {
    await fetch(`${url}/rest/v1/users`, {
      method: 'POST',
      headers: {
        apikey: key,
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
        Prefer: 'resolution=merge-duplicates',
      },
      body: JSON.stringify({
        id: user.id,
        name: user.name,
        age: user.age || null,
        email: user.email,
        provider: user.provider,
        verified: user.verified,
        role: user.role || 'user',
        is_banned: Boolean(user.isBanned),
        ban_reason: user.banReason || null,
        created_at: user.createdAt,
        last_login_at: user.lastLoginAt,
      }),
    });
  } catch (e) {
    console.warn('Supabase sync warning:', e);
  }
}

/**
 * Sync ban status to Supabase
 */
export async function syncBanToSupabase(
  email: string,
  isBanned: boolean,
  reason?: string
): Promise<void> {
  const { url, key } = getSupabaseConfig();
  if (!url || !key) return;

  try {
    await fetch(`${url}/rest/v1/users?email=eq.${encodeURIComponent(email)}`, {
      method: 'PATCH',
      headers: {
        apikey: key,
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        is_banned: isBanned,
        ban_reason: isBanned ? reason : null,
      }),
    });
  } catch (e) {
    console.warn('Supabase ban update warning:', e);
  }
}

/**
 * Delete user from Supabase
 */
export async function deleteUserFromSupabase(email: string): Promise<void> {
  const { url, key } = getSupabaseConfig();
  if (!url || !key) return;

  try {
    await fetch(`${url}/rest/v1/users?email=eq.${encodeURIComponent(email)}`, {
      method: 'DELETE',
      headers: {
        apikey: key,
        Authorization: `Bearer ${key}`,
      },
    });
  } catch (e) {
    console.warn('Supabase delete warning:', e);
  }
}

export function saveOrUpdateUser(user: {
  name: string;
  age?: number;
  email: string;
  provider: string;
  avatarUrl?: string;
}): UserRecord {
  ensureDbFile();
  const users = readAllUsers();
  const normalizedEmail = user.email.toLowerCase().trim();

  const existingIndex = users.findIndex(
    (u) => u.email.toLowerCase().trim() === normalizedEmail
  );

  const now = new Date().toISOString();
  const isOwner = normalizedEmail === OWNER_EMAIL.toLowerCase();

  let resultUser: UserRecord;

  if (existingIndex >= 0) {
    const existing = users[existingIndex];
    resultUser = {
      ...existing,
      name: user.name.trim() || existing.name,
      age: user.age && user.age > 0 ? user.age : existing.age,
      provider: user.provider || existing.provider,
      avatarUrl: user.avatarUrl || existing.avatarUrl,
      role: isOwner ? 'owner' : existing.role || 'user',
      isBanned: isOwner ? false : Boolean(existing.isBanned),
      lastLoginAt: now,
    };
    users[existingIndex] = resultUser;
  } else {
    resultUser = {
      id: isOwner
        ? 'usr_owner_priyal'
        : `usr_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
      name: user.name.trim() || normalizedEmail.split('@')[0],
      age: user.age && user.age > 0 ? user.age : undefined,
      email: normalizedEmail,
      provider: user.provider,
      verified: true,
      avatarUrl: user.avatarUrl,
      role: isOwner ? 'owner' : 'user',
      isBanned: false,
      createdAt: now,
      lastLoginAt: now,
    };
    users.push(resultUser);
  }

  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(users, null, 2), 'utf-8');
  } catch {}

  // Trigger non-blocking Supabase sync
  syncUserToSupabase(resultUser).catch(() => {});

  return resultUser;
}

export function getUserByEmail(email: string): UserRecord | null {
  const users = readAllUsers();
  const target = email.toLowerCase().trim();
  return users.find((u) => u.email.toLowerCase().trim() === target) || null;
}

export function isUserBanned(email: string): boolean {
  const user = getUserByEmail(email);
  if (!user) return false;
  if (user.email.toLowerCase() === OWNER_EMAIL.toLowerCase()) return false;
  return Boolean(user.isBanned);
}

export function banUser(
  email: string,
  ban: boolean,
  reason?: string
): { success: boolean; user?: UserRecord; error?: string } {
  ensureDbFile();
  const normalizedEmail = email.toLowerCase().trim();
  if (normalizedEmail === OWNER_EMAIL.toLowerCase()) {
    return { success: false, error: 'Cannot ban the platform owner.' };
  }

  const users = readAllUsers();
  const index = users.findIndex(
    (u) => u.email.toLowerCase().trim() === normalizedEmail
  );

  if (index === -1) {
    return { success: false, error: 'User not found.' };
  }

  users[index].isBanned = ban;
  if (ban) {
    users[index].banReason = reason || 'Suspended by platform administrator';
    users[index].bannedAt = new Date().toISOString();
  } else {
    delete users[index].banReason;
    delete users[index].bannedAt;
  }

  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(users, null, 2), 'utf-8');
    // Sync ban status to Supabase
    syncBanToSupabase(email, ban, reason).catch(() => {});
    return { success: true, user: users[index] };
  } catch (err: any) {
    return { success: false, error: err.message || 'Failed to update user status.' };
  }
}

export function deleteUser(email: string): { success: boolean; error?: string } {
  ensureDbFile();
  const normalizedEmail = email.toLowerCase().trim();
  if (normalizedEmail === OWNER_EMAIL.toLowerCase()) {
    return { success: false, error: 'Cannot delete the platform owner.' };
  }

  const users = readAllUsers();
  const initialLength = users.length;
  const filtered = users.filter(
    (u) => u.email.toLowerCase().trim() !== normalizedEmail
  );

  if (filtered.length === initialLength) {
    return { success: false, error: 'User not found.' };
  }

  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(filtered, null, 2), 'utf-8');
    // Delete from Supabase
    deleteUserFromSupabase(email).catch(() => {});
    return { success: true };
  } catch (err: any) {
    return { success: false, error: err.message || 'Failed to delete user.' };
  }
}
