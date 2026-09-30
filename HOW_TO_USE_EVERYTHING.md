# How to Use Everything

## What you are hosting

You are using **GitHub + Render**.

- **GitHub** stores this full package.
- **Render** runs the control panel and API.
- **Android APK Worker** is installed on your own Android phone.
- **WhatsApp** sends the birthday message and makes the call.
- **Your sister installs nothing**. She only receives a normal WhatsApp message and call.

## Step 1 - Upload to GitHub

Upload all files in this folder to one GitHub repository.

## Step 2 - Deploy the Render site

1. Go to Render.
2. Create a new **Web Service** from the GitHub repository.
3. Use these settings:
   - Build command: `npm install`
   - Start command: `npm start`
4. After deploy, copy the Render URL. It will look like:
   - `https://your-app-name.onrender.com`

## Step 3 - Download the Android APK

GitHub builds it automatically:

1. Open the GitHub repository.
2. Open **Actions** and wait for **Build Android APK** to show a green check.
3. Open **Releases** on the repository page.
4. Open **Grandma Memory Worker APK**.
5. Download `Grandma-Memory-Worker.apk`.
6. Install it on the Android phone that will make the WhatsApp call. Android may ask you to allow installs from your browser or Files app.

CodeMagic is included only as a backup APK builder.

## Step 4 - Prepare WhatsApp on the Android phone

1. Install WhatsApp or WhatsApp Business.
2. Log in to the WhatsApp number you want to call from.
3. Save your sister's number if you want, but it is not required.

## Step 5 - Create the birthday call job

1. Open the Render URL.
2. Copy or keep the pair code shown on the control panel.
3. Enter:
   - Grandma name
   - Sister name
   - Sister WhatsApp number with country code
   - Grandma picture
   - Birthday message
4. Press **Send job to Android worker**.

## Step 6 - Use the Android APK Worker

1. Open the APK on your Android phone.
2. Paste the Render URL.
3. Enter the same pair code.
4. Tap **Save + Load**.
5. The app shows Grandma picture, sister phone number, and the birthday message.
6. Tap **Open WhatsApp with Message**.
7. Send the message inside WhatsApp.
8. Tap the WhatsApp video-call button.

## Plain truth about the Grandma moving face

This package correctly connects **Render + Android + WhatsApp message/call flow**.

The part where WhatsApp camera becomes a live moving Grandma face is not something a normal APK can force on ordinary Android by itself. For that exact final effect, the next build needs a special video/camera engine. The practical first working version is:

**Control panel creates the job -> APK receives it -> WhatsApp opens -> message is sent -> video call starts.**
