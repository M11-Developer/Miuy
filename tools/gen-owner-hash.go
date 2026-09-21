package main

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"hash"
	"os"
)

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
	hash := pbkdf2Key([]byte(pin), salt, 120000, 32, sha256.New)
	fmt.Printf("salt=%s\n", hex.EncodeToString(salt))
	fmt.Printf("hash=%s\n", hex.EncodeToString(hash))
	fmt.Printf("LDFLAGS=-X main.ownerSalt=%s -X main.ownerHash=%s -X main.ownerBuildID=owner-%d\n", hex.EncodeToString(salt), hex.EncodeToString(hash), os.Getpid())
}
