async function test() {
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
  const item20 = data.linear_conversation[20];
  const parts = item20.message.content.parts;
  console.log('Parts count in item 20:', parts.length);
  const text = parts.join('\n\n');
  console.log('Total text length in item 20:', text.length);
  console.log('Beginning snippet:\n', text.slice(0, 500));
  console.log('Ending snippet:\n', text.slice(-500));
}

test();
