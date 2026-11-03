# Fingerprint Anim (LSPosed module)

Replaces the on-screen fingerprint animation on a rooted Samsung phone (KernelSU + LSPosed).
Three built-in original presets (ripple rings, glow burst, aurora sweep) plus a custom folder
for your own animation (Lottie `animation.json`, or PNG/WebP frames).

**Status: written without being compiled or tested.** Expect to tweak it on first run.

## Build (no Android Studio needed)
1. Create a free GitHub repo and push this folder to it:
   ```
   git init && git add . && git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/YOU/REPO.git
   git push -u origin main
   ```
2. Open the repo's **Actions** tab, open the "Build APK" run, and download the
   `FingerprintAnim-apk` artifact (a zip containing `app-debug.apk`).
   The APK is signed with the standard debug key, which is fine for installing.

## Install
1. Install the APK, open **LSPosed > Modules > Fingerprint Anim**, enable it.
2. Scope: tick **System UI** and **Biometrics** (`com.samsung.android.biometrics.app.setting`).
   Samsung may draw the in-display animation in either one.
3. Open the module app, pick a style, press Save, then use LSPosed's **Soft reboot**.

## Custom animation
Put files in `/sdcard/FingerprintAnim/` (or change the folder in the app):
- `animation.json` for a Lottie animation, or
- `frame_000.png`, `frame_001.png`, ... (zero-padded so they sort correctly, max 150 frames).
Pick **Custom folder** in the app. If the hooked process can't read the folder (SELinux),
the log says so; try a different folder such as `/data/local/tmp/fpanim`.

## If nothing changes (likely on the first try)
Samsung's resource names differ per One UI version. With debug logging on, open LSPosed >
Logs and filter for `FPAnim`. You'll see lines like `saw resource: <name>`. Put a regex that
matches the real animation resource into **Resource name pattern** and soft reboot again.
If no matching resource appears at all, the animation is drawn in code or native code and the
resource hook can't reach it; send me the log and the app's decompiled class names and I'll
write a targeted hook.
