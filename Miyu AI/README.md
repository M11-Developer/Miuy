# Miyu Hoshino
### A little company. A little magic.

An animated, local-first companion inspired by your pink-and-black catgirl reference. Miyu is a fictional adult character, age 23: warm-hearted, curious, and a little playful.

**This is a polished interactive prototype, not a finished commercial AAA game.** It uses original AI-generated 2D character art, a deformable WebGL mesh, custom expression frames, and prerecorded synthetic voice reactions. It is not a 3D model or a Live2D rig.

## Open it on your PC

### Windows 10 / 11, 64-bit
1. Extract the entire `Miyu-Windows.zip` archive into a folder of your choice.
2. Open **Miyu.exe**. No Node, Python, Go, or app installation is required to run it.
3. The app uses **Microsoft Edge WebView2**, normally available on Windows 10/11. If the runtime is missing, install it from Microsoft's official site: https://developer.microsoft.com/en-us/microsoft-edge/webview2/ . Alternatively, open the included browser edition.

The executable is an **unsigned personal prototype**. Windows may display an unknown-publisher warning. Only run software you trust; the complete app source is supplied in `Miyu-Source.zip`. This does not require administrator privileges and does not install a startup service.

**Testing limit:** the Windows executable was cross-compiled and its PE/GUI structure validated in a Linux workspace. It has not been run on a physical Windows PC here. The web UI, privacy state transitions, portable file, and desktop model proxy were tested separately; see `TESTING.md`.

### Browser fallback / macOS / Linux
Open **Miyu-Portable.html** in a recent Chrome, Edge, Firefox, or Safari browser. It is one self-contained file, with all imagery, fonts, icons, voice clips, and code embedded. An Internet connection is not needed for the offline features.

A browser may restrict camera, dictation, or API calls depending on permissions and origin policies. Use the Windows native app for native always-on-top and CORS-free local-model connections. For microphone input where browser recognition is unavailable, focus the chat box and use your operating system's dictation (on Windows: **Win + H**).

## What is included

- Animated idle breathing, gentle body/hair motion, ear twitches, blinking, speech mouth movement, cursor-following, and reactive blush.
- Greetings, head-pat reactions, encouragement, a happy dance, focus companionship, and a guided breathing pause.
- Five consistent studio voice clips. Conversation speech can use your system's text-to-speech voices.
- Master mute, volume, opt-in dictation, opt-in camera preview, full-room view, and a mini companion layout.
- Native always-on-top in the Windows app.
- Three scenes: **Sakura room**, **Moonlit garden**, and **Dream studio**.
- Three subtle palettes for the **same signature outfit**: petal pink, soft lilac, and peach cream.
- Character zoom, motion strength, cursor tracking, petals, reduced motion, and light/dark interfaces.
- Locally generated rain and breeze ambience. These are soundscapes, not music tracks.
- A 15/25/50-minute focus timer with pause, task note, reset, and completed-session count.
- Explicitly saved memories, chat and memory export, and local data reset.
- Portrait export. **Camera frames are never included.**
- Clearly marked scripted preview chat, plus optional real AI connections.

No model weights, commercial AI subscription, or API credits are bundled.

## Enable real AI chat

Without a model connection, chat is a small set of **scripted preview replies**, not an AI language model. Everything else above can be tried immediately.

### Option A — Ollama on your PC

This avoids a paid API and can run privately after you download a model.

1. Install Ollama from https://ollama.com/download/windows .
2. Download and start a model in a terminal, for example:
   ```sh
   ollama run qwen3:4b
   ```
   This is a separate, potentially multi-gigabyte download. Choose a model your PC can run; memory requirements and speed depend on the model and your hardware.
3. Leave Ollama running.
4. Open Miyu → **Settings & privacy → AI connection**.
5. Choose **Ollama · your local AI**.
6. Set the address to `http://localhost:11434` and model to `qwen3:4b`, or the exact name of the model you installed.
7. Click **Send a test greeting**. This sends one test message, not your conversation or memories.
8. After a successful test, save the connection and chat normally.

The Windows app connects to Ollama on loopback without browser CORS restrictions. In the standalone browser edition, Ollama must permit that page's origin. Prefer the Windows app instead of broadly enabling arbitrary origins on a local model server.

### Option B — An OpenAI-compatible API

1. Choose **OpenAI-compatible API** in the same settings page.
2. Enter your provider's HTTPS base URL, usually ending in `/v1`.
3. Enter a model available to your account. For OpenAI, the provided example is `gpt-4.1-mini` at `https://api.openai.com/v1`.
4. Paste **your own** API key and send a test greeting.
5. Save the connection. Your provider may charge for requests.

The API must support the Chat Completions request/response format. The prototype sends `temperature` and `max_tokens`; some providers or reasoning-only models may require a different adapter. No live paid API was called during development: compatible API behavior was verified with test servers and browser request mocks.

**Keys are session-only.** The key input is not stored in preferences, source, downloads, or exports. Closing/reloading clears it. You will need to enter it again. Enter keys only into an app build you trust.

## Privacy, precisely

- Camera and microphone are **off at startup**.
- Camera is an opt-in **local mirrored preview**. There is no recording, AI vision, face recognition, emotion detection, or camera-frame upload. Turning it off stops its media tracks. The app also stops media capture when the page becomes hidden or closes.
- The microphone uses browser/OS speech recognition, if available. **That external speech service may process audio online.** A consent dialog explains this before activation. The app itself does not save microphone audio. Dictation only fills the text box; it never sends automatically.
- Studio voice clips and ambient sound work locally. System TTS voice availability varies by OS; some system voices may themselves use online services.
- Preview mode sends no chat requests. There are no app analytics, advertising, accounts, subscriptions, or background AI requests.
- In connected mode, your last conversation messages and explicitly saved memory notes are sent to the **endpoint you choose**, along with a character instruction. Your model provider's privacy and retention policies apply.
- The native HTTP bridge binds only to `127.0.0.1`, checks the exact local origin and a random session token, blocks insecure remote HTTP, and does not follow model-endpoint redirects.
- Chat, memories, and preferences are stored **unencrypted** under your device account. Windows: `%LOCALAPPDATA%\Miyu\state.json`; browser: the page's local storage. They are not synced between the native and browser editions.
- Settings → Privacy & data can reset the local app state. This cannot delete copies already sent to a provider or exported files. Windows' WebView2 profile cache lives in `%LOCALAPPDATA%\Miyu\WebView2`; close Miyu before removing the whole `%LOCALAPPDATA%\Miyu` folder if you also want to remove that runtime profile.

Miyu is a fictional AI character, not a person or a therapist. The interface does not monitor you or infer your real emotions.

## Controls

| Control | What it does |
|---|---|
| Click Miyu / Head pat | Animated reaction and studio voice |
| Sound / **M** | Mute/unmute voice, ambience, and chimes |
| Camera / **C** | Consent prompt, then local preview; press again to stop |
| Mic | Consent prompt, then browser dictation if supported |
| Focus / **F** | Open the focus timer |
| Window icon | Mini companion layout |
| Pin | Always-on-top (Windows native only) |
| Expand | Immersive room view |
| Aperture icon | Export a portrait, without camera video |
| Enter / Shift+Enter | Send / insert a new line |
| **?** | Shortcut guide |
| **Esc** | Close dialog, immersive view, or mini view |

Single-key shortcuts are ignored while you type in a field. Audio ambience does not auto-resume when the app opens again; start it with a click.

## Build and modify the source

The source archive includes the original app code, prepared artwork/audio assets, native wrapper, build tools, and tests. Dependencies are not bundled.

Requirements: Node 20+ / npm for the UI, Go 1.27+ for the exact native build used here. A lower Go version may work after adjusting `go.mod`, but has not been tested.

```sh
npm ci
npm run dev
```

The Vite preview binds to `0.0.0.0:5173`. For a production web bundle:

```sh
npm run build
```

Create the fully embedded HTML and prepare the native UI:

```sh
npm run build:portable
```

On Windows, with Go installed:

```sh
npm run package:win
```

Cross-compile from Linux/macOS:

```sh
npm run build:portable
cd desktop
go run ./resourcegen
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags='-s -w -H windowsgui' -o ../deliverables/Miyu.exe .
```

Tests:

```sh
npx playwright install --with-deps chromium
# Keep the Vite preview running on port 5173 first:
npm test
npm run test:proxy
```

`tools/prepare_faces.py` is an optional art-processing helper (Pillow, NumPy, OpenCV). Prepared face frames are already included; it is not needed to run the app or compile the UI.

## Credits & rights

The character is an original AI-generated adaptation of the reference you supplied. Scenery and studio voice clips are AI-generated; motion, interfaces, and expression-frame processing are implemented in code. No commercial-quality rig or AAA asset pipeline is claimed.

Fonts: Manrope and Lora (SIL Open Font License). Icons: Lucide (ISC, with Feather portions under MIT). Windows wrapper: go-webview2, go-winloader, and Go's Windows support libraries. The WebView2 runtime itself is supplied by Microsoft, not bundled. Required attribution and licenses are in `licenses/` and `THIRD-PARTY-NOTICES.txt`.

Before commercial redistribution, verify rights to the supplied reference and applicable terms for generated assets. Third-party software retains its original licenses.
