const {spawn} = require('child_process');
const puppeteer = require('puppeteer');

async function run() {
  const server = spawn('npx', ['http-server', '-p', '8080', '-c-1'], {stdio: 'inherit'});
  await new Promise(r => setTimeout(r, 1000));

  const browser = await puppeteer.launch({headless: 'new', ignoreHTTPSErrors: true, args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage', '--ignore-certificate-errors']});
  const page = await browser.newPage();
  page.on('console', msg => console.log('PAGE:', msg.text()));
  await page.goto('http://localhost:8080/index.html?autostart=1&username=TestUser');
  await page.waitForFunction('window.gameLoaded === true', {timeout: 120000});
  await browser.close();
  server.kill();
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
