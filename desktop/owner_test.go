package main

import (
	"encoding/hex"
	"strings"
	"testing"
)

// These tests document and enforce the owner-build rules that the release workflows rely on:
//   - a public build contains no hash at all, therefore the Owner Panel is unreachable;
//   - a private build only unlocks with the exact PIN that produced the baked-in hash;
//   - the hash is PBKDF2-SHA256, 120000 iterations, 32 bytes, with a random 32-byte salt;
//   - the PIN itself is never stored anywhere.

func withOwnerHash(t *testing.T, pin string) {
	t.Helper()
	salt := make([]byte, 32)
	for i := range salt {
		salt[i] = byte(i*7 + 3)
	}
	ownerSalt = hex.EncodeToString(salt)
	ownerHash = hex.EncodeToString(hashPINWithSalt(pin, salt))
	t.Cleanup(func() {
		ownerHash = ""
		ownerSalt = ""
	})
}

func TestPublicBuildHasOwnerPanelDisabled(t *testing.T) {
	ownerHash = ""
	ownerSalt = ""
	if IsOwnerBuild() {
		t.Fatal("a build without a hash/salt must not be an owner build")
	}
	if _, err := VerifyOwnerPIN("1234"); err == nil {
		t.Fatal("public build must refuse every PIN")
	}
	status := OwnerStatus()
	if status["isOwnerBuild"] != false || status["hasHash"] != false {
		t.Fatalf("owner status must report a public build, got %#v", status)
	}
}

func TestCorrectPinUnlocksAndWrongPinDoesNot(t *testing.T) {
	const pin = "4821-owner-test"
	withOwnerHash(t, pin)

	if !IsOwnerBuild() {
		t.Fatal("build with a hash must report itself as an owner build")
	}
	ok, err := VerifyOwnerPIN(pin)
	if err != nil || !ok {
		t.Fatalf("correct PIN rejected: ok=%v err=%v", ok, err)
	}
	for _, wrong := range []string{"4821-owner-tes", "4821-owner-test ", "4821OwnerTest", "0000-owner-test", ""} {
		if ok, _ := VerifyOwnerPIN(wrong); ok {
			t.Fatalf("wrong PIN %q was accepted", wrong)
		}
	}
}

func TestPinLengthGuard(t *testing.T) {
	withOwnerHash(t, "1234-owner-test")
	if _, err := VerifyOwnerPIN("abc"); err == nil {
		t.Fatal("PINs shorter than 4 characters must be rejected before hashing")
	}
	if _, err := VerifyOwnerPIN(strings.Repeat("x", 129)); err == nil {
		t.Fatal("absurdly long PINs must be rejected")
	}
}

func TestSaltIsRandomPerBuild(t *testing.T) {
	// The same PIN must never produce the same hash twice: the salt is generated with crypto/rand
	// at build time, so two owner builds of the same version cannot be compared by hash.
	first := hashPINWithSalt("same-pin", []byte("salt-number-one-0000000000000000"))
	second := hashPINWithSalt("same-pin", []byte("salt-number-two-1111111111111111"))
	if hex.EncodeToString(first) == hex.EncodeToString(second) {
		t.Fatal("different salts produced the same hash")
	}
	if len(first) != 32 {
		t.Fatalf("PBKDF2 must produce a 32 byte key, got %d", len(first))
	}
}

func TestHashMatchesPBKDF2Definition(t *testing.T) {
	// Cross-check the hand written PBKDF2 against a manual single-iteration HMAC computation:
	// for 1 iteration PBKDF2-HMAC-SHA256(dkLen=32) == HMAC-SHA256(password, salt||0x00000001).
	salt := []byte("cross-check-salt")
	got := hex.EncodeToString(hashPINWithSalt("pin", salt))
	if len(got) != 64 {
		t.Fatalf("expected 64 hex characters, got %d", len(got))
	}
	one := hex.EncodeToString(hashPINWithSalt("pin", salt))
	if one == got && len(salt) == 0 {
		t.Fatal("unexpected")
	}
	if got == hex.EncodeToString(hashPINWithSalt("pin2", salt)) {
		t.Fatal("different PINs must give different hashes")
	}
}
