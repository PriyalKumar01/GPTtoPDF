import { jsPDF } from 'jspdf';
import fs from 'fs';
import { COLLEGE_STUDY_LOGO_B64 } from '../lib/college-study-logo-b64';
import { APP_CONFIG } from '../lib/config';

function testBrandingPdf() {
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
  let currentY = marginTop;

  // 1. Page Border (if enabled)
  const borderInset = 7;
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.25);
  doc.roundedRect(
    borderInset,
    borderInset,
    pageWidth - borderInset * 2,
    pageHeight - borderInset * 2,
    1.5,
    1.5,
    'D'
  );

  // 2. Top-Right Partner Badge (College Study)
  const badgeW = 42;
  const badgeH = 14;
  const badgeX = pageWidth - marginX - badgeW;
  const badgeY = marginTop - 1;

  // Badge background card
  doc.setFillColor(248, 250, 252);
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.2);
  doc.roundedRect(badgeX, badgeY, badgeW, badgeH, 1.2, 1.2, 'FD');

  // Embed Logo inside badge
  const logoSize = 11;
  doc.addImage(COLLEGE_STUDY_LOGO_B64, 'JPEG', badgeX + 1.5, badgeY + 1.5, logoSize, logoSize);

  // Text inside badge
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(5.2);
  doc.setTextColor(100, 116, 139);
  doc.text('POWERED BY', badgeX + 14, badgeY + 4.5);

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(7.2);
  doc.setTextColor(15, 23, 42);
  doc.text('College Study', badgeX + 14, badgeY + 8.2);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(5.5);
  doc.setTextColor(2, 132, 199);
  doc.textWithLink('Your Study Hub ↗', badgeX + 14, badgeY + 11.5, {
    url: 'https://collegestudy.in',
  });

  // 3. Document Title on Left
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14.5);
  doc.setTextColor(15, 23, 42);
  const title = 'Software Testing Preparation';
  const titleLines = doc.splitTextToSize(title, contentWidth - badgeW - 6);
  doc.text(titleLines, marginX, currentY + 3);
  currentY += titleLines.length * 5 + 1.5;

  // Metadata / Date
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

  // Dummy Content
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.setTextColor(30, 41, 59);
  doc.text('Unit 3 & 4 Exam Notes Sample text paragraph demonstrating compact layout...', marginX, currentY);

  // 4. Footer
  const footY = pageHeight - 11;
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.2);
  doc.line(marginX, footY, pageWidth - marginX, footY);

  // Line 1: PDF Generator ↗ (Light Green highlight)
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(7.5);
  doc.setTextColor(16, 185, 129); // Light green / emerald
  doc.textWithLink('PDF Generator ↗', marginX, footY + 4, {
    url: 'https://gpttopdf.vercel.app',
  });

  // Line 2: Made By: Priyal Kumar Singh (Light Blue highlight)
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(6.8);
  doc.setTextColor(2, 132, 199); // Sky / Light blue
  doc.textWithLink('Made By: Priyal Kumar Singh', marginX, footY + 7.5, {
    url: 'https://www.linkedin.com/in/priyal-kumar-29b26529a/',
  });

  // Right Side
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  const pageStr = 'Page 01 of 27';
  const pWidth = doc.getTextWidth(pageStr);
  doc.text(pageStr, pageWidth - marginX - pWidth, footY + 4);

  const siteHost = 'gpttopdf.vercel.app';
  const sWidth = doc.getTextWidth(siteHost);
  doc.text(siteHost, pageWidth - marginX - sWidth, footY + 7.5);

  const out = doc.output('arraybuffer');
  fs.writeFileSync('scratch/branding_test.pdf', Buffer.from(out));
  console.log('Saved scratch/branding_test.pdf, size:', out.byteLength, 'bytes');
}

testBrandingPdf();
