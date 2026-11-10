/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          bg: '#090D14',
          sub: '#0F1623',
          card: '#141D2B',
          surface: '#192536',
          border: '#263448',
          text: '#E8EDF5',
          muted: '#94A3B8',
          faint: '#64748B',
          accent: '#3B82F6',
          success: '#22C55E',
          error: '#EF4444',
        },
        light: {
          bg: '#F5F7FA',
          sub: '#FFFFFF',
          card: '#FFFFFF',
          surface: '#F1F5F9',
          border: '#E2E8F0',
          text: '#111827',
          muted: '#64748B',
          faint: '#94A3B8',
          accent: '#2563EB',
          success: '#16A34A',
          error: '#DC2626',
        },
      },
      borderRadius: {
        DEFAULT: '2px',
        sm: '2px',
        md: '4px',
        lg: '4px',
      },
      fontFamily: {
        sans: [
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          'Roboto',
          'sans-serif',
        ],
        mono: [
          'JetBrains Mono',
          'Fira Code',
          'Menlo',
          'Consolas',
          'monospace',
        ],
      },
    },
  },
  plugins: [],
};
