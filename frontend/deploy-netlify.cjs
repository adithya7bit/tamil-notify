const https = require('https');
const fs = require('fs');
const path = require('path');

const NETLIFY_TOKEN = 'nfp_5N499HcVgy6uSTRJaFR3hqCwpUhzXrUe39b5';
const ZIP_PATH = path.join(__dirname, 'dist.zip');

function request(options, data) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          resolve({ status: res.statusCode, data: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function deploy() {
  console.log('1. Checking or creating Netlify site...');
  const siteName = 'tamil-notify-' + Math.floor(1000 + Math.random() * 9000);

  const createRes = await request({
    hostname: 'api.netlify.com',
    path: '/api/v1/sites',
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${NETLIFY_TOKEN}`,
      'Content-Type': 'application/json',
      'User-Agent': 'TamilNotifyDeployer'
    }
  }, JSON.stringify({ name: siteName }));

  if (createRes.status !== 201 && createRes.status !== 200) {
    console.error('Failed to create site:', createRes);
    return;
  }

  const site = createRes.data;
  console.log(`✓ Site created: ${site.name} (ID: ${site.id})`);
  console.log(`  Site URL: ${site.ssl_url || site.url}`);

  console.log('2. Uploading dist.zip deploy payload...');
  const zipBuffer = fs.readFileSync(ZIP_PATH);

  const uploadOptions = {
    hostname: 'api.netlify.com',
    path: `/api/v1/sites/${site.id}/deploys`,
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${NETLIFY_TOKEN}`,
      'Content-Type': 'application/zip',
      'Content-Length': zipBuffer.length,
      'User-Agent': 'TamilNotifyDeployer'
    }
  };

  const deployRes = await new Promise((resolve, reject) => {
    const req = https.request(uploadOptions, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: body });
        }
      });
    });
    req.on('error', reject);
    req.write(zipBuffer);
    req.end();
  });

  if (deployRes.status === 200 || deployRes.status === 201) {
    const deployData = deployRes.data;
    console.log(`✓ Deploy uploaded! State: ${deployData.state}`);
    console.log(`🎉 LIVE PUBLIC URL: ${deployData.ssl_url || deployData.url || site.ssl_url || site.url}`);
  } else {
    console.error('Deploy failed:', deployRes);
  }
}

deploy().catch(console.error);
