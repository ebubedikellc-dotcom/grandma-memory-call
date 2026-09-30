const $ = (selector) => document.querySelector(selector);

const form = $('#jobForm');
const photoInput = $('#grandmaPhoto');
const avatar = $('#previewAvatar');
const sisterPreview = $('#sisterPreview');
const numberPreview = $('#numberPreview');
const messagePreview = $('#messagePreview');
const pairCodeInput = $('#pairCode');
const createdBox = $('#createdBox');
const createdText = $('#createdText');

function cleanPhone(value) {
  return String(value || '').replace(/[^\d]/g, '');
}

function randomPairCode() {
  return Math.random().toString(16).slice(2, 8).toUpperCase();
}

function ensurePairCode() {
  if (pairCodeInput && !pairCodeInput.value.trim()) {
    pairCodeInput.value = randomPairCode();
  }
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    if (!file) return resolve('');
    const reader = new FileReader();
    reader.onerror = reject;
    reader.onload = () => {
      const image = new Image();
      image.onerror = reject;
      image.onload = () => {
        const maxSide = 1280;
        const scale = Math.min(1, maxSide / Math.max(image.width, image.height));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(image.width * scale));
        canvas.height = Math.max(1, Math.round(image.height * scale));
        const context = canvas.getContext('2d');
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', 0.82));
      };
      image.src = reader.result;
    };
    reader.readAsDataURL(file);
  });
}

function refreshPreview() {
  const sisterName = $('#sisterName')?.value.trim() || 'Sister';
  const sisterPhone = cleanPhone($('#sisterPhone')?.value);
  const message = $('#birthdayMessage')?.value.trim();
  if (sisterPreview) sisterPreview.textContent = `${sisterName} sees Grandma`;
  if (numberPreview) numberPreview.textContent = sisterPhone ? `+${sisterPhone}` : 'Add WhatsApp number';
  if (messagePreview) messagePreview.textContent = message ? 'Message sent in WhatsApp chat first' : 'Birthday message goes here';
}

async function createJob(event) {
  event.preventDefault();
  ensurePairCode();

  const submitButton = form.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  submitButton.textContent = 'Preparing job…';

  try {
    const photoDataUrl = await fileToDataUrl(photoInput.files[0]);
    const payload = {
      pairCode: pairCodeInput.value.trim().toUpperCase(),
      grandmaName: $('#grandmaName').value,
      sisterName: $('#sisterName').value,
      sisterPhone: $('#sisterPhone').value,
      birthdayMessage: $('#birthdayMessage').value,
      photoDataUrl
    };

    const response = await fetch('/api/jobs', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await response.json();
    if (!result.ok) throw new Error(result.error || 'Could not create job.');

    const helperUrl = `${location.origin}/helper.html?pair=${encodeURIComponent(result.job.pairCode)}`;
    createdText.innerHTML = `Job ready. Open the Android Worker APK with pair code <strong>${result.job.pairCode}</strong>. Browser helper is also available for testing.`;
    $('#helperLink').href = helperUrl;
    createdBox.classList.remove('hidden');
  } catch (error) {
    alert(error.message || 'Could not create job.');
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = 'Send job to Android Worker';
  }
}

if (form) {
  ensurePairCode();
  form.addEventListener('submit', createJob);
  form.addEventListener('input', refreshPreview);
}

if (photoInput && avatar) {
  photoInput.addEventListener('change', async () => {
    const url = await fileToDataUrl(photoInput.files[0]);
    if (!url) return;
    avatar.textContent = '';
    avatar.style.backgroundImage = `url(${url})`;
  });
}

refreshPreview();
