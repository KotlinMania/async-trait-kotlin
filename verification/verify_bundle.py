#!/usr/bin/env python3
"""Inspect the actual Central bundle, including KMP metadata and binary payloads."""

import argparse
import hashlib
import io
import json
import posixpath
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


PLATFORM = "org.jetbrains.kotlin.platform.type"
NATIVE = "org.jetbrains.kotlin.native.target"
WASM = "org.jetbrains.kotlin.wasm.target"
USAGE = "org.gradle.usage"
TARGETS = {
    "android": {PLATFORM: "androidJvm", "org.gradle.libraryelements": "aar"},
    "jvm": {PLATFORM: "jvm", "org.gradle.libraryelements": "jar"},
    "js": {PLATFORM: "js"},
    "wasmjs": {PLATFORM: "wasm", WASM: "js"},
    "wasmwasi": {PLATFORM: "wasm", WASM: "wasi"},
    "linuxx64": {PLATFORM: "native", NATIVE: "linux_x64"},
    "linuxarm64": {PLATFORM: "native", NATIVE: "linux_arm64"},
    "mingwx64": {PLATFORM: "native", NATIVE: "mingw_x64"},
    "androidnativearm64": {PLATFORM: "native", NATIVE: "android_arm64"},
    "androidnativex64": {PLATFORM: "native", NATIVE: "android_x64"},
    "macosarm64": {PLATFORM: "native", NATIVE: "macos_arm64"},
    "iosarm64": {PLATFORM: "native", NATIVE: "ios_arm64"},
    "iossimulatorarm64": {PLATFORM: "native", NATIVE: "ios_simulator_arm64"},
    "tvosarm64": {PLATFORM: "native", NATIVE: "tvos_arm64"},
    "tvossimulatorarm64": {PLATFORM: "native", NATIVE: "tvos_simulator_arm64"},
    "watchosdevicearm64": {PLATFORM: "native", NATIVE: "watchos_device_arm64"},
    "watchossimulatorarm64": {PLATFORM: "native", NATIVE: "watchos_simulator_arm64"},
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def archive(data):
    result = zipfile.ZipFile(io.BytesIO(data))
    require(result.testzip() is None, "Archive CRC check failed")
    return result


def kotlin_classes(data, namespace):
    with archive(data) as jar:
        names = jar.namelist()
        require(namespace.replace(".", "/") + "/Args.class" in names,
                "Compiled Kotlin API Args is missing")
        require(any(n.endswith(".kotlin_module") for n in names),
                "Kotlin module descriptor is missing")
        return sum(n.endswith(".class") for n in names)


def inspect_payload(data, filename, attributes, namespace):
    platform = attributes[PLATFORM]
    usage = attributes[USAGE]
    with archive(data) as payload:
        names = payload.namelist()
        if usage == "kotlin-metadata":
            require(filename.endswith(".jar"), "Metadata must use its declared JAR container")
            require(not any(n.endswith(".class") for n in names),
                    "Common/native metadata contains target bytecode")
            if platform == "common":
                require("META-INF/kotlin-project-structure-metadata.json" in names,
                        "Root Kotlin project structure metadata is missing")
                require(any("/linkdata/" in n and n.endswith(".knm") for n in names),
                        "Root common Kotlin metadata is missing")
            return {"kind": "kotlin-metadata", "entry_count": len(names)}
        if platform == "androidJvm":
            require(filename.endswith(".aar"), "Android variant must publish an AAR")
            require("AndroidManifest.xml" in names and "classes.jar" in names,
                    "Android AAR lacks its manifest or compiled Kotlin classes")
            manifest = ET.fromstring(payload.read("AndroidManifest.xml"))
            require(manifest.get("package") == namespace, "Android manifest namespace differs")
            require("META-INF/com/android/build/gradle/aar-metadata.properties" in names,
                    "Android AAR metadata is missing")
            require(not any(n.endswith(".dex") for n in names), "A device APK is not a library AAR")
            return {"kind": "android-aar", "kotlin_classes": kotlin_classes(payload.read("classes.jar"), namespace)}
        if platform == "jvm":
            require(filename.endswith(".jar"), "Kotlin/JVM variant must publish a JAR")
            require("AndroidManifest.xml" not in names, "Kotlin/JVM artifact contains an Android manifest")
            return {"kind": "kotlin-jvm-jar", "kotlin_classes": kotlin_classes(data, namespace)}
        require(filename.endswith(".klib"), f"{platform} variant must publish a KLIB")
        require(not any(n.endswith(".class") for n in names), "KLIB contains JVM bytecode")
        require("default/manifest" in names, "KLIB manifest is missing")
        require(any(n.startswith("default/ir/") and payload.getinfo(n).file_size for n in names),
                "KLIB contains no compiled IR")
        require(any(n.startswith("default/linkdata/") and payload.getinfo(n).file_size for n in names),
                "KLIB contains no link metadata")
        manifest = dict(line.split("=", 1) for line in payload.read("default/manifest").decode().splitlines() if "=" in line)
        require(manifest.get("builtins_platform") == platform.upper(), "KLIB platform does not match variant")
        if platform == "native":
            require(manifest.get("native_targets") == attributes[NATIVE], "KLIB native target does not match variant")
        if platform == "wasm":
            require(manifest.get("wasm_targets") == "wasm-" + attributes[WASM], "Wasm KLIB environment does not match variant")
        return {"kind": "kotlin-klib", "manifest": manifest}


def verify(args):
    group_path = args.group.replace(".", "/")
    modules = {args.module: None, **{f"{args.module}-{target}": attrs for target, attrs in TARGETS.items()}}
    receipts = []
    with zipfile.ZipFile(args.bundle) as bundle:
        entries = bundle.namelist()
        require(len(entries) == len(set(entries)), "Bundle contains duplicate ZIP entries")
        files = {n for n in entries if not n.endswith("/")}
        directories = {f"{group_path}/{module}/{args.version}/" for module in modules}
        require(all(any(n.startswith(d) for d in directories) for n in files),
                "Bundle includes another coordinate/version or repository-level files")
        require(not any(n.endswith(".apk") for n in files), "Device APK must not be published as a library")
        metadata = {}
        for module, expected in modules.items():
            base = f"{group_path}/{module}/{args.version}/{module}-{args.version}"
            data = json.loads(bundle.read(base + ".module"))
            component = data["component"]
            owner = args.module if "url" in component else module
            require(all(data["component"].get(k) == v for k, v in
                        {"group": args.group, "module": owner, "version": args.version}.items()),
                    f"Wrong module coordinates: {module}")
            if "url" in component:
                owner_path = posixpath.normpath(posixpath.join(posixpath.dirname(base), component["url"]))
                require(owner_path == f"{group_path}/{args.module}/{args.version}/{args.module}-{args.version}.module",
                        f"Target component points to the wrong root publication: {module}")
            metadata[module] = data
            pom = ET.fromstring(bundle.read(base + ".pom"))
            packaging = "aar" if expected and expected[PLATFORM] == "androidJvm" else (
                "klib" if expected and expected[PLATFORM] not in ("jvm", "androidJvm") else "jar")
            require(pom.findtext("{*}packaging", "jar") == packaging, f"Wrong POM packaging: {module}")
            for key, value in {"groupId": args.group, "artifactId": module, "version": args.version}.items():
                require(pom.findtext("{*}" + key) == value, f"Wrong POM {key}: {module}")
            for classifier in ["sources", "javadoc"]:
                with archive(bundle.read(base + f"-{classifier}.jar")) as documentation:
                    require(any(not n.endswith("/") and not n.startswith("META-INF/") for n in documentation.namelist()),
                            f"Empty {classifier} archive: {module}")
            usages = set()
            for variant in data["variants"]:
                attrs = variant["attributes"]
                variant_receipt = {"module": module, "variant": variant["name"], "attributes": attrs, "files": []}
                for artifact in variant.get("files", []):
                    path = posixpath.normpath(posixpath.join(posixpath.dirname(base), artifact["url"]))
                    require(path in files, f"Metadata references a missing artifact: {path}")
                    payload = bundle.read(path)
                    require(len(payload) == artifact["size"], f"Artifact size mismatch: {path}")
                    for algorithm in ["sha512", "sha256", "sha1", "md5"]:
                        require(hashlib.new(algorithm, payload).hexdigest() == artifact[algorithm],
                                f"Artifact {algorithm} mismatch: {path}")
                    receipt = {"path": path, "sha256": hashlib.sha256(payload).hexdigest()}
                    if attrs.get("org.gradle.category") == "library":
                        if expected:
                            require(all(attrs.get(k) == v for k, v in expected.items()),
                                    f"Wrong target attributes: {module}/{variant['name']}")
                        receipt.update(inspect_payload(payload, path, attrs, args.namespace))
                        usages.add(attrs[USAGE])
                    variant_receipt["files"].append(receipt)
                receipts.append(variant_receipt)
            required_usages = {"kotlin-metadata"} if expected is None else (
                {"java-api", "java-runtime"} if expected[PLATFORM] in ("androidJvm", "jvm") else
                {"kotlin-api"} if expected[PLATFORM] == "native" else {"kotlin-api", "kotlin-runtime"})
            require(required_usages <= usages, f"Missing library usage variants for {module}: {required_usages - usages}")
        referenced = set()
        for variant in metadata[args.module]["variants"]:
            reference = variant.get("available-at")
            if reference is None:
                continue
            module = reference["module"]
            require(module in modules and module != args.module, "Root points to an unexpected target")
            require(reference["group"] == args.group and reference["version"] == args.version,
                    "Root target reference uses the wrong coordinates")
            root_directory = f"{group_path}/{args.module}/{args.version}"
            reference_path = posixpath.normpath(posixpath.join(root_directory, reference["url"]))
            require(json.loads(bundle.read(reference_path)) == metadata[module], "Root target metadata URL is incorrect")
            require(any(v["name"] == variant["name"] and v["attributes"] == variant["attributes"]
                        for v in metadata[module]["variants"]), "Root and target attributes disagree")
            referenced.add(module)
        require(referenced == set(modules) - {args.module}, "Root metadata does not expose every supported target")
        checksum_count = 0
        signatures = 0
        for path in sorted(files):
            extension = path.rsplit(".", 1)[-1]
            if extension in ("sha512", "sha256", "sha1", "md5"):
                target = path.rsplit(".", 1)[0]
                require(bundle.read(path).decode().strip() == hashlib.new(extension, bundle.read(target)).hexdigest(),
                        f"Checksum sidecar mismatch: {path}")
                checksum_count += 1
            elif extension == "asc":
                require(b"BEGIN PGP SIGNATURE" in bundle.read(path), f"Invalid signature container: {path}")
                signatures += 1
            elif args.require_signatures:
                require(path + ".asc" in files, f"Unsigned publication file: {path}")
    return {"bundle": str(args.bundle.resolve()), "sha256": hashlib.sha256(args.bundle.read_bytes()).hexdigest(),
            "group": args.group, "module": args.module, "version": args.version,
            "publication_count": len(modules), "target_count": len(TARGETS),
            "checksum_count": checksum_count, "signature_containers": signatures,
            "signatures_required": args.require_signatures, "variants": receipts}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--group", required=True)
    parser.add_argument("--module", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--namespace", required=True)
    parser.add_argument("--receipt", required=True, type=Path)
    parser.add_argument("--require-signatures", action="store_true")
    args = parser.parse_args()
    result = verify(args)
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(result, indent=2) + "\n")
    print(f"Verified bundle payloads and metadata: {result['target_count']} KMP targets, "
          f"{result['publication_count']} publications. Receipt: {args.receipt}")


if __name__ == "__main__":
    main()
