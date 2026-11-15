# Design System & UI Engineering Guidelines

This document outlines the strict design rules for this application. All pages, components, and tools **MUST** strictly adhere to these enterprise SaaS standards (modeled after Linear, Vercel, Stripe, and GitHub).

---

## 1. Geometric Shapes & Border Radii
- **Strict Rule:** NEVER use `rounded-xl`, `rounded-2xl`, `rounded-3xl`, or large pill shapes for cards or inputs. That produces an informal "bubbly" toy-like aesthetic.
- **Permitted Radii:**
  - Micro elements (badges, small icons, tags): `rounded-[2px]` or `rounded-sm` (2px - 3px max).
  - Inputs & Action Buttons: `rounded-[2px]` or `rounded-[3px]` (clean, sharp, modern).
  - Cards & Containers: `rounded-[2px]` or `rounded-[3px]` (max 4px).
  - Avatars / Circular indicator dots only: `rounded-full`.

---

## 2. Color Palette & Dark Mode Scheme
- **Dark Mode Backgrounds:**
  - Page Background: `#090D14` (Deep obsidian navy)
  - Card / Panel Background: `#111A29` or `#141D2B` (Clean dark slate navy)
  - Input / Inset Background: `#0F1623`
  - Active / Hover State: `#192536`
- **Light Mode Backgrounds:**
  - Page Background: `bg-slate-50` or `#F8FAFC`
  - Card / Panel: `bg-white`
  - Input / Inset: `bg-slate-50` or `bg-slate-100`
- **Borders & Dividers:**
  - Light mode: `border-slate-200` / `border-slate-300`
  - Dark mode: `border-[#263448]` (Crisp, high-contrast, professional)
- **Primary Brand Accents:**
  - Primary Action Blue: `#3B82F6` (Hover: `#2563EB`)
  - Success: `#22C55E`
  - Danger: `#EF4444`
  - Secondary / Text: `text-slate-600 dark:text-[#94A3B8]`
  - Headings: `text-slate-900 dark:text-[#E8EDF5]`

---

## 3. Typography & Spacing
- **Font Stack:** Modern Inter / sans-serif with monospace accents for IDs and numbers.
- **Headings:** Semibold to Bold (font-semibold, tracking-tight). Avoid gigantic cartoonish titles; use clean, structured, authoritative hierarchy (`text-2xl sm:text-4xl`).
- **Labels & Badges:** `text-[10px]` or `text-[11px]`, uppercase, tracking-wider, `font-bold` with border.
- **Layout Width:** Full-screen responsive layout (`max-w-7xl mx-auto px-4 sm:px-6 lg:px-8` or full-bleed `w-full px-4 sm:px-8`). Content should never look cramped or trapped in a narrow vertical strip.

---

## 4. Navigation Architecture
- **Desktop Navbar:** Maximum 3-4 top-level items to prevent clutter.
- **Dropdown Menus:** Sharp `rounded-[2px]` dropdowns with high z-index, subtle borders (`border-slate-200 dark:border-[#263448]`), and crisp hover effects.
- **Mobile Navbar:** Clean vertical drawer with grouped categories.
