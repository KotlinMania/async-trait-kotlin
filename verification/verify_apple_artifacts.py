#!/usr/bin/env python3
"""Verify the built static XCFramework payloads independently of Maven KLIBs."""

import argparse
import hashlib
import json
import plistlib
import re
import subprocess
from pathlib import Path


EXPECTED = {
    ("macos", ""): 1,
    ("ios", ""): 2, ("ios", "simulator"): 7,
    ("tvos", ""): 3, ("tvos", "simulator"): 8,
    ("watchos", ""): 4, ("watchos", "simulator"): 9,
}


def verify(root, module):
    receipts = []
    for mode in ("debug", "release"):
        framework = root / mode / f"{module}.xcframework"
        info = plistlib.loads((framework / "Info.plist").read_bytes())
        libraries = info["AvailableLibraries"]
        actual = [(entry["SupportedPlatform"], entry.get("SupportedPlatformVariant", "")) for entry in libraries]
        if len(actual) != len(EXPECTED) or set(actual) != set(EXPECTED):
            raise ValueError(f"{mode} XCFramework has incorrect device/simulator slices: {actual}")
        for entry in libraries:
            if entry["SupportedArchitectures"] != ["arm64"]:
                raise ValueError(f"Unexpected or retired architecture: {entry}")
            binary = framework / entry["LibraryIdentifier"] / entry["BinaryPath"]
            data = binary.read_bytes()
            if not data.startswith(b"!<arch>\n"):
                raise ValueError(f"Expected a compiled static library archive: {binary}")
            architectures = subprocess.check_output(["xcrun", "lipo", "-archs", str(binary)], text=True).split()
            if architectures != ["arm64"]:
                raise ValueError(f"Binary architecture disagrees with XCFramework metadata: {binary}: {architectures}")
            commands = subprocess.check_output(["xcrun", "otool", "-l", str(binary)], text=True)
            platforms = {int(value) for value in re.findall(r"cmd LC_BUILD_VERSION\s+cmdsize \d+\s+platform (\d+)", commands)}
            expected_platform = EXPECTED[(entry["SupportedPlatform"], entry.get("SupportedPlatformVariant", ""))]
            if platforms != {expected_platform}:
                raise ValueError(f"Compiled Mach-O platform disagrees with XCFramework metadata: {binary}: {platforms}")
            header = binary.parent / "Headers" / f"{module}.h"
            modulemap = binary.parent / "Modules" / "module.modulemap"
            if "@interface" not in header.read_text() or f"framework module \"{module}\"" not in modulemap.read_text():
                raise ValueError(f"Framework header or module declaration is missing: {binary}")
            receipts.append({"mode": mode, "platform": entry["SupportedPlatform"],
                             "environment": entry.get("SupportedPlatformVariant", "device"),
                             "binary": str(binary.resolve()), "size": len(data),
                             "sha256": hashlib.sha256(data).hexdigest(), "architectures": architectures,
                             "mach_o_platform": expected_platform})
    return receipts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("--module", required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    args = parser.parse_args()
    result = verify(args.root, args.module)
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps({"xcframework_slices": result}, indent=2) + "\n")
    print(f"Verified {len(result)} static Apple framework slices. Receipt: {args.receipt}")


if __name__ == "__main__":
    main()
