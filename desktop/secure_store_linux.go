//go:build linux

package main

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"io"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

// Linux secure storage: XDG-compliant, 0600 permissions, AES-GCM encrypted.
// Key is derived from machine-id + user id + app salt (not perfect as DPAPI but local encrypted).

type secureVault struct {
	path       string
	legacyPath string
	mu         sync.Mutex
}

func newSecureVault(path string) *secureVault {
	return &secureVault{
		path:       path,
		legacyPath: strings.TrimSuffix(path, ".bin") + ".json",
	}
}

func deriveLinuxKey() ([]byte, error) {
	// Try machine-id locations
	var machineID string
	for _, p := range []string{"/etc/machine-id", "/var/lib/dbus/machine-id"} {
		if b, err := os.ReadFile(p); err == nil {
			machineID = strings.TrimSpace(string(b))
			if machineID != "" {
				break
			}
		}
	}
	if machineID == "" {
		machineID = "miyu-fallback-machine-id"
	}
	uid := os.Getenv("USER")
	if uid == "" {
		uid = "default-user"
	}
	// Key = SHA256(machineID + uid + fixed app pepper)
	pepper := "MiyuLinuxPepper2026-v1"
	sum := sha256.Sum256([]byte(machineID + "|" + uid + "|" + pepper))
	return sum[:], nil
}

func (v *secureVault) Read() ([]byte, error) {
	v.mu.Lock()
	defer v.mu.Unlock()

	raw, err := os.ReadFile(v.path)
	if os.IsNotExist(err) {
		// Try legacy plaintext JSON once
		raw, err = os.ReadFile(v.legacyPath)
		if os.IsNotExist(err) {
			return nil, nil
		}
		if err == nil && len(raw) > 0 && raw[0] == '{' {
			return raw, nil
		}
		return nil, nil
	}
	if err != nil {
		return nil, errors.New("local data could not be read")
	}
	if len(raw) == 0 {
		return nil, nil
	}
	// If plaintext JSON (migration), return as-is
	if raw[0] == '{' {
		return raw, nil
	}
	// Decrypt AES-GCM: nonce (12) + ciphertext
	if len(raw) < 13 {
		return nil, errors.New("local data corrupted")
	}
	key, err := deriveLinuxKey()
	if err != nil {
		return nil, err
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, errors.New("decryption failed")
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, errors.New("decryption failed")
	}
	nonce := raw[:gcm.NonceSize()]
	ciphertext := raw[gcm.NonceSize():]
	plain, err := gcm.Open(nil, nonce, ciphertext, nil)
	if err != nil {
		return nil, errors.New("local data could not be decrypted for this account")
	}
	return plain, nil
}

func (v *secureVault) Write(data []byte) error {
	v.mu.Lock()
	defer v.mu.Unlock()

	key, err := deriveLinuxKey()
	if err != nil {
		return err
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return errors.New("encryption failed")
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return errors.New("encryption failed")
	}
	nonce := make([]byte, gcm.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return errors.New("encryption failed")
	}
	sealed := gcm.Seal(nonce, nonce, data, nil)

	// Ensure directory exists with 0700
	dir := filepath.Dir(v.path)
	if err := os.MkdirAll(dir, 0700); err != nil {
		return errors.New("could not create data dir")
	}
	tmp := v.path + ".tmp"
	if err := os.WriteFile(tmp, sealed, 0600); err != nil {
		return errors.New("local data could not be saved")
	}
	if err := os.Rename(tmp, v.path); err != nil {
		_ = os.Remove(tmp)
		return errors.New("local data could not be replaced")
	}
	// Log file should also be 0600, but handled elsewhere
	return nil
}

// Linux XDG paths helper
func getLinuxDataDir() (string, error) {
	if xdg := os.Getenv("XDG_DATA_HOME"); xdg != "" {
		return filepath.Join(xdg, "miyu"), nil
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(home, ".local", "share", "miyu"), nil
}

func getLinuxConfigDir() (string, error) {
	if xdg := os.Getenv("XDG_CONFIG_HOME"); xdg != "" {
		return filepath.Join(xdg, "miyu"), nil
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(home, ".config", "miyu"), nil
}

func getLinuxLogPath(dataDir string) string {
	return filepath.Join(dataDir, "miyu.log")
}

func ensureSecurePermissions(path string) {
	_ = os.Chmod(path, 0600)
	_ = hex.EncodeToString // keep import used
}
