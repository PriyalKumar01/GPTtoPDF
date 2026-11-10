import { jsPDF } from 'jspdf';
import fs from 'fs';
import { COLLEGE_STUDY_LOGO_B64 } from '../lib/college-study-logo-b64';

function testShadowBorderAndPills() {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const pageWidth = 210;
  const pageHeight = 297;
  const marginX = 14;
  const marginTop = 14;
  const contentWidth = pageWidth - marginX * 2;

  // 1. MS Word Classic "Shadow" Page Border
  const bInset = 7;
  const bX = bInset;
  const bY = bInset;
  const bW = pageWidth - bInset * 2; // 196mm
  const bH = pageHeight - bInset * 2; // 283mm
  const shadowThick = 1.0; // 1mm shadow thickness

  // Drop Shadow on Right and Bottom (MS Word style)
  doc.setFillColor(15, 23, 42); // Dark slate / black shadow
  // Right Shadow
  doc.rect(bX + bW, bY + shadowThick, shadowThick, bH, 'F');
  // Bottom Shadow
  doc.rect(bX + shadowThick, bY + bH, bW, shadowThick, 'F');

  // Main Border Box (Dark crisp frame)
  doc.setDrawColor(15, 23, 42);
  doc.setLineWidth(0.4);
  doc.rect(bX, bY, bW, bH, 'D');

  // 2. Top-Right Partner Badge (College Study)
  const badgeW = 42;
  const badgeH = 13.5;
  const badgeX = pageWidth - marginX - badgeW;
  const badgeY = marginTop - 1;

  doc.setFillColor(248, 250, 252);
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.2);
  doc.roundedRect(badgeX, badgeY, badgeW, badgeH, 1, 1, 'FD');

  const logoSize = 10.5;
  doc.addImage(COLLEGE_STUDY_LOGO_B64, 'JPEG', badgeX + 1.5, badgeY + 1.5, logoSize, logoSize);

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(5.2);
  doc.setTextColor(100, 116, 139);
  doc.text('POWERED BY', badgeX + 13.5, badgeY + 4.2);

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(7.2);
  doc.setTextColor(15, 23, 42);
  doc.text('College Study', badgeX + 13.5, badgeY + 7.8);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(5.8);
  doc.setTextColor(2, 132, 199);
  doc.textWithLink('Your Study Hub', badgeX + 13.5, badgeY + 11.2, {
    url: 'https://college-study.netlify.app/',
  });

  // 3. Document Title on Left
  let currentY = marginTop;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14);
  doc.setTextColor(15, 23, 42);
  const title = 'Software Testing Preparation';
  const titleLines = doc.splitTextToSize(title, contentWidth - badgeW - 6);
  doc.text(titleLines, marginX, currentY + 3);
  currentY += titleLines.length * 5 + 1.5;

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  doc.text('Generated: 2026-11-10 • Typeset for Academic Review', marginX, currentY + 3);
  currentY = Math.max(currentY + 6, badgeY + badgeH + 3);

  // Header Divider
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.2);
  doc.line(marginX, currentY, marginX + contentWidth, currentY);
  currentY += 6;

  // Content Sample
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.setTextColor(30, 41, 59);
  doc.text('Exam study notes body text demonstrating spacing and layout within the MS Word shadow border...', marginX, currentY);

  // 4. Footer with Highlighted Pill Containers
  const footY = 275; // Divider well above bottom border (at 290mm)
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.2);
  doc.line(marginX, footY, pageWidth - marginX, footY);

  // Pill 1: "PDF Generator" (Light green highlight pill, no arrows/punctuation)
  const pill1X = marginX;
  const pill1Y = 277;
  const p1Text = 'PDF Generator';
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(6.8);
  const p1W = doc.getTextWidth(p1Text) + 5;
  const p1H = 4.2;

  // Green surrounding pill
  doc.setFillColor(220, 252, 231); // emerald-100 (#DCFCE7)
  doc.setDrawColor(134, 239, 172); // emerald-300 (#86EFAC)
  doc.setLineWidth(0.2);
  doc.roundedRect(pill1X, pill1Y, p1W, p1H, 1, 1, 'FD');

  doc.setTextColor(21, 128, 61); // emerald-700 (#15803D)
  doc.textWithLink(p1Text, pill1X + 2.5, pill1Y + 3.0, {
    url: 'https://gpttopdf.vercel.app',
  });

  // Pill 2: "Made By: Priyal Kumar Singh" (Light blue highlight pill)
  const pill2X = marginX;
  const pill2Y = 282.2;
  const p2Text = 'Made By: Priyal Kumar Singh';
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(6.8);
  const p2W = doc.getTextWidth(p2Text) + 5;
  const p2H = 4.2;

  // Blue surrounding pill
  doc.setFillColor(224, 242, 254); // sky-100 (#E0F2FE)
  doc.setDrawColor(186, 230, 253); // sky-300 (#BAE6FD)
  doc.setLineWidth(0.2);
  doc.roundedRect(pill2X, pill2Y, p2W, p2H, 1, 1, 'FD');

  doc.setTextColor(3, 105, 161); // sky-700 (#0369A1)
  doc.textWithLink(p2Text, pill2X + 2.5, pill2Y + 3.0, {
    url: 'https://www.linkedin.com/in/priyal-kumar-29b26529a/',
  });

  // Right Side: Page X of Y & Website Domain (Classic clean slate)
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  const pageStr = 'Page 01 of 27';
  const pWidth = doc.getTextWidth(pageStr);
  doc.text(pageStr, pageWidth - marginX - pWidth, 279.5);

  const siteHost = 'gpttopdf.vercel.app';
  const sWidth = doc.getTextWidth(siteHost);
  doc.text(siteHost, pageWidth - marginX - sWidth, 284.5);

  const out = doc.output('arraybuffer');
  fs.writeFileSync('scratch/shadow_border_test.pdf', Buffer.from(out));
  console.log('Saved scratch/shadow_border_test.pdf, size:', out.byteLength, 'bytes');
}

testShadowBorderAndPills();
