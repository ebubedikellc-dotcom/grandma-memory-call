const $ = (selector) => document.querySelector(selector);

const pairInput = $('#pairCode');
const jobBox = $('#jobBox');
const emptyBox = $('#emptyBox');
const statusText = $('#statusText');
const waMessage = $('#waMessage');
const waChat = $('#waChat');
const waCallNote = $('#waCallNote');
const photo = $('#photo');
let currentJob = null;

function cleanPhone(value) {
  return String(value || '').replace(/[^\d]/g, '');
}

function qsPair() {
  const params = new URLSearchParams(location.search);
  return params.get('pair') || '';
}

function setStatus(text) {
  if (statusText) statusText.textContent = text;
}

function whatsappMessageUrl(job) {
  const text = encodeURIComponent(job.birthdayMessage || '');
  return `https://wa.me/${cleanPhone(job.sisterPhone)}?text=${text}`;
}

function whatsappChatUrl(job) {
  return `https://wa.me/${cleanPhone(job.sisterPhone)}`;
}

function renderJob(job) {
  currentJob = job;

  if (!job) {
    emptyBox.classList.remove('hidden');
    jobBox.classList.add('hidden');
    setStatus('Waiting for a birthday call job from the control panel.');
    return;
  }

  emptyBox.classList.add('hidden');
  jobBox.classList.remove('hidden');
  setStatus(`Job received for ${job.sisterName}.`);

  $('#jobTitle').textContent = `${job.grandmaName} birthday call for ${job.sisterName}`;
  $('#jobPhone').textContent = `+${job.sisterPhone}`;
  $('#jobMessage').textContent = job.birthdayMessage || 'No message provided.';

  if (job.photoDataUrl) {
    photo.src = job.photoDataUrl;
    photo.classList.remove('hidden');
  } else {
    photo.classList.add('hidden');
  }

  waMessage.href = whatsappMessageUrl(job);
  waChat.href = whatsappChatUrl(job);
  waCallNote.textContent = 'After the chat opens, send the prepared message if WhatsApp asks, then tap the video-call button. A normal website cannot force WhatsApp Android to start video call automatically.';
}

async function pollJob() {
  const pairCode = pairInput.value.trim().toUpperCase();
  if (!pairCode) {
    renderJob(null);
    return;
  }

  try {
    const response = await fetch(`/api/jobs/latest?pairCode=${encodeURIComponent(pairCode)}`);
    const result = await response.json();
    renderJob(result.job);
  } catch {
    setStatus('Could not reach Render server. Keep this page open and try again.');
  }
}

async function updateStatus(status) {
  if (!currentJob) return;
  await fetch(`/api/jobs/${currentJob.id}`, {
    method: 'PATCH',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ status })
  });
  await pollJob();
}

pairInput.value = qsPair();
$('#loadJob').addEventListener('click', pollJob);
$('#markStarted').addEventListener('click', () => updateStatus('call-started-on-android'));
setInterval(pollJob, 5000);
pollJob();
