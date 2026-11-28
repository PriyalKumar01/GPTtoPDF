const base = 'https://assets-proxy.anthropic.com/claude-ai/v2';
const files = [
  '/assets/v1/shared-900-DoPfP8KP.js',
  '/assets/v1/shared-902-BbdDV5cQ.js',
  '/assets/v1/shared-903-Uf02o5hX.js',
  '/assets/v1/shared-904-CtPlN5dA.js',
  '/assets/v1/shared-905-DwQvB0A8.js',
  '/assets/v1/shared-906-DVLem9sy.js',
  '/assets/v1/shared-907-DCO_qAAY.js',
  '/assets/v1/shared-908-Bi3yBBqw.js',
  '/assets/v1/shared-909-B84E8Qaa.js',
  '/assets/v1/shared-910-CUKMjgPa.js',
  '/assets/v1/shared-911-DRbG4bsb.js'
];

async function run() {
  for (const f of files) {
    const res = await fetch(base + f);
    const text = await res.text();
    const idx = text.indexOf('/share/');
    if (idx !== -1) {
      console.log('Found /share/ in', f);
      console.log(text.slice(Math.max(0, idx - 100), idx + 200));
    }
  }
}
run();
