package main

import (
	"bytes"
	"context"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type ChatMessage struct {
	Role    string `json:"role"`
	Content string `json:"content"`
}
type ChatRequest struct {
	Provider string        `json:"provider"`
	Endpoint string        `json:"endpoint"`
	Model    string        `json:"model"`
	Key      string        `json:"key"`
	Messages []ChatMessage `json:"messages"`
}

func modelURL(provider, endpoint string) (string, error) {
	if provider != "ollama" && provider != "compatible" {
		return "", errors.New("choose Ollama or an OpenAI-compatible provider")
	}
	u, err := url.Parse(endpoint)
	if err != nil || u.Host == "" {
		return "", errors.New("invalid model endpoint")
	}
	loopback := u.Hostname() == "localhost" || u.Hostname() == "127.0.0.1" || u.Hostname() == "::1"
	if u.Scheme != "https" && !(u.Scheme == "http" && loopback) {
		return "", errors.New("remote endpoints must use HTTPS; HTTP is allowed only on loopback")
	}
	if u.User != nil || u.RawQuery != "" || u.Fragment != "" {
		return "", errors.New("credentials, query parameters, and fragments are not allowed in the base URL")
	}
	u.Path = strings.TrimRight(u.Path, "/")
	suffix := "/chat/completions"
	if provider == "ollama" {
		suffix = "/api/chat"
	}
	if !strings.HasSuffix(u.Path, suffix) {
		u.Path += suffix
	}
	return u.String(), nil
}

func validateChat(c ChatRequest) error {
	if len(c.Model) == 0 || len(c.Model) > 160 {
		return errors.New("enter a valid model name")
	}
	if len(c.Key) > 2048 {
		return errors.New("invalid API key length")
	}
	if len(c.Messages) == 0 || len(c.Messages) > 24 {
		return errors.New("invalid message count")
	}
	for _, m := range c.Messages {
		if m.Role != "system" && m.Role != "user" && m.Role != "assistant" {
			return errors.New("invalid message role")
		}
		limit := 64000
		if m.Role == "system" {
			limit = 150000
		}
		if len(m.Content) > limit {
			return errors.New("message is too long")
		}
	}
	return nil
}

func callModel(ctx context.Context, c ChatRequest) (string, error) {
	endpoint, err := modelURL(c.Provider, c.Endpoint)
	if err != nil {
		return "", err
	}
	if err := validateChat(c); err != nil {
		return "", err
	}
	payload := map[string]any{"model": c.Model, "messages": c.Messages, "stream": false}
	if c.Provider == "ollama" {
		payload["options"] = map[string]any{"temperature": 0.75}
		payload["keep_alive"] = "5m"
	} else {
		payload["temperature"] = 0.75
		payload["max_tokens"] = 600
	}
	body, err := json.Marshal(payload)
	if err != nil {
		return "", err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(body))
	if err != nil {
		return "", errors.New("could not create the model request")
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	if c.Key != "" && c.Provider == "compatible" {
		req.Header.Set("Authorization", "Bearer "+c.Key)
	}
	client := &http.Client{
		Timeout: 85 * time.Second,
		// Do not forward messages or credentials to an unselected redirected host.
		CheckRedirect: func(req *http.Request, via []*http.Request) error { return http.ErrUseLastResponse },
	}
	res, err := client.Do(req)
	if err != nil {
		if ctx.Err() != nil {
			return "", errors.New("request canceled or timed out")
		}
		return "", errors.New("could not reach the chosen model; check its address, certificate, and that the server is running")
	}
	defer res.Body.Close()
	raw, err := io.ReadAll(io.LimitReader(res.Body, 2*1024*1024))
	if err != nil {
		return "", errors.New("could not read the model response")
	}
	var parsed struct {
		Message struct {
			Content string `json:"content"`
		} `json:"message"`
		Choices []struct {
			Message struct {
				Content string `json:"content"`
			} `json:"message"`
		} `json:"choices"`
		Error json.RawMessage `json:"error"`
	}
	if res.StatusCode < 200 || res.StatusCode >= 300 {
		detail := ""
		var envelope map[string]json.RawMessage
		if json.Unmarshal(raw, &envelope) == nil {
			var s string
			if json.Unmarshal(envelope["error"], &s) == nil {
				detail = s
			} else {
				var apierr struct {
					Message string `json:"message"`
				}
				if json.Unmarshal(envelope["error"], &apierr) == nil {
					detail = apierr.Message
				}
			}
		}
		if c.Key != "" {
			detail = strings.ReplaceAll(detail, c.Key, "[key hidden]")
		}
		if len(detail) > 400 {
			detail = detail[:400]
		}
		if detail == "" {
			detail = "check the endpoint, model name, and API credentials"
		}
		return "", fmt.Errorf("model returned HTTP %d: %s", res.StatusCode, detail)
	}
	if err := json.Unmarshal(raw, &parsed); err != nil {
		return "", errors.New("the model did not return a supported JSON response")
	}
	content := parsed.Message.Content
	if c.Provider == "compatible" {
		if len(parsed.Choices) > 0 {
			content = parsed.Choices[0].Message.Content
		} else {
			content = ""
		}
	}
	if strings.TrimSpace(content) == "" {
		return "", errors.New("the model returned no text; try a different model")
	}
	if len(content) > 64000 {
		content = content[:64000]
	}
	return strings.TrimSpace(content), nil
}

func chatHandler(token, origin string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		fail := func(status int, message string) {
			w.WriteHeader(status)
			_ = json.NewEncoder(w).Encode(map[string]string{"error": message})
		}
		if r.Method != http.MethodPost {
			fail(http.StatusMethodNotAllowed, "POST required")
			return
		}
		if subtle.ConstantTimeCompare([]byte(r.Header.Get("X-Miyu-Token")), []byte(token)) != 1 || r.Header.Get("Origin") != origin {
			fail(http.StatusForbidden, "this request did not come from the local Miyu window")
			return
		}
		r.Body = http.MaxBytesReader(w, r.Body, 1024*1024)
		var c ChatRequest
		dec := json.NewDecoder(r.Body)
		dec.DisallowUnknownFields()
		if err := dec.Decode(&c); err != nil {
			fail(http.StatusBadRequest, "invalid model request")
			return
		}
		if _, err := modelURL(c.Provider, c.Endpoint); err != nil {
			fail(http.StatusBadRequest, err.Error())
			return
		}
		if err := validateChat(c); err != nil {
			fail(http.StatusBadRequest, err.Error())
			return
		}
		text, err := callModel(r.Context(), c)
		if err != nil {
			fail(http.StatusBadGateway, err.Error())
			return
		}
		_ = json.NewEncoder(w).Encode(map[string]string{"content": text})
	}
}
