//go:build darwin

package main

import (
	"errors"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

// macOS secure storage: similar to Linux, using 0600 + simple file.
// For real keychain integration, would use Security framework, but this is local encrypted via file permissions.

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

func (v *secureVault) Read() ([]byte, error) {
	v.mu.Lock()
	defer v.mu.Unlock()
	raw, err := os.ReadFile(v.path)
	if os.IsNotExist(err) {
		raw, err = os.ReadFile(v.legacyPath)
		if os.IsNotExist(err) {
			return nil, nil
		}
	}
	if err != nil {
		return nil, errors.New("local data could not be read")
	}
	// For darwin prototype, store plaintext but with 0600; DPAPI-like encryption would need keychain.
	// We keep same interface; future could use keychain.
	if len(raw) == 0 {
		return nil, nil
	}
	return raw, nil
}

func (v *secureVault) Write(data []byte) error {
	v.mu.Lock()
	defer v.mu.Unlock()
	dir := filepath.Dir(v.path)
	if err := os.MkdirAll(dir, 0700); err != nil {
		return err
	}
	tmp := v.path + ".tmp"
	if err := os.WriteFile(tmp, data, 0600); err != nil {
		return errors.New("could not save")
	}
	if err := os.Rename(tmp, v.path); err != nil {
		_ = os.Remove(tmp)
		return errors.New("could not replace")
	}
	return nil
}

func getDarwinDataDir() (string, error) {
	home, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(home, "Library", "Application Support", "Miyu"), nil
}
