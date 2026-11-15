import jsPDF from 'jspdf';

export interface ImageToPdfItem {
  id: string;
  file: File;
  previewUrl: string;
  name: string;
  size: number;
  width: number;
  height: number;
  rotation: number; // 0, 90, 180, 270
}

export interface ImageToPdfOptions {
  pageSize: 'A4' | 'Letter' | 'Fit';
  orientation: 'auto' | 'portrait' | 'landscape';
  margin: 'none' | 'small' | 'normal'; // none: 0mm, small: 10mm, normal: 20mm
  filename: string;
}

// Standard page dimensions in mm
const PAGE_SIZES: Record<'A4' | 'Letter', { width: number; height: number }> = {
  A4: { width: 210, height: 297 },
  Letter: { width: 215.9, height: 279.4 },
};

const MARGIN_SIZES: Record<'none' | 'small' | 'normal', number> = {
  none: 0,
  small: 10,
  normal: 20,
};

/**
 * Rotates an image in a canvas and returns a data URL
 */
async function getRotatedImageData(
  imgUrl: string,
  rotation: number
): Promise<{ dataUrl: string; width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      if (rotation === 0) {
        resolve({ dataUrl: imgUrl, width: img.naturalWidth, height: img.naturalHeight });
        return;
      }

      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve({ dataUrl: imgUrl, width: img.naturalWidth, height: img.naturalHeight });
        return;
      }

      const isRotated90or270 = rotation === 90 || rotation === 270;
      canvas.width = isRotated90or270 ? img.naturalHeight : img.naturalWidth;
      canvas.height = isRotated90or270 ? img.naturalWidth : img.naturalHeight;

      ctx.translate(canvas.width / 2, canvas.height / 2);
      ctx.rotate((rotation * Math.PI) / 180);
      ctx.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2);

      resolve({
        dataUrl: canvas.toDataURL('image/jpeg', 0.95),
        width: canvas.width,
        height: canvas.height,
      });
    };
    img.onerror = () => reject(new Error('Failed to load image for rotation'));
    img.src = imgUrl;
  });
}

/**
 * Converts a list of images to a single PDF document
 */
export async function convertImagesToPdf(
  items: ImageToPdfItem[],
  options: ImageToPdfOptions,
  onProgress?: (current: number, total: number) => void
): Promise<Blob> {
  if (items.length === 0) {
    throw new Error('No images provided for PDF generation');
  }

  let pdf: jsPDF | null = null;
  const marginMm = MARGIN_SIZES[options.margin] || 0;

  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (onProgress) onProgress(i + 1, items.length);

    // Apply rotation if needed
    const { dataUrl, width, height } = await getRotatedImageData(
      item.previewUrl,
      item.rotation
    );

    const isImageLandscape = width > height;

    let pageOrientation: 'p' | 'l' = 'p';
    if (options.orientation === 'portrait') {
      pageOrientation = 'p';
    } else if (options.orientation === 'landscape') {
      pageOrientation = 'l';
    } else {
      // Auto: match image aspect ratio
      pageOrientation = isImageLandscape ? 'l' : 'p';
    }

    if (options.pageSize === 'Fit') {
      // Fit to image dimensions (convert px to mm at ~72-96 dpi)
      // 1 px = 0.264583 mm
      const mmWidth = width * 0.264583 + marginMm * 2;
      const mmHeight = height * 0.264583 + marginMm * 2;

      if (i === 0) {
        pdf = new jsPDF({
          orientation: mmWidth > mmHeight ? 'l' : 'p',
          unit: 'mm',
          format: [mmWidth, mmHeight],
        });
      } else {
        pdf!.addPage([mmWidth, mmHeight], mmWidth > mmHeight ? 'l' : 'p');
      }

      pdf!.addImage(dataUrl, 'JPEG', marginMm, marginMm, mmWidth - marginMm * 2, mmHeight - marginMm * 2);
    } else {
      // Standard A4 or Letter
      const standardDimensions = PAGE_SIZES[options.pageSize];
      let pageWidth =
        pageOrientation === 'p' ? standardDimensions.width : standardDimensions.height;
      let pageHeight =
        pageOrientation === 'p' ? standardDimensions.height : standardDimensions.width;

      if (i === 0) {
        pdf = new jsPDF({
          orientation: pageOrientation,
          unit: 'mm',
          format: options.pageSize.toLowerCase() as 'a4' | 'letter',
        });
      } else {
        pdf!.addPage(options.pageSize.toLowerCase() as 'a4' | 'letter', pageOrientation);
      }

      const printableWidth = pageWidth - marginMm * 2;
      const printableHeight = pageHeight - marginMm * 2;

      // Calculate best fitting dimensions preserving aspect ratio
      const imgRatio = width / height;
      const targetRatio = printableWidth / printableHeight;

      let renderWidth = printableWidth;
      let renderHeight = printableHeight;

      if (imgRatio > targetRatio) {
        renderWidth = printableWidth;
        renderHeight = printableWidth / imgRatio;
      } else {
        renderHeight = printableHeight;
        renderWidth = printableHeight * imgRatio;
      }

      // Center image within margins
      const posX = marginMm + (printableWidth - renderWidth) / 2;
      const posY = marginMm + (printableHeight - renderHeight) / 2;

      pdf!.addImage(dataUrl, 'JPEG', posX, posY, renderWidth, renderHeight, undefined, 'FAST');
    }
  }

  if (!pdf) {
    throw new Error('PDF initialization failed');
  }

  return pdf.output('blob');
}
