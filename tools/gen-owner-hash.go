package main

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"os"

	"golang.org/x/crypto/pbkdf2"
)

func main() {
	pin := os.Getenv("MIYU_OWNER_PIN")
	if pin == "" {
		fmt.Fprintln(os.Stderr, "MIYU_OWNER_PIN env not set")
		os.Exit(1)
	}
	if len(pin) < 4 {
		fmt.Fprintln(os.Stderr, "PIN too short")
		os.Exit(1)
	}
	salt := make([]byte, 32)
	if _, err := rand.Read(salt); err != nil {
		fmt.Fprintln(os.Stderr, "failed to generate salt")
		os.Exit(1)
	}
	hash := pbkdf2.Key([]byte(pin), salt, 120000, 32, sha256.New)
	fmt.Printf("salt=%s\n", hex.EncodeToString(salt))
	fmt.Printf("hash=%s\n", hex.EncodeToString(hash))
	// Also output for ldflags
	fmt.Printf("LDFLAGS=-X main.ownerSalt=%s -X main.ownerHash=%s -X main.ownerBuildID=owner-%d\n", hex.EncodeToString(salt), hex.EncodeToString(hash), os.Getpid())
}
