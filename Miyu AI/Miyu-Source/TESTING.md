# Miyu 1.0 — verification notes

## Browser UI: 22 automated functional checks
Tested with Playwright Chromium in this Linux workspace.

- Startup: avatar loads, preview mode is explicit, camera and microphone are off.
- No external app network requests on startup.
- Character greeting updates animation/mood and plays its studio clip.
- Master sound toggles correctly.
- Scripted conversation is functional and clearly identified as preview.
- Scene changes work and save locally.
- Palette, tracking, reduced-motion, and light/dark settings work.
- Explicit memories can be created.
- Focus duration, task, start, pause, and reset work.
- Locally generated rain/breeze start and stop.
- Guided breathing starts and can be dismissed.
- Camera requires consent, obtains a video stream, and stops it when switched off.
  This test used Chromium's synthetic camera device, not a physical webcam.
- Microphone disclosure is shown before activation.
- Immersive and mini companion modes enter and exit.
- Portrait export produces a PNG and does not include camera frames.
- Insecure non-loopback API endpoints are rejected.
- A connection test sends only one explicitly requested test greeting.
- Connected chat uses a mock API correctly, includes saved notes, treats returned
  HTML as text, and does not save the API-key input in local storage.
- Reload preserves notes/settings and resets key/media state.
- 390-pixel mobile layout has no horizontal overflow.
- No JavaScript runtime exceptions in the tested flows.
- The self-contained portable HTML loads and reacts with HTTP(S) network blocked.

## Native model bridge: 8 Go tests

- HTTPS/loopback endpoint validation.
- Method, session-token, and origin authorization.
- Compatible Chat Completions request/response handling.
- Ollama handling without forwarding API credentials.
- Model redirects are not followed.
- Credentials are redacted from provider error messages.
- Complete HTTP-handler integration with a local test server.
- Empty model output is an explicit error, not fake success.

These used synthetic/local test servers. No paid model request was made.

## Windows build

- Go cross-compilation to Windows amd64 completed successfully.
- Output identified as a PE32+ x86-64 Windows GUI executable.
- Contains the app icon, version info, a no-elevation manifest, and the embedded UI.
- WebView2 is an external runtime requirement; it is not bundled.

**Not verified in this workspace:** executing the binary on Windows, actual
WebView2 camera/permission dialogs, physical camera/microphone hardware,
Windows always-on-top behavior, real microphone transcription, installed
system TTS voices, or a user's real Ollama/API model. Those depend on the
runtime, OS configuration, hardware, credentials, and provider policy.

The native code implements pinning, window resizing, disk persistence, and a
local authenticated model proxy, but this is not a substitute for Windows QA.
The browser edition is included as an immediate fallback.

## Embedded desktop UI security policy

The embedded HTML was also loaded under the native app's production
Content-Security-Policy in Chromium. Native flags, persistence/window bindings,
and the same-origin model route were mocked. The UI loaded without CSP or
JavaScript errors, called the expected pin/resize bindings, and routed model
requests through the authenticated same-origin bridge. This is a bridge/UI
integration test, not an actual Win32 execution test.
