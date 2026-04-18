// Probe the live index.html navigation for each nav link
const { chromium } = require('playwright');

const base = 'http://localhost:8766';
const routes = [
  '/', '/#manifesto', '/#/white-papers', '/#/rituals', '/#/mob-programming',
  '/#/flywheel', '/#/showcases', '/#/members', '/#/hall-of-fame',
  '/#/hall-of-shame', '/#/mission', '/#/charter', '/#/code-of-conduct', '/#sign'
];

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  const errors = [];
  page.on('console', m => { if (m.type() === 'error' || m.type() === 'warning') errors.push(`[${m.type()}] ${m.text()}`); });
  page.on('requestfailed', r => errors.push(`[reqfail] ${r.url()} ${r.failure() && r.failure().errorText}`));
  page.on('response', r => { if (r.status() >= 400) errors.push(`[${r.status()}] ${r.url()}`); });

  for (const r of routes) {
    errors.length = 0;
    await page.goto(base + r, { waitUntil: 'networkidle' }).catch(() => {});
    await page.waitForTimeout(400);
    const bodyClass = await page.evaluate(() => document.body.className);
    const appHTML = await page.evaluate(() => {
      const a = document.getElementById('app');
      return a ? (a.innerHTML || '').slice(0, 200) : '(no #app)';
    });
    const title = await page.title();
    console.log('─'.repeat(60));
    console.log('ROUTE', r);
    console.log('  title    :', title);
    console.log('  body.cls :', bodyClass || '(none)');
    console.log('  #app     :', appHTML.replace(/\s+/g, ' ').trim().slice(0, 160));
    if (errors.length) console.log('  issues   :\n    - ' + errors.join('\n    - '));
  }
  await browser.close();
})();
