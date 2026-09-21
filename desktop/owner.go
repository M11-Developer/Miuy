package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"hash"
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
	ownerVersion string = "1.2.0"
)

// IsOwnerBuild reports whether this binary was built as Owner Edition.
func IsOwnerBuild() bool {
	return ownerHash != "" && ownerSalt != ""
}

// pbkdf2Key implements PBKDF2 with HMAC-SHA256, no external deps.
// This is a minimal implementation for Owner PIN hashing.
func pbkdf2Key(password, salt []byte, iter, keyLen int, h func() hash.Hash) []byte {
	prf := hmac.New(h, password)
	hashLen := prf.Size()
	numBlocks := (keyLen + hashLen - 1) / hashLen

	var buf [4]byte
	dk := make([]byte, 0, numBlocks*hashLen)
	U := make([]byte, hashLen)

	for block := 1; block <= numBlocks; block++ {
		prf.Reset()
		prf.Write(salt)
		buf[0] = byte(block >> 24)
		buf[1] = byte(block >> 16)
		buf[2] = byte(block >> 8)
		buf[3] = byte(block)
		prf.Write(buf[:])
		dk = prf.Sum(dk)
		T := dk[len(dk)-hashLen:]
		copy(U, T)

		for n := 2; n <= iter; n++ {
			prf.Reset()
			prf.Write(U)
			U = U[:0]
			U = prf.Sum(U)
			for x := range U {
				T[x] ^= U[x]
			}
		}
	}
	return dk[:keyLen]
}

// hashPINWithSalt computes PBKDF2-SHA256 with 120k iterations, 32 bytes.
func hashPINWithSalt(pin string, salt []byte) []byte {
	return pbkdf2Key([]byte(pin), salt, 120000, 32, sha256.New)
}

// VerifyOwnerPIN checks a user supplied PIN against the embedded hash.
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
	// constant time compare
	if len(computed) != len(expected) {
		return false, nil
	}
	var diff byte
	for i := 0; i < len(computed); i++ {
		diff |= computed[i] ^ expected[i]
	}
	return diff == 0, nil
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
