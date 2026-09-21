package main

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"time"
)

type updateManifest struct {
	Version string `json:"version"`
	URL     string `json:"url"`
	SHA256  string `json:"sha256"`
	Notes   string `json:"notes"`
}

// checkUpdateManifest is deliberately opt-in through MIYU_UPDATE_MANIFEST.
// A signed production build can point it at its HTTPS release manifest. The
// prototype never silently executes a downloaded binary.
func checkUpdateManifest(ctx context.Context, manifestURL, current string) (updateManifest, error) {
	if manifestURL == "" {
		return updateManifest{Version: current, Notes: "automatic updates are not configured for this personal build"}, nil
	}
	u, err := url.Parse(manifestURL)
	if err != nil || u.Scheme != "https" || u.Host == "" || u.RawQuery != "" || u.Fragment != "" {
		return updateManifest{}, errors.New("the update manifest must be a clean HTTPS URL")
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
	if err != nil {
		return updateManifest{}, errors.New("could not create the update request")
	}
	client := &http.Client{Timeout: 12 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	res, err := client.Do(req)
	if err != nil {
		return updateManifest{}, errors.New("could not reach the update manifest")
	}
	defer res.Body.Close()
	if res.StatusCode < 200 || res.StatusCode >= 300 {
		return updateManifest{}, fmt.Errorf("update manifest returned HTTP %d", res.StatusCode)
	}
	raw, err := io.ReadAll(io.LimitReader(res.Body, 128*1024))
	if err != nil {
		return updateManifest{}, errors.New("could not read the update manifest")
	}
	var manifest updateManifest
	if err := json.Unmarshal(raw, &manifest); err != nil || manifest.Version == "" {
		return updateManifest{}, errors.New("the update manifest is invalid")
	}
	if manifest.URL != "" {
		artifact, err := url.Parse(manifest.URL)
		if err != nil || artifact.Scheme != "https" || artifact.Host == "" || artifact.User != nil {
			return updateManifest{}, errors.New("the update download URL is not trusted")
		}
	}
	if manifest.SHA256 != "" {
		decoded, err := hex.DecodeString(manifest.SHA256)
		if err != nil || len(decoded) != sha256.Size {
			return updateManifest{}, errors.New("the update checksum is invalid")
		}
	}
	return manifest, nil
}

func checksum(data []byte) string {
	sum := sha256.Sum256(data)
	return hex.EncodeToString(sum[:])
}
