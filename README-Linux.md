# Miyu Linux — Native

Linux native, not just Portable HTML.

## Packages

- Miyu-Linux-x86_64.AppImage (universal)
- Miyu-Linux-x86_64.deb (Debian/Ubuntu)
- Future: arm64, rpm

## Requirements

- GTK 3
- WebKitGTK 4.1 (libwebkit2gtk-4.1-0)
- libayatana-appindicator3 for tray (optional)

```bash
# Ubuntu/Debian
sudo apt install libgtk-3-0 libwebkit2gtk-4.1-0
```

## Install & Run

AppImage:
```bash
chmod +x Miyu-Linux-x86_64.AppImage
./Miyu-Linux-x86_64.AppImage
```

deb:
```bash
sudo dpkg -i Miyu-Linux-x86_64.deb
miyu
```

## Wayland vs X11

Miyu uses WebKitGTK which works on both Wayland and X11.

Differences:
- **X11**: Full always-on-top support via window manager hints, system tray stable, drag & drop smooth
- **Wayland**: Always-on-top depends on compositor (e.g., GNOME may not allow apps to force top, user must pin via WM), system tray via StatusNotifier, some compositors require extension for tray
- No fake button: If WM doesn't support always-on-top, UI shows "Not supported by compositor" instead of pretending

Tested on:
- X11: GNOME Xorg, KDE X11 — full features
- Wayland: GNOME Wayland, KDE Wayland — tray works, always-on-top best-effort

## Storage & Security

- Data dir: $XDG_DATA_HOME/miyu or ~/.local/share/miyu
- Config dir: $XDG_CONFIG_HOME/miyu or ~/.config/miyu
- State: state.bin (AES-GCM encrypted, key derived from machine-id + user, 0600)
- Logs: miyu.log (0600, redacted, no chat text or API keys)
- No 0777 files

## Features

- GTK/WebKitGTK WebView
- System Tray (where supported)
- Always-on-top (where WM supports)
- Mini Companion (resize)
- Drag & drop character (pointer tracking)
- Keyboard shortcuts (M mute, C camera, F focus, ? help, Esc close)
- XDG compliant
- Encrypted secrets
- Bounded worker pool, timeouts, crash recovery

## Build from source (Linux)

```bash
npm ci
npm run build
npm run build:portable
cd desktop
go mod tidy
go build -trimpath -ldflags="-s -w" -o ../Miyu-Linux-x86_64 .
```

AppImage:
```bash
# Install appimagetool
wget https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage
chmod +x appimagetool
# Create AppDir (see workflow public-linux-release.yml)
./appimagetool AppDir Miyu-Linux-x86_64.AppImage
```

deb:
```bash
dpkg-deb --build deb Miyu-Linux-x86_64.deb
```

## Logs & Debug

```bash
cat ~/.local/share/miyu/miyu.log
# JSON lines, no secrets
```

## Troubleshooting

- WebKitGTK missing: install libwebkit2gtk-4.1-0
- Tray not showing: install libayatana-appindicator3, or check compositor supports StatusNotifier
- Always-on-top not working: Wayland compositor may block; use WM's pin feature
- State decryption fails: machine-id changed? Reset via Settings

## Security Notes

- Binds 127.0.0.1 only
- Origin + token validation
- HTTPS only for remote endpoints
- No camera/mic auto-start
- 0600 permissions

## SHA256

Check SHA256SUMS.txt

## Future

- arm64 build (needs runner)
- rpm package
- Flatpak
