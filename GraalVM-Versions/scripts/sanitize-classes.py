#!/usr/bin/env python3
"""
Scanner and Sanitizer for Minecraft 1.2.5 + Forge Modpack classes for GraalVM.

Legacy obfuscated bytecode from Minecraft 1.2.5 and 2012 Forge mods contains
anonymous and local inner classes with malformed EnclosingMethod/EnclosingConstructor
attributes. When modern OpenJDK/GraalVM's points-to analysis queries reflection metadata
(queryGenericInfo / getGenericInterfaces), it throws:
    java.lang.InternalError: Enclosing constructor not found
or
    java.lang.VerifyError: Bad return type

This script tests classes dynamically and outputs a clean reflect-config.json
excluding broken legacy classes, allowing GraalVM Native Image to complete
points-to analysis successfully.
"""

import json
import os
import subprocess
import sys
import tempfile
import zipfile

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, "..", ".."))
    game_jar = os.path.join(root_dir, "build", "prepared", "game.jar")
    byname_txt = os.path.join(root_dir, "build", "prepared", "byname.txt")
    classes_txt = os.path.join(root_dir, "build", "prepared", "classes.txt")
    out_config = os.path.join(root_dir, "GraalVM-Versions", "config", "reflect-config.json")

    if not os.path.isfile(game_jar):
        print(f"Error: {game_jar} not found. Run ./gradlew prepareGame first.")
        sys.exit(1)

    all_classes = set()
    for txt_file in [byname_txt, classes_txt]:
        if os.path.isfile(txt_file):
            with open(txt_file, "r") as f:
                for line in f:
                    c = line.strip().replace("/", ".")
                    if c:
                        all_classes.add(c)

    if not all_classes:
        with zipfile.ZipFile(game_jar, "r") as zf:
            for name in zf.namelist():
                if name.endswith(".class") and not name.startswith("META-INF/"):
                    all_classes.add(name[:-6].replace("/", "."))

    print(f"Total candidate classes: {len(all_classes)}")

    # Java scanner harness
    java_code = """
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ClassScanner {
    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(args[0]));
        for (String c : lines) {
            try {
                Class<?> cls = Class.forName(c, false, ClassScanner.class.getClassLoader());
                try {
                    cls.getGenericInterfaces();
                    cls.getGenericSuperclass();
                    cls.getEnclosingConstructor();
                    cls.getEnclosingMethod();
                    cls.getDeclaredFields();
                    cls.getDeclaredConstructors();
                } catch (Throwable t) {
                    System.out.println("BAD:" + c + ":" + t.getClass().getSimpleName());
                }
            } catch (Throwable t) {
                System.out.println("BAD:" + c + ":" + t.getClass().getSimpleName());
            }
        }
    }
}
"""
    with tempfile.TemporaryDirectory() as tmpdir:
        scanner_java = os.path.join(tmpdir, "ClassScanner.java")
        class_list_file = os.path.join(tmpdir, "classes.txt")
        with open(scanner_java, "w") as f:
            f.write(java_code)
        with open(class_list_file, "w") as f:
            for c in sorted(all_classes):
                f.write(c + "\n")

        subprocess.check_call(["javac", scanner_java])

        cp = f"{tmpdir}:{game_jar}"
        lwjgl_dir = os.path.expanduser("~/lwjgl-libs")
        if os.path.isdir(lwjgl_dir):
            for j in os.listdir(lwjgl_dir):
                if j.endswith(".jar"):
                    cp += f":{os.path.join(lwjgl_dir, j)}"

        proc = subprocess.run(
            ["java", "-cp", cp, "ClassScanner", class_list_file],
            capture_output=True,
            text=True
        )

        bad_classes = set()
        for line in proc.stdout.splitlines():
            if line.startswith("BAD:"):
                parts = line.split(":")
                bad_classes.add(parts[1])

        print(f"Filtered out {len(bad_classes)} broken/corrupted classes.")

    clean_entries = []
    for c in sorted(all_classes):
        if c in bad_classes:
            continue
        clean_entries.append({
            "name": c,
            "allDeclaredConstructors": True,
            "allPublicConstructors": True,
            "allDeclaredFields": True,
            "allPublicFields": True
        })

    os.makedirs(os.path.dirname(out_config), exist_ok=True)
    with open(out_config, "w") as f:
        json.dump(clean_entries, f, indent=2)

    print(f"Successfully generated {out_config} with {len(clean_entries)} valid classes.")

if __name__ == "__main__":
    main()
