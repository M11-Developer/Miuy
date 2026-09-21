package main

const (
	AppName    = "Miyu"
	AppVersion = "1.2.0"
	BuildChannel = "public" // overridden via ldflags for owner builds
)

var (
	BuildTime   = "unknown"
	GitCommit   = "unknown"
)
