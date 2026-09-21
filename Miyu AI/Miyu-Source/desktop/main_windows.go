//go:build windows

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
	"runtime"
	"strings"
	"sync"
	"time"
	"unsafe"

	webview "github.com/jchv/go-webview2"
	"golang.org/x/sys/windows"
)

//go:embed ui.html
var ui []byte

var user32 = windows.NewLazySystemDLL("user32.dll")
var dataMu sync.Mutex

func alert(text string) {
	title, _ := windows.UTF16PtrFromString("Miyu — a little company")
	body, _ := windows.UTF16PtrFromString(text)
	user32.NewProc("MessageBoxW").Call(0, uintptr(unsafe.Pointer(body)), uintptr(unsafe.Pointer(title)), 0x40)
}

func main() {
	runtime.LockOSThread()
	// Best-effort per-monitor DPI awareness. The manifest also requests this.
	dpi := user32.NewProc("SetProcessDpiAwarenessContext")
	if dpi.Find() == nil {
		dpi.Call(^uintptr(3))
	}

	configRoot := os.Getenv("LOCALAPPDATA")
	if configRoot == "" {
		configRoot, _ = os.UserConfigDir()
	}
	if configRoot == "" {
		alert("Miyu could not find a writable user data folder.")
		return
	}
	dataDir := filepath.Join(configRoot, "Miyu")
	if err := os.MkdirAll(dataDir, 0700); err != nil {
		alert("Miyu could not create its local data folder.")
		return
	}
	stateFile := filepath.Join(dataDir, "state.json")

	tokenBytes := make([]byte, 32)
	if _, err := rand.Read(tokenBytes); err != nil {
		alert("Miyu could not create a secure local session.")
		return
	}
	token := hex.EncodeToString(tokenBytes)
	// Only this native app's local window uses the loopback server.
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		alert("Miyu could not open its local interface.")
		return
	}
	origin := "http://" + listener.Addr().String()
	host := listener.Addr().String()
	mux := http.NewServeMux()
	mux.HandleFunc("/api/chat", chatHandler(token, origin))
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
		w.Header().Set("Permissions-Policy", "camera=(self), microphone=(self), geolocation=(), display-capture=()")
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
		ReadHeaderTimeout: 5 * time.Second, IdleTimeout: 30 * time.Second,
	}
	go func() { _ = server.Serve(listener) }()
	defer func() {
		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		defer cancel()
		_ = server.Shutdown(ctx)
	}()

	screenW, _, _ := user32.NewProc("GetSystemMetrics").Call(0)
	screenH, _, _ := user32.NewProc("GetSystemMetrics").Call(1)
	width, height := uint(1440), uint(940)
	if screenW > 300 && screenW < 1510 {
		width = uint(screenW) - 70
	}
	if screenH > 400 && screenH < 1025 {
		height = uint(screenH) - 85
	}
	w := webview.NewWithOptions(webview.WebViewOptions{
		Debug: false, AutoFocus: true, DataPath: filepath.Join(dataDir, "WebView2"),
		WindowOptions: webview.WindowOptions{Title: "Miyu — a little company, a little magic", Width: width, Height: height, Center: true, IconId: 1},
	})
	if w == nil {
		alert("Miyu needs the Microsoft Edge WebView2 Runtime. It is normally installed with Windows 10 and 11.\n\nInstall it from Microsoft’s official WebView2 website, or use the included Miyu-Portable.html browser edition.\n\nSee START HERE.txt for details.")
		return
	}
	defer w.Destroy()
	w.SetSize(380, 520, webview.HintMin)
	jsToken, _ := json.Marshal(token)
	w.Init("window.__MIYU_DESKTOP__=true;window.__MIYU_API_TOKEN__=" + string(jsToken) + ";")
	_ = w.Bind("miyuLoad", func() (string, error) {
		dataMu.Lock()
		defer dataMu.Unlock()
		raw, err := os.ReadFile(stateFile)
		if os.IsNotExist(err) {
			return "", nil
		}
		if err != nil {
			return "", errors.New("local data could not be read")
		}
		if len(raw) > 4*1024*1024 {
			return "", errors.New("local state is too large")
		}
		return string(raw), nil
	})
	_ = w.Bind("miyuSave", func(data string) error {
		if len(data) > 4*1024*1024 {
			return errors.New("local state is too large")
		}
		var state map[string]json.RawMessage
		if json.Unmarshal([]byte(data), &state) != nil {
			return errors.New("invalid local state")
		}
		// The UI never persists API credentials. Defense in depth if a future UI accidentally does.
		delete(state, "apiKey")
		delete(state, "key")
		delete(state, "token")
		if raw, ok := state["connection"]; ok {
			var config map[string]json.RawMessage
			if json.Unmarshal(raw, &config) == nil {
				delete(config, "apiKey")
				delete(config, "key")
				delete(config, "token")
				state["connection"], _ = json.Marshal(config)
			}
		}
		encoded, err := json.Marshal(state)
		if err != nil {
			return err
		}
		dataMu.Lock()
		defer dataMu.Unlock()
		temporary := stateFile + ".tmp"
		if err := os.WriteFile(temporary, encoded, 0600); err != nil {
			return errors.New("local data could not be saved")
		}
		if err := os.Rename(temporary, stateFile); err != nil {
			return errors.New("local data could not be replaced")
		}
		return nil
	})
	_ = w.Bind("miyuWindow", func(action string, enabled bool) error {
		switch strings.ToLower(action) {
		case "pin":
			after := ^uintptr(1) // HWND_NOTOPMOST (-2)
			if enabled {
				after = ^uintptr(0)
			} // HWND_TOPMOST (-1)
			ok, _, _ := user32.NewProc("SetWindowPos").Call(uintptr(w.Window()), after, 0, 0, 0, 0, 0x0001|0x0002|0x0010)
			if ok == 0 {
				return errors.New("window position could not be updated")
			}
		case "compact":
			if enabled {
				w.SetSize(450, 660, webview.HintNone)
			} else {
				w.SetSize(int(width), int(height)-40, webview.HintNone)
			}
		default:
			return fmt.Errorf("unknown window action")
		}
		return nil
	})
	w.Navigate(origin + "/")
	w.Run()
}
