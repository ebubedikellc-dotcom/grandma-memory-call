# Grandma Memory Worker APK Source

This folder is the Android app source. Build it with CodeMagic or Android Studio to get the APK that installs on the Android phone.

## What the APK does

- Saves the Render control-panel URL.
- Saves the pair code from the Render site.
- Checks Render every 5 seconds for the latest birthday call job.
- Shows Grandma's picture, the birthday person, the phone number, and the message.
- Opens WhatsApp or WhatsApp Business with the message ready.
- Opens the same chat so the user can tap WhatsApp video call.

## What it cannot do by itself

Android does not allow a normal APK to silently replace WhatsApp's camera with a generated Grandma video. That needs a deeper camera/virtual-camera engine. This Worker is the phone controller for the WhatsApp message and call flow.

## Build with CodeMagic

1. Upload the whole project folder to GitHub.
2. Open CodeMagic and add the GitHub repo.
3. Use the `codemagic.yaml` in the project root.
4. Start the `android-debug-apk` workflow.
5. Download the APK from the build artifacts.

After installing the APK, enter your Render site URL and the same pair code shown on the control panel.
