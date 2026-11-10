import { NotesPdfGenerator } from '../lib/pdf-generator';
import { SAMPLE_CONVERSATIONS } from '../lib/sample-data';
import { APP_CONFIG } from '../lib/config';

async function main() {
  const sample = SAMPLE_CONVERSATIONS['operating-systems'];
  const generator = new NotesPdfGenerator(sample, APP_CONFIG.defaultPdfOptions);
  const doc = generator.generate();
  const pageCount = doc.getNumberOfPages();
  const buffer = doc.output('arraybuffer');
  console.log(`[TEST-SUCCESS] Pages: ${pageCount}, Bytes: ${buffer.byteLength}`);
}

main().catch(err => {
  console.error('[TEST-ERROR]', err);
  process.exit(1);
});
