#!/usr/bin/env python3
"""Generate SkriptHub documentation JSON for skript-tebex.

SkriptHub's own docs tool (https://github.com/SkriptHub/SkriptHubDocsTool) is a Bukkit plugin that
reads Skript's registries from a running server, which skript-tebex cannot use: it targets Minestom
through skript-minestom. This script produces the same JSON shape by reading the addon's sources -
the Skript doc annotations, the registered patterns, the event values and the registered types.

Usage:
    python tools/generate_skripthub_docs.py [--output docs/skript-tebex.json]

Upload the result with the JSON import tool of the docs site you publish to (skripthub.net, or the
developer portal at smdocs.hapily.me).
"""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "src/main/java/com/github/cjh3139/skripttebex"
ELEMENTS = SOURCE / "elements"
REGISTRATION = SOURCE / "registration/Registration.java"
EVENTS_FILE = ELEMENTS / "events/TebexEvents.java"

# Doc names Skript gives the classes our syntax returns, used for the "return type" field.
RETURN_TYPES = {
    "Object": "Object",
    "String": "Text",
    "Number": "Number",
    "Player": "Player",
    "Package": "Tebex Package",
    "Category": "Tebex Category",
    "QueuedCommand": "Tebex Command",
    "QueuedPlayer": "Tebex Player",
    "Payment": "Tebex Payment",
    "PlayerLookup": "Tebex Player Lookup",
    "Ban": "Tebex Ban",
    "Coupon": "Tebex Coupon",
    "GiftCard": "Tebex Gift Card",
    "Sale": "Tebex Sale",
    "CommunityGoal": "Tebex Community Goal",
    "ServerInformation": "Tebex Store",
}


# --------------------------------------------------------------------------------------------
# Java source helpers
# --------------------------------------------------------------------------------------------

JAVA_ESCAPES = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f", "s": " ",
                '"': '"', "'": "'", "\\": "\\", "0": "\0"}


def decode_literal(raw: str) -> str:
    """Decode the body of a Java string literal (without its quotes)."""
    out, i = [], 0
    while i < len(raw):
        char = raw[i]
        if char == "\\" and i + 1 < len(raw):
            nxt = raw[i + 1]
            if nxt == "\n":  # line continuation inside a text block
                i += 2
                continue
            out.append(JAVA_ESCAPES.get(nxt, nxt))
            i += 2
            continue
        out.append(char)
        i += 1
    return "".join(out)


def strip_text_block_indent(body: str) -> str:
    """Apply Java's incidental-whitespace rules to a text block body."""
    lines = body.split("\n")
    if lines and lines[0].strip() == "":
        lines = lines[1:]
    significant = [line for line in lines if line.strip()] or lines
    indent = min((len(line) - len(line.lstrip()) for line in significant), default=0)
    return "\n".join(line[indent:].rstrip() for line in lines).strip("\n")


def java_strings(text: str) -> list[str]:
    """Every string literal in `text`, in order, decoded. Text blocks count as one literal."""
    values, i = [], 0
    while i < len(text):
        if text.startswith('"""', i):
            end = text.index('"""', i + 3)
            values.append(decode_literal(strip_text_block_indent(text[i + 3:end])))
            i = end + 3
            continue
        if text[i] == '"':
            j, buf = i + 1, []
            while j < len(text) and text[j] != '"':
                if text[j] == "\\":
                    buf.append(text[j:j + 2])
                    j += 2
                    continue
                buf.append(text[j])
                j += 1
            values.append(decode_literal("".join(buf)))
            i = j + 1
            continue
        i += 1
    return values


def arguments(text: str, start: int) -> tuple[list[str], int]:
    """Split the argument list of the call whose '(' is at `start`. Returns (args, index after ')')."""
    depth, args, current, i = 0, [], [], start
    in_string = False
    while i < len(text):
        char = text[i]
        if in_string:
            current.append(char)
            if char == "\\":
                current.append(text[i + 1])
                i += 2
                continue
            if char == '"':
                in_string = False
            i += 1
            continue
        if text.startswith('"""', i):
            end = text.index('"""', i + 3) + 3
            current.append(text[i:end])
            i = end
            continue
        if char == '"':
            in_string = True
            current.append(char)
            i += 1
            continue
        if char in "([{":
            depth += 1
            if depth == 1 and char == "(" and i == start:
                i += 1
                continue
            current.append(char)
            i += 1
            continue
        if char in ")]}":
            depth -= 1
            if depth == 0 and char == ")":
                args.append("".join(current))
                return [a.strip() for a in args if a.strip()], i + 1
            current.append(char)
            i += 1
            continue
        if char == "," and depth == 1:
            args.append("".join(current))
            current = []
            i += 1
            continue
        current.append(char)
        i += 1
    raise ValueError("unbalanced argument list")


def call_arguments(text: str, call: str) -> list[list[str]]:
    """Argument lists of every `call(` occurrence in `text`."""
    results, index = [], 0
    while True:
        found = text.find(call + "(", index)
        if found == -1:
            return results
        args, end = arguments(text, found + len(call))
        results.append(args)
        index = end


def literal_of(argument: str) -> str:
    """A single argument built from concatenated string literals."""
    return "".join(java_strings(argument))


# --------------------------------------------------------------------------------------------
# Pattern cleaning, mirroring SkriptHubDocsTool's GenerateSyntax.cleanSyntaxInfoPatterns
# --------------------------------------------------------------------------------------------

def clean_pattern(pattern: str) -> str:
    pattern = re.sub(r"\\([()])", r"\1", pattern)
    pattern = re.sub(r"-?\d+¦", "", pattern)
    pattern = pattern.replace("&lt;", "<").replace("&gt;", ">")
    pattern = re.sub(r"%-(.+?)%", lambda m: m.group(0).replace("-", ""), pattern)
    pattern = re.sub(r"%~(.+?)%", lambda m: m.group(0).replace("~", ""), pattern)
    pattern = pattern.replace("()", "")
    pattern = re.sub(r"@-?\d", "", pattern)
    pattern = re.sub(r"(\w+):", "", pattern)
    pattern = pattern.replace("[:", "[").replace("(:", "(").replace("|:", "|")
    return pattern


def clean_user_pattern(pattern: str) -> str:
    """Turn a ClassInfo user pattern (a regex) into something readable, as the docs tool does."""
    pattern = re.sub(r"\((.+?)\)\?", r"[\1]", pattern)
    return re.sub(r"(.)\?", r"[\1]", pattern)


# --------------------------------------------------------------------------------------------
# Syntax collection
# --------------------------------------------------------------------------------------------

def annotations_of(source: str) -> dict[str, list[str]]:
    """The Skript doc annotations of the class declared in `source`."""
    header = source[:source.index("public class ")] if "public class " in source else source
    found: dict[str, list[str]] = {}
    for annotation in ("Name", "Description", "Examples", "Since", "Keywords", "RequiredPlugins"):
        marker = "@" + annotation
        index = header.find(marker + "(")
        if index == -1:
            continue
        args, _ = arguments(header, index + len(marker))
        body = args[0] if args else ""
        if body.startswith("{"):
            inner, _ = arguments("(" + body[1:-1] + ")", 0)
            found[annotation] = [literal_of(part) for part in inner]
        else:
            found[annotation] = [literal_of(body)]
    return found


def syntax_entry(class_name: str, annotations: dict[str, list[str]], patterns: list[str],
                 return_type: str | None = None) -> dict:
    entry: dict[str, object] = {
        "id": class_name,
        "name": annotations.get("Name", [class_name])[0],
    }
    if "Description" in annotations:
        entry["description"] = annotations["Description"]
    if "Examples" in annotations:
        entry["examples"] = annotations["Examples"]
    if "Since" in annotations:
        entry["since"] = annotations["Since"]
    if return_type:
        entry["return type"] = return_type
    entry["patterns"] = [clean_pattern(pattern) for pattern in patterns]
    if "RequiredPlugins" in annotations:
        entry["required plugins"] = annotations["RequiredPlugins"]
    if "Keywords" in annotations:
        entry["keywords"] = annotations["Keywords"]
    return entry


def collect_effects_and_conditions(kind: str, register_call: str) -> list[dict]:
    entries = []
    for path in sorted((ELEMENTS / kind).glob("*.java")):
        source = path.read_text(encoding="utf-8")
        calls = call_arguments(source, register_call)
        if not calls:
            continue
        patterns = [literal_of(argument) for argument in calls[0][1:]]
        entries.append(syntax_entry(path.stem, annotations_of(source), patterns))
    return entries


def collect_expressions() -> list[dict]:
    entries = []
    for path in sorted((ELEMENTS / "expressions").glob("*.java")):
        source = path.read_text(encoding="utf-8")
        simple = call_arguments(source, "Skript.registerExpression")
        if simple:
            args = simple[0]
            return_class = args[1].replace(".class", "").strip()
            patterns = [literal_of(argument) for argument in args[3:]]
        else:
            property_calls = [args for args in call_arguments(source, "register")
                              if args and args[0].endswith(".class")]
            if not property_calls:
                continue
            args = property_calls[0]
            return_class = args[1].replace(".class", "").strip()
            prop, from_type = literal_of(args[2]), literal_of(args[3])
            # PropertyExpression.register builds these two patterns.
            patterns = [f"[the] {prop} of %{from_type}%", f"%{from_type}%'[s] {prop}"]
        return_type = RETURN_TYPES.get(return_class, return_class)
        entries.append(syntax_entry(path.stem, annotations_of(source), patterns, return_type))
    return entries


def event_values() -> dict[str, list[str]]:
    """Event value names per event class, as `event-<pattern>` strings."""
    values: dict[str, list[str]] = {}
    for path in sorted((ELEMENTS / "events").glob("*.java")):
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(r"EventValue\.(simple|builder)\(\s*(\w+)\.class,\s*([\w\[\]]+)\.class",
                                 source):
            style, event_class, value_class = match.groups()
            names = values.setdefault(event_class, [])
            tail = source[match.end():match.end() + 400]
            patterns = re.search(r"\.patterns\(([^)]*)\)", tail) if style == "builder" else None
            if patterns:
                names.extend("event-" + name for name in java_strings(patterns.group(1)))
            else:
                fallback = RETURN_TYPES.get(value_class.replace("[]", ""), value_class)
                names.append("event-" + fallback.lower())
    return {event: sorted(set(names)) for event, names in values.items()}


def collect_events() -> list[dict]:
    source = EVENTS_FILE.read_text(encoding="utf-8")
    values = event_values()
    entries = []
    for match in re.finditer(r"Skript\.registerEvent\(", source):
        args, end = arguments(source, match.end() - 1)
        name = literal_of(args[0])
        event_class = args[2].replace(".class", "").strip()
        patterns = ["[on] " + clean_pattern(literal_of(argument)) for argument in args[3:]]
        chain = source[end:source.index(";", end)]
        entry: dict[str, object] = {
            "id": re.sub(r"\s+", "_", re.sub(r"[#'\"<>/&]", "", name.lower())),
            "name": name,
        }
        for key, field in (("description", "description"), ("examples", "examples"),
                           ("since", "since")):
            calls = call_arguments(chain, "." + key)
            if calls:
                entry[field] = [literal_of(argument) for argument in calls[0]]
        cancellable = "implements Cancellable" in (
            ELEMENTS / "events" / f"{event_class}.java").read_text(encoding="utf-8")
        entry["patterns"] = patterns
        if values.get(event_class):
            entry["event values"] = values[event_class]
        entry["cancellable"] = cancellable
        entries.append(entry)
    return entries


def collect_types() -> list[dict]:
    source = REGISTRATION.read_text(encoding="utf-8")
    entries = []
    for match in re.finditer(r"new ClassInfo<>\((\w+)\.class,\s*\"(\w+)\"\)", source):
        model_class, code_name = match.groups()
        chain = source[match.end():]
        entry: dict[str, object] = {"id": model_class, "name": code_name}
        for key, field in (("name", "name"), ("description", "description"),
                           ("examples", "examples"), ("since", "since")):
            call = re.search(r"\.%s\(" % key, chain)
            if not call:
                continue
            args, _ = arguments(chain, call.end() - 1)
            texts = [literal_of(argument) for argument in args]
            entry[field] = texts[0] if field == "name" else texts
        user = re.search(r"\.user\(", chain)
        entry["patterns"] = ([clean_user_pattern(literal_of(arguments(chain, user.end() - 1)[0][0]))]
                             if user else [code_name])
        entries.append(entry)
    return entries


def addon_version() -> str:
    build = (ROOT / "build.gradle").read_text(encoding="utf-8")
    match = re.search(r"^version\s+'([^']+)'", build, re.M)
    return match.group(1) if match else "unknown"


def build_documentation() -> dict:
    documentation: dict[str, object] = {"metadata": {"version": addon_version()}}
    sections = [
        ("events", collect_events()),
        ("conditions", collect_effects_and_conditions("conditions", "Skript.registerCondition")),
        ("effects", collect_effects_and_conditions("effects", "Skript.registerEffect")),
        ("expressions", collect_expressions()),
        ("types", collect_types()),
    ]
    for name, entries in sections:
        if entries:
            documentation[name] = entries
    return documentation


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default=str(ROOT / "docs/skript-tebex.json"),
                        help="where to write the JSON (default: docs/skript-tebex.json)")
    options = parser.parse_args()

    documentation = build_documentation()
    output = pathlib.Path(options.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(documentation, indent=2, ensure_ascii=False) + "\n",
                      encoding="utf-8")

    counts = ", ".join(f"{len(value)} {key}" for key, value in documentation.items()
                       if isinstance(value, list))
    print(f"wrote {output} ({counts})")

    missing = [entry["id"] for key, value in documentation.items() if isinstance(value, list)
               for entry in value if not entry.get("description")]
    if missing:
        print("warning: no description for " + ", ".join(missing), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
