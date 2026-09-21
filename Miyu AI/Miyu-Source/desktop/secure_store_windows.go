//go:build windows

package main

// The WebView profile and local conversation are user data. Windows DPAPI
// encrypts the small state file with the current Windows user's key, so an
// exported state file cannot be read on another account or machine.

import (
	"errors"
	"fmt"
	"os"
	"syscall"
	"strings"
	"unsafe"
)

type dpapiBlob struct {
	cbData uint32
	pbData *byte
}

var (
	crypt32      = syscall.NewLazyDLL("crypt32.dll")
	cryptProtect = crypt32.NewProc("CryptProtectData")
	cryptUnprotect = crypt32.NewProc("CryptUnprotectData")
	kernel32     = syscall.NewLazyDLL("kernel32.dll")
	localFree    = kernel32.NewProc("LocalFree")
)

func dpapiTransform(proc *syscall.LazyProc, input []byte) ([]byte, error) {
	if len(input) == 0 {
		return nil, errors.New("empty protected data")
	}
	in := dpapiBlob{cbData: uint32(len(input)), pbData: &input[0]}
	var out dpapiBlob
	ok, _, callErr := proc.Call(uintptr(unsafe.Pointer(&in)), 0, 0, 0, 0, uintptr(unsafe.Pointer(&out)))
	if ok == 0 {
		if callErr != syscall.Errno(0) {
			return nil, fmt.Errorf("Windows data protection failed: %w", callErr)
		}
		return nil, errors.New("Windows data protection failed")
	}
	if out.pbData == nil || out.cbData == 0 {
		return nil, errors.New("Windows returned empty protected data")
	}
	defer localFree.Call(uintptr(unsafe.Pointer(out.pbData)))
	result := unsafe.Slice(out.pbData, out.cbData)
	copyOfResult := make([]byte, len(result))
	copy(copyOfResult, result)
	return copyOfResult, nil
}

type secureVault struct{ path, legacyPath string }

func newSecureVault(path string) *secureVault {
	return &secureVault{path: path, legacyPath: strings.TrimSuffix(path, ".bin") + ".json"}
}

func (v *secureVault) Read() ([]byte, error) {
	raw, err := os.ReadFile(v.path)
	if os.IsNotExist(err) {
		// Read the previous prototype location once; the next save writes the
		// migrated data to the encrypted .bin file.
		raw, err = os.ReadFile(v.legacyPath)
	}
	if os.IsNotExist(err) {
		return nil, nil
	}
	if err != nil {
		return nil, errors.New("local data could not be read")
	}
	plain, err := dpapiTransform(cryptUnprotect, raw)
	if err == nil {
		return plain, nil
	}
	// One-time migration for the prototype's old plaintext state.json. A
	// successful next save replaces it with DPAPI-protected bytes.
	if len(raw) > 0 && raw[0] == '{' {
		return raw, nil
	}
	return nil, errors.New("local data could not be decrypted for this Windows account")
}

func (v *secureVault) Write(data []byte) error {
	sealed, err := dpapiTransform(cryptProtect, data)
	if err != nil {
		return errors.New("local data could not be encrypted")
	}
	temporary := v.path + ".tmp"
	if err := os.WriteFile(temporary, sealed, 0600); err != nil {
		return errors.New("local data could not be saved")
	}
	if err := os.Rename(temporary, v.path); err != nil {
		_ = os.Remove(temporary)
		return errors.New("local data could not be replaced")
	}
	return nil
}
