package main

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestEndpointPolicy(t *testing.T) {
	cases := []struct {
		provider, url, want string
		bad                 bool
	}{
		{"ollama", "http://localhost:11434", "http://localhost:11434/api/chat", false},
		{"ollama", "http://127.0.0.1:11434/api/chat", "http://127.0.0.1:11434/api/chat", false},
		{"compatible", "https://api.example.com/v1/", "https://api.example.com/v1/chat/completions", false},
		{"compatible", "https://api.example.com/v1/chat/completions", "https://api.example.com/v1/chat/completions", false},
		{"compatible", "http://api.example.com/v1", "", true},
		{"compatible", "file:///etc/passwd", "", true},
		{"compatible", "https://user:key@api.example.com/v1", "", true},
		{"compatible", "https://api.example.com/v1?key=secret", "", true},
		{"compatible", "http://127.0.0.1.evil.example/v1", "", true},
		{"preview", "https://api.example.com", "", true},
	}
	for _, c := range cases {
		got, err := modelURL(c.provider, c.url)
		if c.bad {
			if err == nil {
				t.Errorf("accepted unsafe endpoint %s", c.url)
			}
		} else if err != nil || got != c.want {
			t.Errorf("%s: got %q (%v), wanted %q", c.url, got, err, c.want)
		}
	}
}
func TestRequestGuards(t *testing.T) {
	h := chatHandler("test-token", "http://127.0.0.1:1234")
	for _, c := range []struct {
		method, token, origin string
		status                int
	}{{"GET", "test-token", "http://127.0.0.1:1234", 405}, {"POST", "bad-token", "http://127.0.0.1:1234", 403}, {"POST", "test-token", "https://evil.example", 403}, {"POST", "test-token", "", 403}} {
		r := httptest.NewRequest(c.method, "http://127.0.0.1:1234/api/chat", strings.NewReader("{}"))
		r.Header.Set("X-Miyu-Token", c.token)
		r.Header.Set("Origin", c.origin)
		w := httptest.NewRecorder()
		h(w, r)
		if w.Code != c.status {
			t.Errorf("status %d, want %d", w.Code, c.status)
		}
	}
}
func TestCompatibleProxy(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/chat/completions" {
			t.Errorf("unexpected path: %s", r.URL.Path)
		}
		if r.Header.Get("Authorization") != "Bearer test-key" {
			t.Error("missing selected API credentials")
		}
		var body map[string]any
		_ = json.NewDecoder(r.Body).Decode(&body)
		if body["stream"] != false || body["model"] != "test" {
			t.Error("invalid model payload")
		}
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"choices":[{"message":{"content":"Hello from a model."}}]}`))
	}))
	defer upstream.Close()
	got, err := callModel(context.Background(), ChatRequest{Provider: "compatible", Endpoint: upstream.URL + "/v1", Model: "test", Key: "test-key", Messages: []ChatMessage{{Role: "user", Content: "Hello"}}})
	if err != nil || got != "Hello from a model." {
		t.Fatalf("proxy reply: %q %v", got, err)
	}
}
func TestOllamaDoesNotReceiveAPIKey(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/chat" {
			t.Errorf("unexpected path: %s", r.URL.Path)
		}
		if r.Header.Get("Authorization") != "" {
			t.Error("external API key leaked to Ollama")
		}
		_, _ = w.Write([]byte(`{"message":{"content":"Local model hello."}}`))
	}))
	defer upstream.Close()
	got, err := callModel(context.Background(), ChatRequest{Provider: "ollama", Endpoint: upstream.URL, Model: "test", Key: "should-not-be-sent", Messages: []ChatMessage{{Role: "user", Content: "Hi"}}})
	if err != nil || got != "Local model hello." {
		t.Fatalf("Ollama response %q %v", got, err)
	}
}
func TestNoCrossHostRedirect(t *testing.T) {
	reached := false
	target := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { reached = true }))
	defer target.Close()
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, target.URL, http.StatusTemporaryRedirect)
	}))
	defer upstream.Close()
	_, err := callModel(context.Background(), ChatRequest{Provider: "compatible", Endpoint: upstream.URL, Model: "test", Key: "private-key", Messages: []ChatMessage{{Role: "user", Content: "Private text"}}})
	if err == nil || reached {
		t.Fatal("redirect should not forward a request")
	}
}
func TestErrorsRedactCredentials(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(401)
		_, _ = w.Write([]byte(`{"error":{"message":"Bad key private-key"}}`))
	}))
	defer upstream.Close()
	_, err := callModel(context.Background(), ChatRequest{Provider: "compatible", Endpoint: upstream.URL, Model: "test", Key: "private-key", Messages: []ChatMessage{{Role: "user", Content: "Hi"}}})
	if err == nil || strings.Contains(err.Error(), "private-key") {
		t.Fatal("API key visible in an error")
	}
}
func TestHandlerIntegration(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte(`{"message":{"content":"Hello."}}`))
	}))
	defer upstream.Close()
	c := ChatRequest{Provider: "ollama", Endpoint: upstream.URL, Model: "test", Messages: []ChatMessage{{Role: "user", Content: "Hi"}}}
	body, _ := json.Marshal(c)
	r := httptest.NewRequest("POST", "http://127.0.0.1:1234/api/chat", bytes.NewReader(body))
	r.Header.Set("Origin", "http://127.0.0.1:1234")
	r.Header.Set("X-Miyu-Token", "secret")
	w := httptest.NewRecorder()
	chatHandler("secret", "http://127.0.0.1:1234")(w, r)
	if w.Code != 200 || !strings.Contains(w.Body.String(), `"content":"Hello."`) {
		t.Fatalf("%d: %s", w.Code, w.Body.String())
	}
}
func TestEmptyResponseIsNotSuccess(t *testing.T) {
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { _, _ = w.Write([]byte(`{"choices":[]}`)) }))
	defer upstream.Close()
	_, err := callModel(context.Background(), ChatRequest{Provider: "compatible", Endpoint: upstream.URL, Model: "test", Messages: []ChatMessage{{Role: "user", Content: "Hi"}}})
	if err == nil {
		t.Fatal("empty output must be reported honestly")
	}
}
