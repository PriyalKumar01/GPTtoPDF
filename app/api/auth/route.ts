import { NextRequest, NextResponse } from 'next/server';
import { saveOrUpdateUser } from '@/lib/users-db';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

// Configuration for OAuth Providers
const OAUTH_CONFIG = {
  google: {
    authUrl: 'https://accounts.google.com/o/oauth2/v2/auth',
    tokenUrl: 'https://oauth2.googleapis.com/token',
    userInfoUrl: 'https://www.googleapis.com/oauth2/v2/userinfo',
    scope: 'openid email profile',
    clientId: process.env.GOOGLE_CLIENT_ID || '',
    clientSecret: process.env.GOOGLE_CLIENT_SECRET || '',
  },
  github: {
    authUrl: 'https://github.com/login/oauth/authorize',
    tokenUrl: 'https://github.com/login/oauth/access_token',
    userInfoUrl: 'https://api.github.com/user',
    scope: 'read:user user:email',
    clientId: process.env.GITHUB_CLIENT_ID || '',
    clientSecret: process.env.GITHUB_CLIENT_SECRET || '',
  },
  facebook: {
    authUrl: 'https://www.facebook.com/v18.0/dialog/oauth',
    tokenUrl: 'https://graph.facebook.com/v18.0/oauth/access_token',
    userInfoUrl: 'https://graph.facebook.com/me?fields=id,name,email,picture.type(large)',
    scope: 'email,public_profile',
    clientId:
      process.env.FACEBOOK_CLIENT_ID ||
      process.env.FACEBOOK_APP_ID || '',
    clientSecret: process.env.FACEBOOK_CLIENT_SECRET || '',
  },
};

/**
 * GET /api/auth?provider=google|github|facebook&redirect_uri=...
 * Returns the OAuth authorization URL to initiate sign-in
 */
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const provider = (searchParams.get('provider') || 'google').toLowerCase() as keyof typeof OAUTH_CONFIG;
  const config = OAUTH_CONFIG[provider];

  if (!config) {
    return NextResponse.json({ error: 'Unsupported provider' }, { status: 400 });
  }

  const host =
    request.headers.get('x-forwarded-host') ||
    request.headers.get('host') ||
    'qwiksave.vercel.app';
  const protocol =
    request.headers.get('x-forwarded-proto') ||
    (host.includes('localhost') ? 'http' : 'https');

  const customRedirect = searchParams.get('redirect_uri');
  const callbackUrl = customRedirect || `${protocol}://${host}/auth/callback`;

  const params = new URLSearchParams({
    client_id: config.clientId,
    redirect_uri: callbackUrl,
    response_type: 'code',
    scope: config.scope,
    state: provider,
  });

  const authRedirectUrl = `${config.authUrl}?${params.toString()}`;

  return NextResponse.json({
    success: true,
    provider,
    authUrl: authRedirectUrl,
    callbackUrl,
    isConfigured: Boolean(config.clientId),
  });
}

/**
 * POST /api/auth
 * Exchanges authorization code for user info
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json().catch(() => ({}));
    const { provider, code, redirectUri } = body;

    const prov = (provider || 'google').toLowerCase() as keyof typeof OAUTH_CONFIG;
    const config = OAUTH_CONFIG[prov];

    if (!config || !code) {
      return NextResponse.json({ error: 'Missing code or provider' }, { status: 400 });
    }

    let userName = '';
    let userEmail = '';

    if (prov === 'google') {
      const tokenRes = await fetch(config.tokenUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          code,
          client_id: config.clientId,
          client_secret: config.clientSecret,
          redirect_uri: redirectUri,
          grant_type: 'authorization_code',
        }),
      });

      const tokenData = await tokenRes.json();
      if (!tokenData.access_token) {
        throw new Error(tokenData.error_description || tokenData.error || 'Failed to exchange Google token');
      }

      const userRes = await fetch(config.userInfoUrl, {
        headers: { Authorization: `Bearer ${tokenData.access_token}` },
      });
      const userData = await userRes.json();
      userName = userData.name || userData.given_name || 'Google User';
      userEmail = userData.email || '';
    } else if (prov === 'github') {
      const tokenRes = await fetch(config.tokenUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({
          client_id: config.clientId,
          client_secret: config.clientSecret,
          code,
          redirect_uri: redirectUri,
        }),
      });

      const tokenData = await tokenRes.json();
      if (!tokenData.access_token) {
        throw new Error(tokenData.error_description || tokenData.error || 'Failed to exchange GitHub token');
      }

      const userRes = await fetch(config.userInfoUrl, {
        headers: {
          Authorization: `Bearer ${tokenData.access_token}`,
          'User-Agent': 'QwikSave-App',
        },
      });
      const userData = await userRes.json();
      userName = userData.name || userData.login || 'GitHub User';

      // If email is private, fetch primary email
      userEmail = userData.email || '';
      if (!userEmail) {
        try {
          const emailsRes = await fetch('https://api.github.com/user/emails', {
            headers: {
              Authorization: `Bearer ${tokenData.access_token}`,
              'User-Agent': 'QwikSave-App',
            },
          });
          const emails = await emailsRes.json();
          if (Array.isArray(emails)) {
            const primary = emails.find((e: any) => e.primary) || emails[0];
            userEmail = primary?.email || `${userData.login}@users.noreply.github.com`;
          }
        } catch (e) {
          userEmail = `${userData.login}@users.noreply.github.com`;
        }
      }
    } else if (prov === 'facebook') {
      const tokenUrl = `${config.tokenUrl}?client_id=${config.clientId}&redirect_uri=${encodeURIComponent(
        redirectUri
      )}&client_secret=${config.clientSecret}&code=${code}`;

      const tokenRes = await fetch(tokenUrl);
      const tokenData = await tokenRes.json();
      if (!tokenData.access_token) {
        throw new Error(tokenData.error?.message || 'Failed to exchange Facebook token');
      }

      const userRes = await fetch(
        `https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token=${tokenData.access_token}`
      );
      const userData = await userRes.json();
      userName = userData.name || 'Facebook User';
      userEmail = userData.email || `${userData.id || 'meta'}@facebook.user`;
    }

    if (!userEmail) {
      userEmail = `${userName.toLowerCase().replace(/[^a-z0-9]/g, '')}@${prov}.auth`;
    }

    // Check if account is suspended / banned by Owner
    const { isUserBanned, getUserByEmail } = await import('@/lib/users-db');
    if (isUserBanned(userEmail)) {
      const bannedInfo = getUserByEmail(userEmail);
      return NextResponse.json(
        {
          success: false,
          isBanned: true,
          error: `Your account has been suspended by platform administration. Reason: ${
            bannedInfo?.banReason || 'Terms violation'
          }`,
        },
        { status: 403 }
      );
    }

    const customName = (body.name || '').trim();
    const parsedAge = body.age ? parseInt(body.age, 10) : undefined;
    const finalName = customName || userName;

    // Securely persist verified user in database (data/users.json & server records)
    const dbUser = saveOrUpdateUser({
      name: finalName,
      age: parsedAge && parsedAge > 0 && parsedAge <= 100 ? parsedAge : undefined,
      email: userEmail,
      provider: prov,
    });

    if (dbUser.isBanned) {
      return NextResponse.json(
        {
          success: false,
          isBanned: true,
          error: `Your account has been suspended by platform administration.`,
        },
        { status: 403 }
      );
    }

    const ageParam = dbUser.age ? `&age=${encodeURIComponent(dbUser.age)}` : '';
    const deepLinkUrl = `qwiksave://auth/callback?name=${encodeURIComponent(finalName)}&email=${encodeURIComponent(
      userEmail
    )}&provider=${prov}${ageParam}`;

    return NextResponse.json({
      success: true,
      provider: prov,
      user: {
        id: dbUser.id,
        name: finalName,
        age: dbUser.age,
        email: userEmail,
        verified: true,
      },
      deepLink: deepLinkUrl,
    });
  } catch (err: any) {
    console.error('OAuth token exchange error:', err);
    return NextResponse.json(
      { success: false, error: err.message || 'Authentication exchange failed' },
      { status: 422 }
    );
  }
}
