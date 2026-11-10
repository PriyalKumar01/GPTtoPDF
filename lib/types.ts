export type MessageRole = 'user' | 'assistant' | 'system';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  authorName: string; // 'You' | 'ChatGPT'
  content: string; // Markdown formatted content
  timestamp?: string | number;
}

export interface ConversationData {
  id: string;
  title: string;
  sourceUrl?: string;
  createdAt?: string;
  messages: ChatMessage[];
  stats: {
    userMessages: number;
    assistantMessages: number;
    totalMessages: number;
    estimatedPages: number;
  };
}

export interface PdfOptions {
  paperSize: 'A4' | 'Letter';
  fontSize: 'small' | 'medium' | 'large';
  fontFamily: 'Helvetica' | 'Times' | 'Courier';
  borderStyle: 'shadow' | 'clean' | 'none';
  partnerBadgeScope: 'all-pages' | 'first-page';
  includeTitle: boolean;
  includeDate: boolean;
  includePageNumbers: boolean;
  includeUserMessages: boolean;
  includeAssistantMessages: boolean;
  includeCodeBlocks: boolean;
  includeImages: boolean;
}

export type ExtractionProgressStep =
  | 'idle'
  | 'validating'
  | 'fetching'
  | 'extracting'
  | 'preparing'
  | 'ready'
  | 'error';

export interface ExtractionProgress {
  step: ExtractionProgressStep;
  message: string;
  percent: number;
}

export interface ApiExtractResponse {
  success: boolean;
  conversation?: ConversationData;
  error?: string;
  errorCode?:
    | 'INVALID_URL'
    | 'UNSUPPORTED_DOMAIN'
    | 'SSRF_BLOCKED'
    | 'NOT_FOUND'
    | 'BOT_PROTECTED'
    | 'EMPTY_CONVERSATION'
    | 'INTERNAL_ERROR';
  details?: string;
}
