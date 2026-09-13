# OPPO Screen Share (Android 5.1 + Windows)

This project is a **screen-only** LAN streamer. The PC never sends mouse/keyboard control to the phone.

## Important
This is a source project, not a prebuilt APK. You need Android Studio/Gradle to build the APK.

## PC setup
1. Install Python 3 on Windows.
2. Open CMD in this folder.
3. Run:
   `pip install qrcode[pil]`
4. Run:
   `python pc_server.py`
5. The script creates `opposhare_qr.png`. Open it on the PC.
6. On the phone, install the built APK and scan that QR code using the app.
7. The Android screen-capture permission appears. Tap Allow.
8. Open `http://127.0.0.1:8765` on the PC to view the stream.

### If the QR uses the wrong Wi-Fi IP
Run `ipconfig` in Windows, find the **IPv4 Address** of the Wi-Fi adapter, and replace the fallback `192.168.1.100` in `pc_server.py` if necessary.

### Firewall
Allow Python through Windows Firewall on Private networks when prompted.

## Build
Open the folder in Android Studio. Let Gradle sync, then Build > Build APK(s).
Minimum Android version is 5.0 (API 21), so Android 5.1 is supported.

## Limitations
- Wi-Fi only; phone and PC must be on the same LAN.
- Video is JPEG/MJPEG, so quality and frame rate are modest.
- No audio.
- No PC control.
- Android system UI and protected DRM content may be restricted by Android.
