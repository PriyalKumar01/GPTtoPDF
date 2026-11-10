import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import fs from 'fs';

async function testDensePdf() {
  const shareId = '6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const url = `https://chatgpt.com/backend-api/share/${shareId}`;
  const res = await fetch(url, {
    headers: {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
      Accept: 'application/json',
    },
  });
  const data = await res.json();

  // Extract clean user prompt and clean assistant text
  const userItem = data.linear_conversation[1];
  const userPrompt = userItem.message.content.parts
    .filter(p => typeof p === 'string')
    .join(' ')
    .trim();

  const assistantItem = data.linear_conversation[20];
  const assistantRaw = assistantItem.message.content.parts[0];

  // Clean XML & DIL markup
  const assistantText = assistantRaw
    .replace(/<badge[^>]*>(.*?)<\/badge>/gi, '($1) ')
    .replace(/\{@body[\s\S]*?\}/g, '')
    .replace(/<\/?(?:grid|grid-item|AsyncImage|box|row|badge|checkbox|button)[^>]*>/gi, '')
    .replace(/\r\n/g, '\n')
    .trim();

  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const marginX = 14;
  const marginTop = 15;
  const marginBottom = 15;
  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  const contentWidth = pageWidth - marginX * 2;
  let currentY = marginTop;

  function checkBreak(needed) {
    if (currentY + needed > pageHeight - marginBottom) {
      doc.addPage();
      currentY = marginTop;
    }
  }

  // 1. Header
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14.5);
  doc.setTextColor(15, 23, 42); // slate 900
  doc.text(data.title || 'Software Testing Preparation', marginX, currentY);
  currentY += 4.5;

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139); // slate 500
  doc.text(`Generated: ${new Date().toISOString()} • Source: Shared ChatGPT link`, marginX, currentY);
  currentY += 4;

  // Divider
  doc.setDrawColor(226, 232, 240);
  doc.setLineWidth(0.2);
  doc.line(marginX, currentY, marginX + contentWidth, currentY);
  currentY += 4.5;

  // 2. User Prompt Box (compact card matching screenshot)
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(6.5);
  doc.setTextColor(100, 116, 139); // USER badge
  
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  const userLines = doc.splitTextToSize(userPrompt, contentWidth - 8);
  const userBoxHeight = userLines.length * 3.7 + 9;

  checkBreak(userBoxHeight + 3);

  // Background card
  doc.setFillColor(248, 250, 252); // slate 50
  doc.setDrawColor(226, 232, 240); // slate 200
  doc.setLineWidth(0.2);
  doc.roundedRect(marginX, currentY, contentWidth, userBoxHeight, 1.5, 1.5, 'FD');

  // USER label
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(6.5);
  doc.setTextColor(100, 116, 139);
  doc.text('USER', marginX + 4, currentY + 4.5);

  // User text
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.setTextColor(30, 41, 59);
  doc.text(userLines, marginX + 4, currentY + 8);

  currentY += userBoxHeight + 5;

  // 3. Assistant Section
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(6.5);
  doc.setTextColor(100, 116, 139);
  doc.text('ASSISTANT', marginX, currentY);
  currentY += 4;

  // Parse lines of assistant notes
  const lines = assistantText.split('\n');
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Markdown Table
    if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
      const tableLines = [];
      while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
        tableLines.push(lines[i]);
        i++;
      }
      if (tableLines.length >= 2) {
        checkBreak(18);
        const parseRow = (l) =>
          l.replace(/^\|/, '').replace(/\|$/, '').split('|').map(c => c.trim().replace(/\*\*/g, ''));
        const head = [parseRow(tableLines[0])];
        const body = tableLines.slice(2).map(parseRow).filter(r => r.some(c => c.length > 0));

        autoTable(doc, {
          head,
          body,
          startY: currentY,
          margin: { left: marginX, right: marginX, bottom: marginBottom },
          theme: 'grid',
          headStyles: {
            fillColor: [241, 245, 249],
            textColor: [15, 23, 42],
            fontStyle: 'bold',
            fontSize: 8,
            cellPadding: 1.8,
          },
          bodyStyles: {
            textColor: [30, 41, 59],
            fontSize: 8,
            cellPadding: 1.6,
          },
          alternateRowStyles: {
            fillColor: [255, 255, 255],
          },
          tableLineColor: [226, 232, 240],
          tableLineWidth: 0.15,
        });

        const finalY = doc.lastAutoTable?.finalY;
        if (finalY) currentY = finalY + 4;
      }
      continue;
    }

    // Code block
    if (line.trim().startsWith('```')) {
      const codeLines = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // skip closing ```
      
      const codeText = codeLines.join('\n');
      doc.setFont('courier', 'normal');
      doc.setFontSize(8);
      const splitCode = doc.splitTextToSize(codeText, contentWidth - 6);
      const boxH = splitCode.length * 3.4 + 4;

      checkBreak(Math.min(boxH, 30));
      doc.setFillColor(248, 250, 252);
      doc.setDrawColor(226, 232, 240);
      doc.roundedRect(marginX, currentY, contentWidth, boxH, 1, 1, 'FD');

      doc.setTextColor(30, 41, 59);
      doc.text(splitCode, marginX + 3, currentY + 3.5);
      currentY += boxH + 3.5;
      continue;
    }

    // Headings
    if (line.startsWith('# ')) {
      checkBreak(12);
      currentY += 2;
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(13);
      doc.setTextColor(15, 23, 42);
      const cleanH1 = line.slice(2).trim().replace(/\*\*/g, '');
      const hLines = doc.splitTextToSize(cleanH1, contentWidth);
      doc.text(hLines, marginX, currentY);
      currentY += hLines.length * 4.8 + 2.5;
      i++;
      continue;
    }

    if (line.startsWith('## ')) {
      checkBreak(10);
      currentY += 2;
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(11);
      doc.setTextColor(15, 23, 42);
      const cleanH2 = line.slice(3).trim().replace(/\*\*/g, '');
      const hLines = doc.splitTextToSize(cleanH2, contentWidth);
      doc.text(hLines, marginX, currentY);
      currentY += hLines.length * 4.2 + 2;
      i++;
      continue;
    }

    if (line.startsWith('### ')) {
      checkBreak(9);
      currentY += 1.5;
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(9.5);
      doc.setTextColor(15, 23, 42);
      const cleanH3 = line.slice(4).trim().replace(/\*\*/g, '');
      const hLines = doc.splitTextToSize(cleanH3, contentWidth);
      doc.text(hLines, marginX, currentY);
      currentY += hLines.length * 3.8 + 1.8;
      i++;
      continue;
    }

    // Callout / Blockquote
    if (line.startsWith('>')) {
      const qText = line.replace(/^>\s*/, '').replace(/\*\*/g, '').trim();
      doc.setFont('helvetica', 'italic');
      doc.setFontSize(8.5);
      const qLines = doc.splitTextToSize(qText, contentWidth - 8);
      const qBoxH = qLines.length * 3.5 + 4;
      checkBreak(qBoxH + 2);

      doc.setFillColor(248, 250, 252);
      doc.setDrawColor(226, 232, 240);
      doc.roundedRect(marginX, currentY, contentWidth, qBoxH, 1, 1, 'FD');
      doc.setFillColor(59, 130, 246);
      doc.rect(marginX, currentY, 1.2, qBoxH, 'F');

      doc.setTextColor(71, 85, 105);
      doc.text(qLines, marginX + 4, currentY + 3.2);
      currentY += qBoxH + 2.5;
      i++;
      continue;
    }

    // Horizontal Rule
    if (line.trim() === '---' || line.trim() === '***') {
      checkBreak(5);
      currentY += 1.5;
      doc.setDrawColor(226, 232, 240);
      doc.setLineWidth(0.2);
      doc.line(marginX, currentY, marginX + contentWidth, currentY);
      currentY += 3.5;
      i++;
      continue;
    }

    // Numbered List
    const numMatch = line.trim().match(/^(\d+)\.\s+(.*)/);
    if (numMatch) {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9);
      const cleanItem = numMatch[2].replace(/\*\*/g, '');
      const itemLines = doc.splitTextToSize(cleanItem, contentWidth - 6);
      const itemHeight = itemLines.length * 3.6;

      checkBreak(itemHeight + 1.5);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(37, 99, 235);
      doc.text(`${numMatch[1]}.`, marginX, currentY);

      doc.setFont('helvetica', 'normal');
      doc.setTextColor(30, 41, 59);
      doc.text(itemLines, marginX + 5, currentY);
      currentY += itemHeight + 1.5;
      i++;
      continue;
    }

    // Bullet List
    if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9);
      const cleanBullet = line.trim().replace(/^[-*]\s+/, '').replace(/\*\*/g, '');
      const bLines = doc.splitTextToSize(cleanBullet, contentWidth - 5);
      const bHeight = bLines.length * 3.6;

      checkBreak(bHeight + 1.5);
      doc.setFillColor(37, 99, 235);
      doc.circle(marginX + 1.5, currentY - 1, 0.5, 'F');

      doc.setTextColor(30, 41, 59);
      doc.text(bLines, marginX + 4, currentY);
      currentY += bHeight + 1.5;
      i++;
      continue;
    }

    // Standard Paragraph
    if (line.trim().length > 0) {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9);
      doc.setTextColor(30, 41, 59);
      const cleanP = line.replace(/\*\*/g, '').replace(/`/g, '');
      const pLines = doc.splitTextToSize(cleanP, contentWidth);
      const pHeight = pLines.length * 3.6;

      checkBreak(pHeight + 2);
      doc.text(pLines, marginX, currentY);
      currentY += pHeight + 2.2;
    } else {
      currentY += 1.5; // empty line gap
    }

    i++;
  }

  // Stamp Mandatory Clickable Footer on EVERY page
  const totalPages = doc.getNumberOfPages();
  for (let p = 1; p <= totalPages; p++) {
    doc.setPage(p);
    const footY = pageHeight - 11;

    doc.setDrawColor(226, 232, 240);
    doc.setLineWidth(0.2);
    doc.line(marginX, footY, pageWidth - marginX, footY);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(7.5);
    doc.setTextColor(37, 99, 235);
    doc.textWithLink('GPT TO PDF ↗', marginX, footY + 4, { url: 'https://gpttopdf.vercel.app' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(6.8);
    doc.setTextColor(100, 116, 139);
    doc.textWithLink('Made by Priyal', marginX, footY + 7.5, { url: 'https://www.linkedin.com/in/priyal-kumar-29b26529a/' });

    const pageStr = `Page ${p} of ${totalPages}`;
    const pWidth = doc.getTextWidth(pageStr);
    doc.text(pageStr, pageWidth - marginX - pWidth, footY + 4);

    const siteStr = 'gpttopdf.vercel.app';
    const sWidth = doc.getTextWidth(siteStr);
    doc.text(siteStr, pageWidth - marginX - sWidth, footY + 7.5);
  }

  console.log(`[DENSE-PDF RESULT] Total Pages Generated: ${doc.getNumberOfPages()}`);
  const buf = doc.output('arraybuffer');
  console.log(`Buffer byte length: ${buf.byteLength}`);
}

testDensePdf();
