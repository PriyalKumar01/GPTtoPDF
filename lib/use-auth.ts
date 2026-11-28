'use client';

import { useState, useEffect, useCallback } from 'react';

export interface UserSession {
  id: string;
  name: string;
  email: string;
  provider: string;
  age?: number;
}

export function useAuth() {
  const [user, setUser] = useState<UserSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);

  const refreshUser = useCallback(() => {
    if (typeof window === 'undefined') return;
    const email = localStorage.getItem('qs_user_email') || localStorage.getItem('user_email');
    const name = localStorage.getItem('qs_user_name') || localStorage.getItem('user_name');
    const provider = localStorage.getItem('qs_user_provider') || 'web';
    const id = localStorage.getItem('qs_user_id') || '';
    const ageStr = localStorage.getItem('qs_user_age');
    const age = ageStr ? parseInt(ageStr, 10) : undefined;

    if (email && name) {
      setUser({ id, name, email, provider, age });
    } else {
      setUser(null);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    refreshUser();

    const handleAuthChange = () => {
      refreshUser();
    };

    window.addEventListener('qs_auth_changed', handleAuthChange);
    window.addEventListener('storage', handleAuthChange);

    return () => {
      window.removeEventListener('qs_auth_changed', handleAuthChange);
      window.removeEventListener('storage', handleAuthChange);
    };
  }, [refreshUser]);

  const signOut = useCallback(() => {
    if (typeof window === 'undefined') return;
    localStorage.removeItem('qs_user_id');
    localStorage.removeItem('qs_user_name');
    localStorage.removeItem('qs_user_email');
    localStorage.removeItem('qs_user_provider');
    localStorage.removeItem('qs_user_age');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_name');
    sessionStorage.clear();
    setUser(null);
    window.dispatchEvent(new Event('qs_auth_changed'));
  }, []);

  const openAuthModal = useCallback((callback?: () => void) => {
    if (callback) {
      setPendingAction(() => callback);
    } else {
      setPendingAction(null);
    }
    setIsAuthModalOpen(true);
  }, []);

  const closeAuthModal = useCallback(() => {
    setIsAuthModalOpen(false);
    setPendingAction(null);
  }, []);

  const requireAuth = useCallback(
    (action: () => void) => {
      const email = typeof window !== 'undefined' ? (localStorage.getItem('qs_user_email') || localStorage.getItem('user_email')) : null;
      if (email) {
        action();
      } else {
        openAuthModal(action);
      }
    },
    [openAuthModal]
  );

  const handleAuthSuccess = useCallback(
    (newUser: UserSession) => {
      setUser(newUser);
      setIsAuthModalOpen(false);
      if (pendingAction) {
        pendingAction();
        setPendingAction(null);
      }
    },
    [pendingAction]
  );

  return {
    user,
    loading,
    isAuthenticated: Boolean(user?.email),
    isAuthModalOpen,
    openAuthModal,
    closeAuthModal,
    requireAuth,
    signOut,
    handleAuthSuccess,
  };
}
