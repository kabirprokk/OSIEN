const fs = require('fs');
const path = require('path');
const { startTui } = require('./tui');

function loadEnv() {
  const paths = [path.join(__dirname, '.env'), path.join(process.env.USERPROFILE || '', 'OSIEN', '.env')];
  for (const file of paths) {
    try {
      if (!fs.existsSync(file)) continue;
      for (const line of fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/)) {
        const entry = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
        if (entry && !process.env[entry[1]]) process.env[entry[1]] = entry[2].trim();
      }
    } catch {}
  }
}
loadEnv();

const configuration = {
  model: process.env.GROQ_MODEL || process.env.OSIN_MODEL || 'llama-3.3-70b-versatile',
  apiKey: process.env.GROQ_API_KEY || process.env.OPENAI_API_KEY || '',
  baseUrl: process.env.GROQ_BASE_URL || 'https://api.groq.com/openai/v1',
  supportUrl: process.env.OSIN_SUPPORT_URL || '',
};

const ESC = '\x1b[';
const dot = String.fromCharCode(0x25CF);
const useColor = !!process.stdout.isTTY && !process.env.NO_COLOR;
const logoPath = path.join(__dirname, 'logo-dots.json');

async function showLogo() {
  let rows;
  try { rows = JSON.parse(fs.readFileSync(logoPath, 'utf8')); }
  catch { return; }
  const render = phase => {
    const pulse = 0.63 + 0.37 * (0.5 - 0.5 * Math.cos(phase));
    let frame = '';
    for (let y = 0; y < rows.length; y++) {
      for (let x = 0; x < rows[y].length; x++) {
        const color = rows[y][x];
        if (!color) { frame += '  '; continue; }
        const lift = pulse * 0.25;
        const rgb = color.map(channel => Math.min(255, Math.round(channel * (1 - lift) + 145 * lift)));
        frame += useColor ? `${ESC}38;2;${rgb[0]};${rgb[1]};${rgb[2]}m${dot}${ESC}0m ` : `${dot} `;
      }
      frame += '\n';
    }
    process.stdout.write(frame);
  };
  if (!process.stdout.isTTY || !useColor || process.env.TERM === 'dumb') {
    for (const row of rows) console.log(row.map(color => color ? dot : ' ').join(' '));
    return;
  }
  const frames = 14;
  process.stdout.write(`${ESC}?25l`);
  try {
    render(0);
    for (let i = 1; i <= frames; i++) {
      await new Promise(resolve => setTimeout(resolve, 75));
      process.stdout.write(`${ESC}${rows.length}A`);
      render((i / frames) * Math.PI * 2);
    }
  } finally {
    process.stdout.write(`${ESC}?25h`);
  }
}

showLogo().then(() => startTui(configuration));
