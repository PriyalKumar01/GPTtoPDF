/**
 * Email validation utility to block disposable, temporary, and fake emails
 * while welcoming verified @gmail.com, educational (.edu, .ac.in, etc.), and legitimate corporate emails.
 */

// Comprehensive list of disposable/temporary email domains
const DISPOSABLE_DOMAINS = new Set([
  '10minutemail.com',
  '10minutemail.net',
  '10mail.org',
  '20minutemail.com',
  'airmail.news',
  'anonaddy.me',
  'armyspy.com',
  'binkmail.com',
  'bobmail.info',
  'burnermail.io',
  'chacuo.net',
  'clipmail.eu',
  'crazymailing.com',
  'cuvox.de',
  'dayrep.com',
  'dispostable.com',
  'dropmail.me',
  'emailondeck.com',
  'emailfake.com',
  'fakeinbox.com',
  'fakemailgenerator.com',
  'fleckens.hu',
  'generator.email',
  'getairmail.com',
  'getnada.com',
  'guerrillamail.biz',
  'guerrillamail.com',
  'guerrillamail.de',
  'guerrillamail.info',
  'guerrillamail.net',
  'guerrillamail.org',
  'gustr.com',
  'inboxkitten.com',
  'incognitoid.com',
  'internxt.com',
  'jourrapide.com',
  'maildrop.cc',
  'mailcatch.com',
  'mailinator.com',
  'mailnesia.com',
  'minutemailbox.com',
  'mohmal.com',
  'mytemp.email',
  'nada.ltd',
  'nightlymail.com',
  'oneoffmail.com',
  'owlpic.com',
  'rhyta.com',
  'sharklasers.com',
  'superrito.com',
  'teleworm.us',
  'temp-mail.org',
  'tempmail.com',
  'tempmail.net',
  'tempmailo.com',
  'temporary-mail.net',
  'throwawaymail.com',
  'tinypm.com',
  'trashmail.com',
  'trashmail.net',
  'trashmail.org',
  'trbvm.com',
  'vmani.com',
  'yopmail.com',
  'yopmail.net',
  'yopmail.fr',
  'zippymail.info',
]);

const DISPOSABLE_KEYWORDS = [
  'tempmail',
  'temp-mail',
  'disposable',
  'fakeinbox',
  'throwaway',
  'trashmail',
  '10minute',
  'guerrillamail',
  'fakemail',
  'temporarymail',
  'dropmail',
  'mohmal',
  'sharklaser',
];

export interface EmailValidationResult {
  valid: boolean;
  reason?: string;
  isEducational?: boolean;
}

/**
 * Validates an email address ensuring it is a legitimate address and NOT disposable/temporary.
 */
export function validateUserEmail(rawEmail: string): EmailValidationResult {
  if (!rawEmail || typeof rawEmail !== 'string') {
    return { valid: false, reason: 'Please enter a valid email address.' };
  }

  const email = rawEmail.trim().toLowerCase();

  // Basic regex check
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(email)) {
    return { valid: false, reason: 'Invalid email address format.' };
  }

  const parts = email.split('@');
  if (parts.length !== 2) {
    return { valid: false, reason: 'Invalid email structure.' };
  }

  const [localPart, domain] = parts;

  if (localPart.length < 2) {
    return { valid: false, reason: 'Email username is too short.' };
  }

  // Check educational domain
  const isEducational =
    domain.endsWith('.edu') ||
    domain.endsWith('.ac.in') ||
    domain.endsWith('.edu.in') ||
    domain.endsWith('.ac.uk') ||
    domain.endsWith('.edu.au') ||
    domain.endsWith('.res.in') ||
    domain.endsWith('.school') ||
    domain.endsWith('.college') ||
    domain.includes('.edu.');

  if (isEducational) {
    return { valid: true, isEducational: true };
  }

  // Check known disposable domain match
  if (DISPOSABLE_DOMAINS.has(domain)) {
    return {
      valid: false,
      reason:
        'Temporary/disposable email addresses are strictly prohibited. Please use your official @gmail.com or college/university email.',
    };
  }

  // Check disposable keywords in domain
  for (const kw of DISPOSABLE_KEYWORDS) {
    if (domain.includes(kw)) {
      return {
        valid: false,
        reason:
          'Temporary/disposable email addresses are strictly prohibited. Please use your official @gmail.com or college/university email.',
      };
    }
  }

  return { valid: true, isEducational: false };
}
