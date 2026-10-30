#!/usr/bin/env python3
"""Validates a WristBlocks app file and prints build settings.

Every app file is untrusted input (anyone can open a pull request), so this
runs before any build and rejects anything outside the small allowed format.

Usage:
  python3 tools/validate.py apps/tally.json          # check, print KEY=VALUE lines
  python3 tools/validate.py --github-env apps/x.json # also append to $GITHUB_ENV
  python3 tools/validate.py --selftest               # run the built-in tests
"""
import json
import os
import re
import sys

BLOCK_FIELDS = {
    "title": {"text"},
    "text": {"text"},
    "counter": {"label", "goal", "step", "gesture"},
    "timer": {"label", "minutes", "gesture"},
}
NAME_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 .'&!?-]{0,23}$")
ID_RE = re.compile(r"^[a-z][a-z0-9]{1,30}$")
COLOR_RE = re.compile(r"^#[0-9A-Fa-f]{6}$")
MAX_BLOCKS = 8
MAX_TEXT = 40


class Invalid(ValueError):
    pass


def _text(v, where):
    if not isinstance(v, str) or not v.strip() or len(v) > MAX_TEXT or any(ord(c) < 32 for c in v):
        raise Invalid(f"{where}: must be 1 to {MAX_TEXT} characters of plain text")
    return v.strip()


def _int(v, where, lo, hi):
    if not isinstance(v, int) or isinstance(v, bool) or not lo <= v <= hi:
        raise Invalid(f"{where}: must be a whole number from {lo} to {hi}")
    return v


def validate(spec):
    """Returns a cleaned copy of the spec, or raises Invalid with a friendly message."""
    if not isinstance(spec, dict):
        raise Invalid("the app file must be a JSON object")
    extra = set(spec) - {"name", "id", "color", "blocks"}
    if extra:
        raise Invalid(f"unknown setting(s): {', '.join(sorted(extra))}")
    name = spec.get("name")
    if not isinstance(name, str) or not NAME_RE.match(name):
        raise Invalid("name: 1 to 24 letters, digits, spaces or . ' & ! ? -")
    app_id = spec.get("id")
    if not isinstance(app_id, str) or not ID_RE.match(app_id):
        raise Invalid("id: 2 to 31 lowercase letters or digits, starting with a letter (e.g. tally)")
    color = spec.get("color", "#F2C46D")
    if not isinstance(color, str) or not COLOR_RE.match(color):
        raise Invalid("color: a hex colour like #F2C46D")
    blocks = spec.get("blocks")
    if not isinstance(blocks, list) or not 1 <= len(blocks) <= MAX_BLOCKS:
        raise Invalid(f"blocks: between 1 and {MAX_BLOCKS} blocks")

    clean, gesture_taken = [], False
    for i, b in enumerate(blocks, 1):
        where = f"block {i}"
        if not isinstance(b, dict) or b.get("type") not in BLOCK_FIELDS:
            raise Invalid(f"{where}: type must be one of {', '.join(BLOCK_FIELDS)}")
        t = b["type"]
        extra = set(b) - BLOCK_FIELDS[t] - {"type"}
        if extra:
            raise Invalid(f"{where} ({t}): unknown field(s) {', '.join(sorted(extra))}")
        out = {"type": t}
        if t in ("title", "text"):
            out["text"] = _text(b.get("text"), f"{where} text")
        if t in ("counter", "timer"):
            out["label"] = _text(b.get("label", "Counter" if t == "counter" else "Timer"), f"{where} label")
            g = b.get("gesture", False)
            if not isinstance(g, bool):
                raise Invalid(f"{where} gesture: true or false")
            if g and gesture_taken:
                raise Invalid(f"{where}: only one block can use the double tap / double pinch gesture")
            gesture_taken |= g
            out["gesture"] = g
        if t == "counter":
            out["goal"] = _int(b.get("goal", 0), f"{where} goal", 0, 100000)
            out["step"] = _int(b.get("step", 1), f"{where} step", 1, 1000)
        if t == "timer":
            out["minutes"] = _int(b.get("minutes"), f"{where} minutes", 1, 180)
        clean.append(out)
    return {"name": name, "id": app_id, "color": color.upper(), "blocks": clean}


def _selftest():
    good = {"name": "Tally", "id": "tally", "blocks": [{"type": "counter", "goal": 33, "gesture": True}]}
    assert validate(good)["blocks"][0] == {"type": "counter", "label": "Counter", "gesture": True, "goal": 33, "step": 1}
    bad = [
        {**good, "id": "Tally"},                       # uppercase id
        {**good, "id": "x;rm -rf"},                    # injection attempt
        {**good, "name": "a" * 30},                    # too long
        {**good, "color": "red"},                      # not hex
        {**good, "blocks": []},                        # empty
        {**good, "blocks": [{"type": "shell"}]},       # unknown block
        {**good, "blocks": [{"type": "timer", "minutes": 0}]},
        {**good, "blocks": [{"type": "counter", "gesture": True}, {"type": "timer", "minutes": 5, "gesture": True}]},
        {**good, "blocks": [{"type": "text", "text": "a\nb"}]},
        {**good, "extra": 1},
    ]
    for i, spec in enumerate(bad):
        try:
            validate(spec)
        except Invalid:
            continue
        raise AssertionError(f"bad spec {i} was accepted")
    here = os.path.dirname(os.path.abspath(__file__))
    for f in sorted(os.listdir(os.path.join(here, "..", "apps"))):
        if f.endswith(".json"):
            validate(json.load(open(os.path.join(here, "..", "apps", f))))
    print("selftest ok")


def main(argv):
    if argv[1:] == ["--selftest"]:
        return _selftest()
    to_env = "--github-env" in argv
    paths = [a for a in argv[1:] if not a.startswith("--")]
    if len(paths) != 1:
        sys.exit(__doc__)
    try:
        with open(paths[0], encoding="utf-8") as fh:
            spec = validate(json.load(fh))
    except (OSError, json.JSONDecodeError) as e:
        sys.exit(f"{paths[0]}: cannot read app file ({e})")
    except Invalid as e:
        sys.exit(f"{paths[0]}: {e}")
    lines = [f"APP_ID={spec['id']}", f"APP_NAME={spec['name']}", f"APP_COLOR={spec['color']}"]
    print("\n".join(lines))
    if to_env and os.environ.get("GITHUB_ENV"):
        with open(os.environ["GITHUB_ENV"], "a") as fh:
            fh.write("\n".join(lines) + "\n")
    out = os.environ.get("WRISTBLOCKS_CLEAN_JSON")
    if out:
        with open(out, "w", encoding="utf-8") as fh:
            json.dump(spec, fh, indent=2)


if __name__ == "__main__":
    main(sys.argv)
