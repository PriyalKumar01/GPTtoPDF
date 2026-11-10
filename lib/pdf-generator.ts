import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { APP_CONFIG } from './config';
import { ConversationData, PdfOptions } from './types';
import { COLLEGE_STUDY_LOGO_B64 } from './college-study-logo-b64';
import { APP_LOGO_BASE64 } from './app-logo-b64';

export class NotesPdfGenerator {
  private doc: jsPDF;
  private options: PdfOptions;
  private conversation: ConversationData;
  private currentY: number;
  private marginX: number = 14;
  private marginTop: number = 14;
  private marginBottom: number = 24; // leaves room for the footer pills without border clipping
  private pageWidth: number;
  private pageHeight: number;
  private contentWidth: number;
  private baseFont: string;

  constructor(conversation: ConversationData, options: PdfOptions) {
    this.conversation = conversation;
    this.options = options;

    const format = options.paperSize === 'Letter' ? 'letter' : 'a4';
    this.doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format,
    });

    this.pageWidth = this.doc.internal.pageSize.getWidth();
    this.pageHeight = this.doc.internal.pageSize.getHeight();
    this.contentWidth = this.pageWidth - this.marginX * 2;
    this.currentY = this.marginTop;

    this.baseFont =
      options.fontFamily === 'Times'
        ? 'times'
        : options.fontFamily === 'Courier'
        ? 'courier'
        : 'helvetica';
  }

  private getScale() {
    switch (this.options.fontSize) {
      case 'small':
        return { body: 8.2, h1: 12, h2: 10, h3: 9, lineH: 3.3, gap: 1.6, caption: 7.0 };
      case 'large':
        return { body: 10.5, h1: 15, h2: 12.5, h3: 11, lineH: 4.2, gap: 2.4, caption: 8.0 };
      case 'medium':
      default:
        return { body: 9, h1: 13.5, h2: 11, h3: 9.5, lineH: 3.6, gap: 2.0, caption: 7.5 };
    }
  }

  private checkBreak(needed: number) {
    if (this.currentY + needed > this.pageHeight - this.marginBottom) {
      this.doc.addPage();
      this.currentY = this.marginTop;
    }
  }

  /**
   * Cleans inline asterisks, backticks, and math delimiters for pure text rendering
   */
  private cleanText(raw: string): string {
    if (!raw) return '';
    return raw
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*(.*?)\*/g, '$1')
      .replace(/`([^`]+)`/g, '$1')
      .replace(/\[(.*?)\]\(.*?\)/g, '$1')
      .replace(/\$\$(.*?)\$\$/g, '$1')
      .replace(/\$(.*?)\$/g, '$1')
      .replace(/<[^>]+>/g, '') // remove any residual HTML or XML tags (<text...>, </text>, etc.)
      .replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]/g, ''); // strip surrogate emojis that turn into garbage characters
  }

  /**
   * Document Title & Metadata Header with College Study Partner Badge
   */
  private renderHeader() {
    if (!this.options.includeTitle) return;

    const scale = this.getScale();
    const badgeW = 42;
    const badgeH = 13.5;
    const badgeX = this.pageWidth - this.marginX - badgeW;
    const badgeY = this.marginTop - 1;

    // 1. College Study Partner Badge on Right Top
    try {
      // Container Card
      this.doc.setFillColor(248, 250, 252);
      this.doc.setDrawColor(226, 232, 240);
      this.doc.setLineWidth(0.2);
      this.doc.roundedRect(badgeX, badgeY, badgeW, badgeH, 1, 1, 'FD');

      // Official College Study Logo
      const logoSize = 10.5;
      this.doc.addImage(
        COLLEGE_STUDY_LOGO_B64,
        'JPEG',
        badgeX + 1.5,
        badgeY + 1.5,
        logoSize,
        logoSize
      );

      // Badge Text Lines (Professional, clean without hyphens or symbols)
      this.doc.setFont('helvetica', 'bold');
      this.doc.setFontSize(5.2);
      this.doc.setTextColor(100, 116, 139);
      this.doc.text('POWERED BY', badgeX + 13.5, badgeY + 4.2);

      this.doc.setFont('helvetica', 'bold');
      this.doc.setFontSize(7.2);
      this.doc.setTextColor(15, 23, 42);
      this.doc.text('College Study', badgeX + 13.5, badgeY + 7.8);

      this.doc.setFont('helvetica', 'normal');
      this.doc.setFontSize(5.8);
      this.doc.setTextColor(2, 132, 199);
      this.doc.textWithLink('Your Study Hub', badgeX + 13.5, badgeY + 11.2, {
        url: APP_CONFIG.partner.url,
      });

      // Clickable hotspot over the whole badge
      if ((this.doc as any).link) {
        (this.doc as any).link(badgeX, badgeY, badgeW, badgeH, {
          url: APP_CONFIG.partner.url,
        });
      }
    } catch {
      // Continue gracefully if image rendering fails
    }

    // 2. Document Title (wrapped so it never overlaps the right-side partner badge)
    const titleWidth = this.contentWidth - badgeW - 6;
    this.doc.setFont(this.baseFont, 'bold');
    this.doc.setFontSize(scale.h1 + 1.5);
    this.doc.setTextColor(15, 23, 42); // slate 900
    const titleLines = this.doc.splitTextToSize(this.conversation.title, titleWidth);
    this.doc.text(titleLines, this.marginX, this.currentY + 2.5);
    this.currentY += titleLines.length * 4.8 + 2;

    // 3. Subtitle / Export Metadata
    if (this.options.includeDate) {
      this.doc.setFont(this.baseFont, 'normal');
      this.doc.setFontSize(7.5);
      this.doc.setTextColor(100, 116, 139); // slate 500
      const dateStr = new Date(this.conversation.createdAt || Date.now()).toISOString();
      const metaText = `Generated: ${dateStr} • Typeset for Academic Review`;
      this.doc.text(metaText, this.marginX, this.currentY);
      this.currentY += 4;
    }

    // Clear badge height
    this.currentY = Math.max(this.currentY, badgeY + badgeH + 3.5);

    // 4. Header Divider Line
    this.doc.setDrawColor(226, 232, 240);
    this.doc.setLineWidth(0.2);
    this.doc.line(this.marginX, this.currentY, this.marginX + this.contentWidth, this.currentY);
    this.currentY += 4.5;
  }

  /**
   * Compact User Question Card matching reference design
   */
  private renderUserBox(content: string) {
    if (!this.options.includeUserMessages) return;

    const scale = this.getScale();
    const cleanPrompt = this.cleanText(content);

    this.doc.setFont(this.baseFont, 'normal');
    this.doc.setFontSize(scale.body);
    const lines = this.doc.splitTextToSize(cleanPrompt, this.contentWidth - 8);
    const boxHeight = lines.length * scale.lineH + 8.5;

    this.checkBreak(boxHeight + 4);

    // Subtle container
    this.doc.setFillColor(248, 250, 252); // slate 50
    this.doc.setDrawColor(226, 232, 240); // slate 200
    this.doc.setLineWidth(0.2);
    this.doc.roundedRect(this.marginX, this.currentY, this.contentWidth, boxHeight, 1.5, 1.5, 'FD');

    // USER label
    this.doc.setFont(this.baseFont, 'bold');
    this.doc.setFontSize(6.5);
    this.doc.setTextColor(100, 116, 139); // slate 500
    this.doc.text('USER', this.marginX + 4, this.currentY + 4.5);

    // Text
    this.doc.setFont(this.baseFont, 'normal');
    this.doc.setFontSize(scale.body);
    this.doc.setTextColor(30, 41, 59); // slate 800
    this.doc.text(lines, this.marginX + 4, this.currentY + 8);

    this.currentY += boxHeight + 4.5;
  }

  /**
   * Markdown Content Stream for Assistant Responses
   */
  private renderAssistantContent(content: string) {
    if (!this.options.includeAssistantMessages) return;

    const scale = this.getScale();

    // ASSISTANT label
    this.checkBreak(8);
    this.doc.setFont(this.baseFont, 'bold');
    this.doc.setFontSize(6.5);
    this.doc.setTextColor(100, 116, 139);
    this.doc.text('ASSISTANT', this.marginX, this.currentY);
    this.currentY += 4;

    const rawLines = content.split('\n');
    let i = 0;

    while (i < rawLines.length) {
      const line = rawLines[i];

      // 1. Table
      if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
        const tableLines: string[] = [];
        while (i < rawLines.length && rawLines[i].trim().startsWith('|') && rawLines[i].trim().endsWith('|')) {
          tableLines.push(rawLines[i]);
          i++;
        }

        if (tableLines.length >= 2) {
          this.checkBreak(18);
          const parseRow = (l: string) =>
            l.replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => this.cleanText(c.trim()));

          const head = [parseRow(tableLines[0])];
          const body = tableLines.slice(2).map(parseRow).filter((r) => r.some((c) => c.length > 0));

          autoTable(this.doc, {
            head,
            body,
            startY: this.currentY,
            margin: { left: this.marginX, right: this.marginX, bottom: this.marginBottom },
            theme: 'grid',
            headStyles: {
              fillColor: [241, 245, 249],
              textColor: [15, 23, 42],
              font: this.baseFont as any,
              fontStyle: 'bold',
              fontSize: scale.body - 1,
              cellPadding: 1.8,
            },
            bodyStyles: {
              textColor: [30, 41, 59],
              font: this.baseFont as any,
              fontSize: scale.body - 1,
              cellPadding: 1.6,
            },
            alternateRowStyles: {
              fillColor: [255, 255, 255],
            },
            tableLineColor: [226, 232, 240],
            tableLineWidth: 0.15,
          });

          const finalY = (this.doc as any).lastAutoTable?.finalY;
          if (finalY) this.currentY = finalY + 3.5;
        }
        continue;
      }

      // 2. Code Block
      if (line.trim().startsWith('```')) {
        const codeLines: string[] = [];
        i++;
        while (i < rawLines.length && !rawLines[i].trim().startsWith('```')) {
          codeLines.push(rawLines[i]);
          i++;
        }
        i++; // skip closing ```

        if (this.options.includeCodeBlocks) {
          const codeText = codeLines.join('\n');
          this.doc.setFont('courier', 'normal');
          this.doc.setFontSize(scale.body - 1);
          const splitCode = this.doc.splitTextToSize(codeText, this.contentWidth - 6);
          const boxH = splitCode.length * (scale.lineH - 0.2) + 4;

          this.checkBreak(Math.min(boxH, 30));
          this.doc.setFillColor(248, 250, 252);
          this.doc.setDrawColor(226, 232, 240);
          this.doc.setLineWidth(0.2);
          this.doc.roundedRect(this.marginX, this.currentY, this.contentWidth, boxH, 1, 1, 'FD');

          this.doc.setTextColor(30, 41, 59);
          this.doc.text(splitCode, this.marginX + 3, this.currentY + 3.5);
          this.currentY += boxH + 3;
        }
        continue;
      }

      // 2b. Markdown Images & Diagrams (High-resolution embedded diagrams)
      const imgMatch = line.trim().match(/^!\[(.*?)\]\((data:image\/[^;]+;base64,[^)]+|https?:\/\/[^)]+)\)$/);
      if (imgMatch) {
        const altText = (imgMatch[1] || '').trim();
        const imgData = imgMatch[2];

        try {
          const imgProps = this.doc.getImageProperties(imgData);
          const aspect =
            imgProps.width && imgProps.height
              ? imgProps.width / imgProps.height
              : 1.6;
          const maxW = Math.min(125, this.contentWidth - 10);
          const maxH = 55;
          let w = maxW;
          let h = w / aspect;
          if (h > maxH) {
            h = maxH;
            w = h * aspect;
          }

          this.checkBreak(h + 12);
          const imgX = this.marginX + (this.contentWidth - w) / 2;

          // Light card background with subtle border
          this.doc.setFillColor(255, 255, 255);
          this.doc.setDrawColor(226, 232, 240);
          this.doc.setLineWidth(0.25);
          this.doc.roundedRect(imgX - 1, this.currentY - 0.5, w + 2, h + 1, 1, 1, 'FD');

          // Embed diagram image
          this.doc.addImage(imgData, 'JPEG', imgX, this.currentY, w, h);
          this.currentY += h + 3;

          // Caption centered below diagram
          if (altText) {
            this.doc.setFont(this.baseFont, 'normal');
            this.doc.setFontSize(scale.caption);
            this.doc.setTextColor(100, 116, 139);
            const cleanCap = this.cleanText(altText);
            const capLines = this.doc.splitTextToSize(cleanCap, this.contentWidth - 20);
            this.doc.text(capLines, this.marginX + this.contentWidth / 2, this.currentY, {
              align: 'center',
            });
            this.currentY += capLines.length * 3 + 2;
          }
          this.currentY += 2;
        } catch {
          // Graceful fallback to styled diagram box if image data fails to decode
          this.checkBreak(10);
          this.doc.setFillColor(248, 250, 252);
          this.doc.setDrawColor(203, 213, 225);
          this.doc.setLineWidth(0.2);
          this.doc.roundedRect(this.marginX, this.currentY, this.contentWidth, 8, 1, 1, 'FD');
          this.doc.setFont(this.baseFont, 'italic');
          this.doc.setFontSize(scale.caption);
          this.doc.setTextColor(100, 116, 139);
          this.doc.text(`[Diagram: ${this.cleanText(altText)}]`, this.marginX + 4, this.currentY + 5.5);
          this.currentY += 11;
        }

        i++;
        continue;
      }

      // 3. Headings
      if (line.startsWith('# ')) {
        this.checkBreak(11);
        this.currentY += 2;
        this.doc.setFont(this.baseFont, 'bold');
        this.doc.setFontSize(scale.h1);
        this.doc.setTextColor(15, 23, 42);
        const text = this.cleanText(line.slice(2));
        const hLines = this.doc.splitTextToSize(text, this.contentWidth);
        this.doc.text(hLines, this.marginX, this.currentY);
        this.currentY += hLines.length * 4.6 + 2.2;
        i++;
        continue;
      }

      if (line.startsWith('## ')) {
        this.checkBreak(9);
        this.currentY += 1.8;
        this.doc.setFont(this.baseFont, 'bold');
        this.doc.setFontSize(scale.h2);
        this.doc.setTextColor(15, 23, 42);
        const text = this.cleanText(line.slice(3));
        const hLines = this.doc.splitTextToSize(text, this.contentWidth);
        this.doc.text(hLines, this.marginX, this.currentY);
        this.currentY += hLines.length * 4.0 + 1.8;
        i++;
        continue;
      }

      if (line.startsWith('### ')) {
        this.checkBreak(8);
        this.currentY += 1.5;
        this.doc.setFont(this.baseFont, 'bold');
        this.doc.setFontSize(scale.h3);
        this.doc.setTextColor(15, 23, 42);
        const text = this.cleanText(line.slice(4));
        const hLines = this.doc.splitTextToSize(text, this.contentWidth);
        this.doc.text(hLines, this.marginX, this.currentY);
        this.currentY += hLines.length * 3.6 + 1.5;
        i++;
        continue;
      }

      // 4. Callout / Blockquote
      if (line.startsWith('>')) {
        const qText = this.cleanText(line.replace(/^>\s*/, ''));
        this.doc.setFont(this.baseFont, 'italic');
        this.doc.setFontSize(scale.body - 0.5);
        const qLines = this.doc.splitTextToSize(qText, this.contentWidth - 8);
        const qBoxH = qLines.length * scale.lineH + 3.5;
        this.checkBreak(qBoxH + 2);

        this.doc.setFillColor(248, 250, 252);
        this.doc.setDrawColor(226, 232, 240);
        this.doc.setLineWidth(0.2);
        this.doc.roundedRect(this.marginX, this.currentY, this.contentWidth, qBoxH, 1, 1, 'FD');

        this.doc.setFillColor(59, 130, 246);
        this.doc.rect(this.marginX, this.currentY, 1.2, qBoxH, 'F');

        this.doc.setTextColor(71, 85, 105);
        this.doc.text(qLines, this.marginX + 4, this.currentY + 3.2);
        this.currentY += qBoxH + 2.5;
        i++;
        continue;
      }

      // 5. Horizontal rule
      if (line.trim() === '---' || line.trim() === '***') {
        this.checkBreak(4);
        this.currentY += 1.5;
        this.doc.setDrawColor(226, 232, 240);
        this.doc.setLineWidth(0.2);
        this.doc.line(this.marginX, this.currentY, this.marginX + this.contentWidth, this.currentY);
        this.currentY += 3.2;
        i++;
        continue;
      }

      // 6. Numbered List
      const numMatch = line.trim().match(/^(\d+)\.\s+(.*)/);
      if (numMatch) {
        this.doc.setFont(this.baseFont, 'normal');
        this.doc.setFontSize(scale.body);
        const cleanItem = this.cleanText(numMatch[2]);
        const itemLines = this.doc.splitTextToSize(cleanItem, this.contentWidth - 6);
        const itemHeight = itemLines.length * scale.lineH;

        this.checkBreak(itemHeight + 1.2);
        this.doc.setFont(this.baseFont, 'bold');
        this.doc.setTextColor(37, 99, 235);
        this.doc.text(`${numMatch[1]}.`, this.marginX, this.currentY);

        this.doc.setFont(this.baseFont, 'normal');
        this.doc.setTextColor(30, 41, 59);
        this.doc.text(itemLines, this.marginX + 5, this.currentY);
        this.currentY += itemHeight + 1.2;
        i++;
        continue;
      }

      // 7. Bullet List
      if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
        this.doc.setFont(this.baseFont, 'normal');
        this.doc.setFontSize(scale.body);
        const cleanBullet = this.cleanText(line.trim().replace(/^[-*]\s+/, ''));
        const bLines = this.doc.splitTextToSize(cleanBullet, this.contentWidth - 5);
        const bHeight = bLines.length * scale.lineH;

        this.checkBreak(bHeight + 1.2);
        this.doc.setFillColor(37, 99, 235);
        this.doc.circle(this.marginX + 1.5, this.currentY - 0.9, 0.45, 'F');

        this.doc.setTextColor(30, 41, 59);
        this.doc.text(bLines, this.marginX + 4, this.currentY);
        this.currentY += bHeight + 1.2;
        i++;
        continue;
      }

      // 8. Standard Paragraph
      if (line.trim().length > 0) {
        this.doc.setFont(this.baseFont, 'normal');
        this.doc.setFontSize(scale.body);
        this.doc.setTextColor(30, 41, 59);
        const cleanP = this.cleanText(line);
        const pLines = this.doc.splitTextToSize(cleanP, this.contentWidth);
        const pHeight = pLines.length * scale.lineH;

        this.checkBreak(pHeight + 1.8);
        this.doc.text(pLines, this.marginX, this.currentY);
        this.currentY += pHeight + scale.gap;
      } else {
        this.currentY += 1.2;
      }

      i++;
    }
  }

  /**
   * Stamp MS Word Shadow border or clean border & headers on every page
   */
  private stampPageDecorations() {
    const totalPages = this.doc.getNumberOfPages();

    for (let p = 1; p <= totalPages; p++) {
      this.doc.setPage(p);

      // 1. Page Border (MS Word Shadow Border or Clean Line)
      if (this.options.borderStyle === 'shadow') {
        const bInset = 7;
        const bX = bInset;
        const bY = bInset;
        const bW = this.pageWidth - bInset * 2;
        const bH = this.pageHeight - bInset * 2;
        const shadowThick = 1.0;

        // Solid drop shadow on Right & Bottom (MS Word style)
        this.doc.setFillColor(15, 23, 42); // deep dark shadow
        this.doc.rect(bX + bW, bY + shadowThick, shadowThick, bH, 'F');
        this.doc.rect(bX + shadowThick, bY + bH, bW, shadowThick, 'F');

        // Crisp dark border frame
        this.doc.setDrawColor(15, 23, 42);
        this.doc.setLineWidth(0.4);
        this.doc.rect(bX, bY, bW, bH, 'D');
      } else if (this.options.borderStyle === 'clean') {
        const bInset = 7;
        this.doc.setDrawColor(226, 232, 240);
        this.doc.setLineWidth(0.25);
        this.doc.roundedRect(bInset, bInset, this.pageWidth - bInset * 2, this.pageHeight - bInset * 2, 1.2, 1.2, 'D');
      }

      // 2. Multi-page College Study badge if scope is 'all-pages'
      if (this.options.partnerBadgeScope === 'all-pages' && p > 1) {
        try {
          const bW = 38;
          const bH = 10;
          const bX = this.pageWidth - this.marginX - bW;
          const bY = 8.5; // fits neatly at the top within border

          this.doc.setFillColor(248, 250, 252);
          this.doc.setDrawColor(226, 232, 240);
          this.doc.setLineWidth(0.18);
          this.doc.roundedRect(bX, bY, bW, bH, 1, 1, 'FD');

          this.doc.addImage(COLLEGE_STUDY_LOGO_B64, 'JPEG', bX + 1.2, bY + 1.2, 7.6, 7.6);
          this.doc.setFont('helvetica', 'bold');
          this.doc.setFontSize(5.8);
          this.doc.setTextColor(15, 23, 42);
          this.doc.text('College Study', bX + 10.2, bY + 4.5);
          this.doc.setFont('helvetica', 'normal');
          this.doc.setFontSize(4.8);
          this.doc.setTextColor(2, 132, 199);
          this.doc.textWithLink('Your Study Hub', bX + 10.2, bY + 7.8, { url: APP_CONFIG.partner.url });
          if ((this.doc as any).link) {
            (this.doc as any).link(bX, bY, bW, bH, { url: APP_CONFIG.partner.url });
          }
        } catch {
          // Continue gracefully
        }
      }
    }
  }

  /**
   * Stamp Mandatory Clickable Footer on every single page
   * Left: App Brand ("AI Chat to PDF" with app logo & PDF badge) + "Developer: Priyal Kumar"
   * Right: Page X of Y & Site Domain
   * Coordinates strictly sit above bottom border line (zero cutting or overlap)
   */
  private stampMandatoryFooter() {
    const totalPages = this.doc.getNumberOfPages();

    for (let p = 1; p <= totalPages; p++) {
      this.doc.setPage(p);
      const footY = 274.5;

      // Top footer divider line
      this.doc.setDrawColor(226, 232, 240);
      this.doc.setLineWidth(0.2);
      this.doc.line(this.marginX, footY, this.pageWidth - this.marginX, footY);

      // App Brand Logo Container (Clean square frame matching Navbar)
      const logoX = this.marginX;
      const logoY = 276.5;
      const logoBoxSize = 8.0;

      this.doc.setFillColor(255, 255, 255);
      this.doc.setDrawColor(226, 232, 240);
      this.doc.setLineWidth(0.2);
      this.doc.roundedRect(logoX, logoY, logoBoxSize, logoBoxSize, 0.8, 0.8, 'FD');

      try {
        this.doc.addImage(
          APP_LOGO_BASE64,
          'JPEG',
          logoX + 0.6,
          logoY + 0.6,
          logoBoxSize - 1.2,
          logoBoxSize - 1.2
        );
      } catch (err) {
        // Fallback icon placeholder if image fails
      }

      // Clickable link over the logo to website
      if ((this.doc as any).link) {
        (this.doc as any).link(logoX, logoY, logoBoxSize, logoBoxSize, {
          url: APP_CONFIG.appUrl,
        });
      }

      // Brand Title + Red PDF Badge (Line 1)
      const textX = logoX + logoBoxSize + 2.5;
      const titleStr = 'AI Chat to PDF';
      this.doc.setFont('helvetica', 'bold');
      this.doc.setFontSize(8.2);
      this.doc.setTextColor(15, 23, 42); // slate-900 (#0F172A)
      this.doc.textWithLink(titleStr, textX, 280.4, {
        url: APP_CONFIG.appUrl,
      });

      const titleW = this.doc.getTextWidth(titleStr);
      if ((this.doc as any).link) {
        (this.doc as any).link(textX, 276.8, titleW, 4.5, {
          url: APP_CONFIG.appUrl,
        });
      }

      // Red PDF Badge beside brand title
      const badgeX = textX + titleW + 1.8;
      const badgeY = 277.6;
      const badgeW = 7.0;
      const badgeH = 3.3;

      this.doc.setFillColor(254, 226, 226); // red-100 (#FEE2E2)
      this.doc.setDrawColor(252, 165, 165); // red-300 (#FCA5A5)
      this.doc.setLineWidth(0.18);
      this.doc.roundedRect(badgeX, badgeY, badgeW, badgeH, 0.5, 0.5, 'FD');

      this.doc.setFont('helvetica', 'bold');
      this.doc.setFontSize(5.5);
      this.doc.setTextColor(220, 38, 38); // red-600 (#DC2626)
      this.doc.text('PDF', badgeX + 1.3, badgeY + 2.4);

      // Developer Line (Line 2): "Developer: Priyal Kumar"
      const devStr = 'Developer: Priyal Kumar';
      this.doc.setFont('helvetica', 'bold');
      this.doc.setFontSize(7.0);
      this.doc.setTextColor(2, 132, 199); // sky-600 (#0284C7)
      this.doc.textWithLink(devStr, textX, 284.4, {
        url: APP_CONFIG.author.linkedinUrl,
      });

      const devW = this.doc.getTextWidth(devStr);
      if ((this.doc as any).link) {
        (this.doc as any).link(textX, 281.5, devW, 3.8, {
          url: APP_CONFIG.author.linkedinUrl,
        });
      }

      // Right: Page X of Y & Site Domain (Clean slate font)
      if (this.options.includePageNumbers) {
        this.doc.setFont('helvetica', 'bold');
        this.doc.setFontSize(7.5);
        this.doc.setTextColor(71, 85, 105); // slate-600

        const pageStr = `Page ${p} of ${totalPages}`;
        const pWidth = this.doc.getTextWidth(pageStr);
        this.doc.text(pageStr, this.pageWidth - this.marginX - pWidth, 280.4);

        this.doc.setFont('helvetica', 'normal');
        this.doc.setFontSize(6.8);
        this.doc.setTextColor(148, 163, 184); // slate-400
        const siteHost = APP_CONFIG.appUrl.replace(/^https?:\/\//, '');
        const sWidth = this.doc.getTextWidth(siteHost);
        this.doc.text(siteHost, this.pageWidth - this.marginX - sWidth, 284.4);
      }
    }
  }

  public generate(): jsPDF {
    this.renderHeader();

    for (const msg of this.conversation.messages) {
      if (msg.role === 'user') {
        this.renderUserBox(msg.content);
      } else {
        this.renderAssistantContent(msg.content);
      }
    }

    this.stampPageDecorations();
    this.stampMandatoryFooter();
    return this.doc;
  }
}

export function downloadConversationPdf(
  conversation: ConversationData,
  options: PdfOptions
): void {
  const generator = new NotesPdfGenerator(conversation, options);
  const doc = generator.generate();

  const cleanTitle = (conversation.title || 'ChatGPT_Notes')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 45);

  const filename = `${cleanTitle}_gpttopdf.pdf`;
  doc.save(filename);
}
