package main

const (
	AppName    = "Miyu"
	AppVersion = "1.2.5"
	// BuildChannel is overridden via ldflags for owner builds.
	// public  = public release build (Owner Panel disabled)
	// owner   = private owner build (Owner Panel enabled, PIN hash baked in at build time)
	BuildChannel = "public"
)

var (
	BuildTime = "unknown"
	GitCommit = "unknown"
)
