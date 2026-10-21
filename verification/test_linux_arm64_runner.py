"""Acceptance tests for Native execution evidence, without simulating library tests."""

import hashlib
import tempfile
import unittest
from pathlib import Path

from run_linux_arm64_tests import passed_cases, verify_binary


OUTPUT = """[==========] Running 1 tests from 1 test cases.
[ RUN      ] Suite.behavior
[       OK ] Suite.behavior (2 ms)
[==========] 1 tests from 1 test cases ran. (3 ms total)
[  PASSED  ] 1 tests.
"""


class NativeEvidenceTests(unittest.TestCase):
    def test_complete_protocol(self):
        self.assertEqual([("Suite.behavior", "2")], passed_cases(OUTPUT))

    def test_rejects_empty_partial_duplicate_failed_and_skipped_runs(self):
        for invalid in ["", OUTPUT.replace("Running 1", "Running 2"), OUTPUT + OUTPUT,
                        OUTPUT.replace("[       OK ]", "[  FAILED  ]"), OUTPUT + "[  SKIPPED ] Suite.other\n",
                        OUTPUT.replace("[==========] 1 tests from 1 test cases ran. (3 ms total)\n", "")]:
            with self.subTest(output=invalid), self.assertRaises(ValueError):
                passed_cases(invalid)

    def test_host_binary_and_manifest_must_match(self):
        scratch = Path(__file__).resolve().parents[1] / "build"
        scratch.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as folder:
            binary = Path(folder) / "test.kexe"
            header = bytearray(20)
            header[:6] = b"\x7fELF\x02\x01"
            header[18:20] = (183).to_bytes(2, "little")
            binary.write_bytes(header)
            digest = hashlib.sha256(header).hexdigest()
            manifest = binary.with_name("test.kexe.sha256")
            manifest.write_text(f"{digest}  test.kexe\n")
            self.assertEqual(digest, verify_binary(binary, "Linux", "aarch64"))
            with self.assertRaises(ValueError):
                verify_binary(binary, "Linux", "x86_64")
            binary.write_bytes(header[:18] + (62).to_bytes(2, "little"))
            with self.assertRaises(ValueError):
                verify_binary(binary, "Linux", "aarch64")
            binary.write_bytes(header)
            manifest.write_text("wrong  test.kexe\n")
            with self.assertRaises(ValueError):
                verify_binary(binary, "Linux", "aarch64")


if __name__ == "__main__":
    unittest.main()
