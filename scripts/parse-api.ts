import fs from 'fs';

async function main() {
  const shareId = '6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const apiRes = await fetch(`https://chatgpt.com/backend-api/share/${shareId}`, {
    headers: {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
      Accept: 'application/json',
    },
  });
  const data = await apiRes.json();
  console.log('Title:', data.title);
  
  if (data.linear_conversation) {
    for (let i = 0; i < data.linear_conversation.length; i++) {
      const item = data.linear_conversation[i];
      console.log(`Index ${i}: keys =`, Object.keys(item));
      if (item.message) {
        console.log(`  message keys:`, Object.keys(item.message));
        console.log(`  author:`, item.message.author);
        console.log(`  content:`, item.message.content);
      }
    }
  }
}

main().catch(console.error);
