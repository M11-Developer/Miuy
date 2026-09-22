package main

// Shared HTTP handlers used by every native platform (Windows, Linux, macOS).
// They are intentionally free of build tags so behaviour cannot drift per OS.

import (
	"encoding/json"
	"net/http"
)

// ownerVerifyHandler checks a PIN sent from the UI against the build-time hash.
// Safety: the PIN is never logged, never stored, and never echoed back.
func ownerVerifyHandler(logger *eventLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "POST required", http.StatusMethodNotAllowed)
			return
		}
		var req struct {
			PIN string `json:"pin"`
		}
		if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 4096)).Decode(&req); err != nil {
			http.Error(w, "invalid request", http.StatusBadRequest)
			return
		}
		ok, err := VerifyOwnerPIN(req.PIN)
		if err != nil {
			logger.Event("owner_verify_error", map[string]any{"error": err.Error()})
			http.Error(w, err.Error(), http.StatusForbidden)
			return
		}
		if !ok {
			// Log the attempt only, never the supplied PIN.
			logger.Event("owner_verify_failed", map[string]any{"length": len(req.PIN)})
			http.Error(w, "invalid PIN", http.StatusUnauthorized)
			return
		}
		logger.Event("owner_verify_ok", nil)
		_ = json.NewEncoder(w).Encode(map[string]bool{"ok": true})
	}
}

// ownerStatusHandler exposes non-sensitive owner build metadata only.
func ownerStatusHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(OwnerStatus())
	}
}
