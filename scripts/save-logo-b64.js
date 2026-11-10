import fs from 'fs';

const b64 = fs.readFileSync('public/images/college-study-logo.jpg').toString('base64');
console.log('Base64 length:', b64.length);

const tsContent = `// College Study Partner Logo (Auto-generated Base64 Data URL)
export const COLLEGE_STUDY_LOGO_B64 = 'data:image/jpeg;base64,${b64}';
`;

fs.writeFileSync('lib/college-study-logo-b64.ts', tsContent, 'utf8');
console.log('Successfully written lib/college-study-logo-b64.ts');
