const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const MAX_BODY_BYTES = 8 * 1024 * 1024;

const jobs = new Map();

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml'
};

function sendJson(res, status, data) {
  const payload = JSON.stringify(data);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(payload)
  });
  res.end(payload);
}

function sendText(res, status, text) {
  res.writeHead(status, { 'content-type': 'text/plain; charset=utf-8' });
  res.end(text);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let total = 0;
    const chunks = [];

    req.on('data', (chunk) => {
      total += chunk.length;
      if (total > MAX_BODY_BYTES) {
        reject(new Error('Body too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });

    req.on('end', () => {
      const text = Buffer.concat(chunks).toString('utf8');
      resolve(text ? JSON.parse(text) : {});
    });

    req.on('error', reject);
  });
}

function normalizePhone(value) {
  return String(value || '').replace(/[^\d]/g, '');
}

function createPairCode() {
  return crypto.randomBytes(3).toString('hex').toUpperCase();
}

function createJob(input) {
  const id = crypto.randomUUID();
  const pairCode = String(input.pairCode || createPairCode()).trim().toUpperCase();
  const now = new Date().toISOString();

  const job = {
    id,
    pairCode,
    status: 'created',
    createdAt: now,
    updatedAt: now,
    grandmaName: String(input.grandmaName || 'Grandma').trim(),
    sisterName: String(input.sisterName || 'Sister').trim(),
    sisterPhone: normalizePhone(input.sisterPhone),
    birthdayMessage: String(input.birthdayMessage || '').trim(),
    photoDataUrl: String(input.photoDataUrl || ''),
    note: 'Birthday write-up is sent as WhatsApp text. Video call uses Grandma face as the intended camera.'
  };

  if (!job.sisterPhone) {
    throw new Error('Sister WhatsApp number is required.');
  }

  jobs.set(id, job);
  return job;
}

function latestJobForPair(pairCode) {
  const normalized = String(pairCode || '').trim().toUpperCase();
  const list = Array.from(jobs.values())
    .filter((job) => job.pairCode === normalized)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  return list[0] || null;
}

function publicPath(urlPath) {
  const clean = decodeURIComponent(urlPath.split('?')[0]);
  const filePath = clean === '/' ? '/index.html' : clean;
  const resolved = path.normalize(path.join(PUBLIC_DIR, filePath));
  if (!resolved.startsWith(PUBLIC_DIR)) return null;
  return resolved;
}

async function handleApi(req, res, url) {
  try {
    if (req.method === 'GET' && url.pathname === '/health') {
      return sendJson(res, 200, { ok: true });
    }

    if (req.method === 'POST' && url.pathname === '/api/jobs') {
      const body = await readBody(req);
      const job = createJob(body);
      return sendJson(res, 201, { ok: true, job });
    }

    if (req.method === 'GET' && url.pathname === '/api/jobs/latest') {
      const pairCode = url.searchParams.get('pairCode');
      const job = latestJobForPair(pairCode);
      return sendJson(res, 200, { ok: true, job });
    }

    if (req.method === 'PATCH' && url.pathname.startsWith('/api/jobs/')) {
      const id = url.pathname.split('/').pop();
      const job = jobs.get(id);
      if (!job) return sendJson(res, 404, { ok: false, error: 'Job not found.' });

      const body = await readBody(req);
      job.status = String(body.status || job.status);
      job.updatedAt = new Date().toISOString();
      jobs.set(id, job);
      return sendJson(res, 200, { ok: true, job });
    }

    return null;
  } catch (error) {
    return sendJson(res, 400, { ok: false, error: error.message });
  }
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const apiResult = await handleApi(req, res, url);
  if (apiResult !== null) return;

  const file = publicPath(url.pathname);
  if (!file) return sendText(res, 403, 'Forbidden');

  fs.readFile(file, (error, content) => {
    if (error) return sendText(res, 404, 'Not found');
    const ext = path.extname(file).toLowerCase();
    res.writeHead(200, { 'content-type': mimeTypes[ext] || 'application/octet-stream' });
    res.end(content);
  });
});

server.listen(PORT, () => {
  console.log(`Grandma Memory Call server listening on ${PORT}`);
});
