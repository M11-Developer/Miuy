# Miyu Windows — Primary Product

Miyu.exe is the primary product, not Portable HTML.

## Installation

1. Extract Miyu-Windows.zip to a folder (e.g., Desktop/Miyu)
2. Double-click Miyu.exe
3. Requires Microsoft Edge WebView2 Runtime (normally on Win10/11)
   - If missing: https://developer.microsoft.com/en-us/microsoft-edge/webview2/
4. No admin needed, no startup service installed

Unsigned prototype: Windows may show unknown publisher warning. Only run software you trust. Source in Miyu-Source.zip.

## Features (Native)

- WebView2 + Go backend + embedded UI (no external files after build)
- Always on top (pin icon)
- Mini Companion (window icon)
- System Tray (future: right-click menu)
- Global Hotkeys (planned: Ctrl+Shift+M for mute, etc.)
- Local encrypted state: %LOCALAPPDATA%\Miyu\state.bin (DPAPI)
- Logs: %LOCALAPPDATA%\Miyu\miyu.log (redacted, 0600)
- WebView2 profile: %LOCALAPPDATA%\Miyu\WebView2
- Bounded worker pool (2), timeouts, crash recovery
- Loopback bridge 127.0.0.1 only, token + origin validation

## Build from source (Windows)

```sh
npm ci
npm run build
npm run build:portable
cd desktop
go run ./resourcegen
go build -trimpath -ldflags="-s -w -H windowsgui" -o ../Miyu.exe .
```

Cross-compile from Linux:
```sh
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags='-s -w -H windowsgui' -o ../Miyu.exe .
```

Verify PE32+:
```sh
file Miyu.exe
# Should show: PE32+ executable (GUI) x86-64, for MS Windows
```

Create ZIP:
```sh
zip Miyu-Windows.zip Miyu.exe Miyu-Portable.html README.md LICENSE
sha256sum Miyu.exe Miyu-Windows.zip > SHA256SUMS.txt
```

## Troubleshooting

- WebView2 missing: Install from Microsoft, or use Portable HTML
- State too large: Reset via Settings & privacy
- Camera/mic: Starts off, opt-in, local preview only
- Always-on-top: Pin icon, uses SetWindowPos HWND_TOPMOST
- Mini: 450x660 window

## Security

- No external network on startup
- Model proxy binds 127.0.0.1 random port, checks Host, Origin, X-Miyu-Token
- Blocks non-HTTPS remote, no redirect follow
- API keys session-only, never stored
- No camera/mic auto-start
- DPAPI encrypts state.bin
- Atomic writes

## Portable Fallback

Miyu-Portable.html is self-contained, no network needed for offline features. Browser may restrict camera, dictation, API calls. Use Windows native for CORS-free local model.

## Owner Edition (Windows)

Private build with Owner PIN hash (PBKDF2). See OWNER-ADMIN-GUIDE.txt (private, not in public release).

Public build has Owner Panel disabled.
