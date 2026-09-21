package main

import (
	"context"
	"encoding/json"
	"os"
	"sync"
	"time"
)

// modelWorkerPool keeps network/model work away from the WebView UI thread.
// The queue is intentionally bounded: a child tapping Send repeatedly cannot
// create unbounded goroutines or memory pressure.
type modelJob struct {
	ctx context.Context
	req ChatRequest
	out chan modelResult
}

type modelResult struct {
	text string
	err  error
}

type modelWorkerPool struct {
	jobs chan modelJob
	stop chan struct{}
	wg   sync.WaitGroup
}

func newModelWorkerPool(size int) *modelWorkerPool {
	if size < 1 {
		size = 1
	}
	p := &modelWorkerPool{jobs: make(chan modelJob, size*2), stop: make(chan struct{})}
	for i := 0; i < size; i++ {
		p.wg.Add(1)
		go func() {
			defer p.wg.Done()
			for {
				select {
				case job := <-p.jobs:
					text, err := callModel(job.ctx, job.req)
					select {
					case job.out <- modelResult{text: text, err: err}:
					case <-job.ctx.Done():
					}
				case <-p.stop:
					return
				}
			}
		}()
	}
	return p
}

func (p *modelWorkerPool) Do(ctx context.Context, req ChatRequest) (string, error) {
	out := make(chan modelResult, 1)
	select {
	case p.jobs <- modelJob{ctx: ctx, req: req, out: out}:
	case <-ctx.Done():
		return "", ctx.Err()
	}
	select {
	case result := <-out:
		return result.text, result.err
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func (p *modelWorkerPool) Close() {
	close(p.stop)
	p.wg.Wait()
}

var modelWorkers = newModelWorkerPool(2)

// eventLogger writes small JSON lines rather than secrets or raw conversation.
// It is useful when a native crash or model integration fails, while keeping
// chat contents out of diagnostics.
type eventLogger struct {
	mu   sync.Mutex
	file *os.File
}

func newEventLogger(path string) *eventLogger {
	file, err := os.OpenFile(path, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil {
		return &eventLogger{}
	}
	return &eventLogger{file: file}
}

func (l *eventLogger) Event(name string, fields map[string]any) {
	if l == nil || l.file == nil {
		return
	}
	record := map[string]any{"at": time.Now().UTC().Format(time.RFC3339Nano), "event": name}
	for key, value := range fields {
		record[key] = value
	}
	encoded, err := json.Marshal(record)
	if err != nil {
		return
	}
	l.mu.Lock()
	defer l.mu.Unlock()
	_, _ = l.file.Write(append(encoded, '\n'))
}

func (l *eventLogger) Close() {
	if l == nil || l.file == nil {
		return
	}
	l.mu.Lock()
	defer l.mu.Unlock()
	_ = l.file.Close()
}
