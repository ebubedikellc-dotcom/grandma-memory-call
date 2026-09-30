# Grandma Memory Call - Render + Android APK Worker

This package is ready for GitHub + Render.

## What it does

- Render hosts the control panel.
- The Android APK Worker is installed on your own Android phone.
- The control panel sends a birthday job to the Android Worker.
- The Android Worker opens WhatsApp with the sister's number and birthday message.
- The birthday write-up is sent as WhatsApp text first.
- The user then starts the WhatsApp video call.
- A browser helper page is included as a backup for testing without the APK.

## Important limit

A normal Render website or ordinary Android APK cannot force WhatsApp Android to start a video call or replace WhatsApp's camera with Grandma's moving face. For that final live effect, you need one of these:

- an Android camera/virtual-camera engine with special support, or
- a computer/VPS engine with WhatsApp Web and a virtual camera.

This package builds the practical Android + Render control layer first.

## Package contents

- `server.js` - Render backend and static website server.
- `public/` - control panel and browser helper fallback.
- `android-worker/` - native Android Worker app source.
- `codemagic.yaml` - CodeMagic workflow to build the APK.
- `HOW_TO_USE_EVERYTHING.md` - simple usage guide.

## Deploy on Render

1. Upload this folder to GitHub.
2. In Render, create a new Web Service from the GitHub repository.
3. Render can use `render.yaml`, or set:
   - Build command: `npm install`
   - Start command: `npm start`
4. Open the Render URL.

## Build the APK automatically on GitHub

1. Push this package to the `main` branch on GitHub.
2. Open the repository's **Actions** tab and wait for **Build Android APK** to finish.
3. Open the repository's **Releases** section.
4. Download `Grandma-Memory-Worker.apk` from **Grandma Memory Worker APK**.
5. Install it on the Android phone that will make the WhatsApp call.

CodeMagic remains included as a backup build service.

## Use Everything

1. Open the Render control panel.
2. Copy the pair code.
3. Enter Grandma picture, sister number, and birthday message.
4. Press **Send job to Android Worker**.
5. Open the APK on the Android phone.
6. Paste the Render URL and pair code.
7. Tap **Save + Load**.
8. Tap **Open WhatsApp with Message**.
9. Send the text message in WhatsApp.
10. Tap WhatsApp video call.

Your sister only receives a normal WhatsApp message and call. She does not install anything.
