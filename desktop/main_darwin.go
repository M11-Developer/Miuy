//go:build darwin

// Miyu macOS native shell (Cocoa + WebKit through webview_go).
// Same local HTTP bridge, same encrypted vault, same owner panel rules as Windows/Linux.

package main

import (
	"context"
	"crypto/rand"
	_ "embed"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"time"

	webview "github.com/webview/webview_go"
)

//go:embed ui.html
var ui []byte

var dataMu sync.Mutex

func main() {
	configRoot, err := getDarwinDataDir()
	if err != nil {
		fmt.Fprintln(os.Stderr, "Miyu could not find its Application Support folder")
		return
	}
	if err := os.MkdirAll(configRoot, 0700); err != nil {
		fmt.Fprintln(os.Stderr, "Miyu could not create data dir")
		return
	}
	stateFile := filepath.Join(configRoot, "state.bin")
	vault := newSecureVault(stateFile)
	logger := newEventLogger(filepath.Join(configRoot, "miyu.log"))
	defer logger.Close()
	defer modelWorkers.Close()
	defer func() {
		if r := recover(); r != nil {
			logger.Event("panic_recovered", map[string]any{"message": fmt.Sprint(r)})
			fmt.Fprintln(os.Stderr, "Miyu recovered from internal error")
		}
	}()

	tokenBytes := make([]byte, 32)
	if _, err := rand.Read(tokenBytes); err != nil {
		fmt.Fprintln(os.Stderr, "Could not create secure session")
		return
	}
	token := hex.EncodeToString(tokenBytes)

	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		fmt.Fprintln(os.Stderr, "Could not open local interface")
		return
	}
	origin := "http://" + listener.Addr().String()
	host := listener.Addr().String()

	mux := http.NewServeMux()
	mux.HandleFunc("/api/chat", chatHandler(token, origin, logger))
	mux.HandleFunc("/api/owner/verify", ownerVerifyHandler(logger))
	mux.HandleFunc("/api/owner/status", ownerStatusHandler())
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" || r.Method != http.MethodGet {
			http.NotFound(w, r)
			return
		}
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("Referrer-Policy", "no-referrer")
		w.Header().Set("Content-Security-Policy", "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data: blob:; font-src data:; media-src data: blob:; connect-src 'self'; object-src 'none'; frame-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
		_, _ = w.Write(ui)
	})

	server := &http.Server{
		Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.Host != host {
				http.Error(w, "Host not allowed", http.StatusForbidden)
				return
			}
			mux.ServeHTTP(w, r)
		}),
		ReadHeaderTimeout: 5 * time.Second,
		IdleTimeout:       30 * time.Second,
	}
	go func() { _ = server.Serve(listener) }()
	defer func() {
		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		defer cancel()
		_ = server.Shutdown(ctx)
	}()

	// Webview window
	w := webview.New(false)
	if w == nil {
		fmt.Fprintln(os.Stderr, "Failed to create webview. Miyu needs the macOS Cocoa/WebKit frameworks (they ship with macOS).")
		return
	}
	defer w.Destroy()
	w.SetTitle("Miyu — a little company, a little magic")
	w.SetSize(1440, 940, webview.HintNone)
	w.SetSize(380, 520, webview.HintMin)

	jsToken, _ := json.Marshal(token)
	w.Init("window.__MIYU_DESKTOP__=true;window.__MIYU_MACOS__=true;window.__MIYU_API_TOKEN__=" + string(jsToken) + ";")
	w.Init(fmt.Sprintf("window.__MIYU_OWNER_BUILD__=%v;window.__MIYU_VERSION__=%q;", IsOwnerBuild(), AppVersion))

	_ = w.Bind("miyuLoad", func() (string, error) {
		dataMu.Lock()
		defer dataMu.Unlock()
		raw, err := vault.Read()
		if err != nil {
			return "", err
		}
		if len(raw) > 4*1024*1024 {
			return "", errors.New("local state too large")
		}
		return string(raw), nil
	})
	_ = w.Bind("miyuSave", func(data string) error {
		if len(data) > 4*1024*1024 {
			return errors.New("local state too large")
		}
		var state map[string]json.RawMessage
		if json.Unmarshal([]byte(data), &state) != nil {
			return errors.New("invalid local state")
		}
		delete(state, "apiKey")
		delete(state, "key")
		delete(state, "token")
		if raw, ok := state["connection"]; ok {
			var cfg map[string]json.RawMessage
			if json.Unmarshal(raw, &cfg) == nil {
				delete(cfg, "apiKey")
				delete(cfg, "key")
				delete(cfg, "token")
				state["connection"], _ = json.Marshal(cfg)
			}
		}
		encoded, err := json.Marshal(state)
		if err != nil {
			return err
		}
		dataMu.Lock()
		defer dataMu.Unlock()
		return vault.Write(encoded)
	})
	_ = w.Bind("miyuCheckForUpdates", func() (string, error) {
		ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		defer cancel()
		manifest, err := checkUpdateManifest(ctx, os.Getenv("MIYU_UPDATE_MANIFEST"), AppVersion)
		if err != nil {
			logger.Event("update_error", map[string]any{"configured": os.Getenv("MIYU_UPDATE_MANIFEST") != ""})
			return "", err
		}
		encoded, _ := json.Marshal(manifest)
		return string(encoded), nil
	})
	_ = w.Bind("miyuWindow", func(action string, enabled bool) error {
		// macOS always-on-top / mini companion positioning is handled by the UI layer and
		// logged here. webview_go does not expose NSWindow level changes, so the app degrades
		// gracefully instead of claiming a capability it does not have on this build.
		logger.Event("window_action", map[string]any{"action": action, "enabled": enabled})
		return nil
	})
	_ = w.Bind("miyuOwnerVerify", func(pin string) (string, error) {
		ok, err := VerifyOwnerPIN(pin)
		if err != nil {
			return "", err
		}
		if !ok {
			return "", errors.New("invalid PIN")
		}
		return `{"ok":true}`, nil
	})
	_ = w.Bind("miyuOwnerStatus", func() (string, error) {
		status := OwnerStatus()
		b, _ := json.Marshal(status)
		return string(b), nil
	})

	w.Navigate(origin + "/")
	w.Run()
}
