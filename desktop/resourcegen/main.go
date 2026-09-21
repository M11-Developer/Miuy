// Build helper: go run ./resourcegen (from the desktop folder)
package main

import (
	"github.com/tc-hib/winres"
	"github.com/tc-hib/winres/version"
	"log"
	"os"
)

func main() {
	in, err := os.Open("icon.ico")
	if err != nil {
		log.Fatal(err)
	}
	defer in.Close()
	icon, err := winres.LoadICO(in)
	if err != nil {
		log.Fatal(err)
	}
	rs := winres.ResourceSet{}
	if err := rs.SetIcon(winres.ID(1), icon); err != nil {
		log.Fatal(err)
	}
	rs.SetManifest(winres.AppManifest{Identity: winres.AssemblyIdentity{Name: "Miyu.Companion", Version: [4]uint16{1, 2, 0, 0}}, Description: "Miyu animated desktop companion", Compatibility: winres.Win10AndAbove, ExecutionLevel: winres.AsInvoker, DPIAwareness: winres.DPIPerMonitorV2, LongPathAware: true})
	vi := version.Info{FileVersion: [4]uint16{1, 2, 0, 0}, ProductVersion: [4]uint16{1, 2, 0, 0}, Type: version.App}
	vi.Set(version.LangDefault, version.ProductName, "Miyu")
	vi.Set(version.LangDefault, version.FileDescription, "Miyu — a little company, a little magic")
	vi.Set(version.LangDefault, version.FileVersion, "1.2.0")
	vi.Set(version.LangDefault, version.ProductVersion, "1.2.0")
	vi.Set(version.LangDefault, version.OriginalFilename, "Miyu.exe")
	vi.Set(version.LangDefault, version.Comments, "Unsigned personal prototype. No model weights bundled.")
	rs.SetVersionInfo(vi)
	out, err := os.Create("rsrc_windows_amd64.syso")
	if err != nil {
		log.Fatal(err)
	}
	defer out.Close()
	if err := rs.WriteObject(out, winres.ArchAMD64); err != nil {
		log.Fatal(err)
	}
}
