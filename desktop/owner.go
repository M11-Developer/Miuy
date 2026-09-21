package main

import (
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"errors"

	"golang.org/x/crypto/pbkdf2"
)

// Build-time injected values via ldflags:
// -X main.ownerHash=<hex>
// -X main.ownerSalt=<hex>
// -X main.ownerBuildID=<id>
// -X main.ownerVersion=<version>
var (
	ownerHash    string = ""
	ownerSalt    string = ""
	ownerBuildID string = ""
	ownerVersion string = "1.1.0"
)

// IsOwnerBuild reports whether this binary was built as Owner Edition.
func IsOwnerBuild() bool {
	return ownerHash != "" && ownerSalt != ""
}

// hashPINWithSalt computes PBKDF2-SHA256 with 120k iterations, 32 bytes.
func hashPINWithSalt(pin string, salt []byte) []byte {
	return pbkdf2.Key([]byte(pin), salt, 120000, 32, sha256.New)
}

// VerifyOwnerPIN checks a user supplied PIN against the embedded hash.
// Returns false if this is not an Owner build.
func VerifyOwnerPIN(pin string) (bool, error) {
	if !IsOwnerBuild() {
		return false, errors.New("owner panel disabled in public build")
	}
	if len(pin) < 4 || len(pin) > 128 {
		return false, errors.New("invalid PIN length")
	}
	salt, err := hex.DecodeString(ownerSalt)
	if err != nil || len(salt) < 16 {
		return false, errors.New("invalid owner salt")
	}
	expected, err := hex.DecodeString(ownerHash)
	if err != nil || len(expected) != 32 {
		return false, errors.New("invalid owner hash")
	}
	computed := hashPINWithSalt(pin, salt)
	if subtle.ConstantTimeCompare(computed, expected) == 1 {
		return true, nil
	}
	return false, nil
}

// OwnerStatus returns safe public info about owner build (no secrets).
func OwnerStatus() map[string]any {
	return map[string]any{
		"isOwnerBuild": IsOwnerBuild(),
		"buildID":      ownerBuildID,
		"version":      ownerVersion,
		"hasHash":      ownerHash != "",
	}
}

// GenerateOwnerHash is used by build tools (not in runtime) to create hash/salt from PIN.
// It returns hex-encoded salt and hash.
func GenerateOwnerHash(pin string) (saltHex string, hashHex string, err error) {
	if len(pin) < 4 {
		return "", "", errors.New("PIN too short")
	}
	// In real build, salt is random. This helper is for tooling; caller should provide random salt.
	// For deterministic testing, we use SHA256(pin+time) style? Actually tool will generate random.
	return "", "", errors.New("use tools/gen-owner-hash.go for generation")
}
