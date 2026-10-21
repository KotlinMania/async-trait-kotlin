"""Execute a cross-linked Kotlin/Native test binary on its real Linux ARM64 host."""

import argparse
import hashlib
import json
import platform
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


def passed_cases(output):
    """Read the installed Kotlin/Native GTestLogger protocol, rejecting partial runs."""
    planned = re.findall(r"^\[==========\] Running (\d+) tests from \d+ test cases\.$", output, re.M)
    summary = re.findall(r"^\[  PASSED  \] (\d+) tests\.$", output, re.M)
    started = re.findall(r"^\[ RUN      \] (.+)$", output, re.M)
    finished = re.findall(r"^\[       OK \] (.+) \((\d+) ms\)$", output, re.M)
    if len(planned) != 1 or len(summary) != 1 or int(planned[0]) == 0:
        raise ValueError("Missing, empty or repeated Native test run")
    if "[  FAILED  ]" in output or "[  SKIPPED ]" in output:
        raise ValueError("Native tests failed or were skipped")
    names = [name for name, _ in finished]
    if len(set(started)) != len(started) or len(set(names)) != len(names):
        raise ValueError("Duplicate Native test result")
    if len(started) != int(planned[0]) or names != started or len(names) != int(summary[0]):
        raise ValueError("Incomplete Native test execution")
    if not re.search(rf"^\[==========\] {len(names)} tests from \d+ test cases ran\. \(\d+ ms total\)$", output, re.M):
        raise ValueError("Missing Native test completion")
    return finished


def verify_binary(binary, system, machine):
    if system != "Linux" or machine not in {"aarch64", "arm64"}:
        raise ValueError(f"Actual Linux ARM64 execution required; got {system}/{machine}")
    header = binary.read_bytes()[:20]
    if header[:6] != b"\x7fELF\x02\x01" or int.from_bytes(header[18:20], "little") != 183:
        raise ValueError("Expected a 64-bit little-endian AArch64 ELF executable")
    digest = hashlib.sha256(binary.read_bytes()).hexdigest()
    expected = binary.with_name(binary.name + ".sha256").read_text().split()
    if expected != [digest, binary.name]:
        raise ValueError("Executable differs from the producer's SHA256 manifest")
    return digest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("binary", type=Path)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    args.reports.mkdir(parents=True, exist_ok=True)
    receipt = {"revision": args.revision, "system": platform.system(), "machine": platform.machine(), "passed": False}
    try:
        receipt["binary_sha256"] = verify_binary(args.binary, receipt["system"], receipt["machine"])
        args.binary.chmod(args.binary.stat().st_mode | 0o100)
        result = subprocess.run([str(args.binary.resolve()), "--ktest_logger=GTEST"], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=1800)
        (args.reports / "native.log").write_text(result.stdout)
        print(result.stdout, end="")
        receipt["exit_code"] = result.returncode
        if result.returncode != 0:
            raise ValueError(f"Native executable exited {result.returncode}")
        cases = passed_cases(result.stdout)
        suite = ET.Element("testsuite", name="linuxArm64Runtime", tests=str(len(cases)), failures="0", errors="0", skipped="0")
        for name, milliseconds in cases:
            classname, _, method = name.rpartition(".")
            ET.SubElement(suite, "testcase", classname=classname, name=method, time=str(int(milliseconds) / 1000))
        ET.ElementTree(suite).write(args.reports / "TEST-linuxArm64Runtime.xml", encoding="utf-8", xml_declaration=True)
        receipt.update(passed=True, executed_tests=[name for name, _ in cases])
    except (ValueError, OSError, subprocess.TimeoutExpired) as error:
        receipt["error"] = str(error)
        raise
    finally:
        (args.reports / "execution.json").write_text(json.dumps(receipt, indent=2) + "\n")


if __name__ == "__main__":
    main()
