#!/usr/bin/env python3
"""Aggregate crash report from exported events.jsonl.

Reads one-JSON-per-line CrashEvent records and produces a Markdown summary:
  - Total crashes, unique apps, time range
  - TOP-N exception classes
  - TOP-N most-crashed apps
  - Daily crash counts
  - Ingestion-source distribution (ingestedFrom)
  - Backend-write distribution (backendWritten)

Usage examples:
  python scripts/analyze-crashes.py events.jsonl
  python scripts/analyze-crashes.py --input events.jsonl --output report.md
  python scripts/analyze-crashes.py events.jsonl --top 10
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from datetime import datetime, timezone


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Generate Markdown crash-aggregation report from events.jsonl.",
    )
    p.add_argument(
        "input_file",
        nargs="?",
        help="Path to events.jsonl (or use --input)",
    )
    p.add_argument(
        "--input",
        dest="input_flag",
        help="Path to events.jsonl",
    )
    p.add_argument("--output", help="Output Markdown file (default: stdout)")
    p.add_argument("--top", type=int, default=5, help="TOP-N entries (default: 5)")
    return p.parse_args()


def load_events(path: str) -> list[dict]:
    events: list[dict] = []
    with open(path, encoding="utf-8") as fh:
        for lineno, line in enumerate(fh, 1):
            line = line.strip()
            if not line:
                continue
            try:
                events.append(json.loads(line))
            except json.JSONDecodeError:
                print(f"WARN: skipping invalid JSON at line {lineno}", file=sys.stderr)
    return events


def fmt_ts(ms: float) -> str:
    return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%d %H:%M UTC")


def daily_key(ms: float) -> str:
    return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%d")


def top_n(counter: Counter, n: int) -> list[tuple[str, int]]:
    return counter.most_common(n)


def render_table(headers: list[str], rows: list[list[str]]) -> str:
    col_widths = [len(h) for h in headers]
    for row in rows:
        for i, cell in enumerate(row):
            col_widths[i] = max(col_widths[i], len(cell))
    fmt = " | ".join(f"{{:<{w}}}" for w in col_widths)
    lines = [fmt.format(*headers), " | ".join("-" * w for w in col_widths)]
    for row in rows:
        lines.append(fmt.format(*row))
    return "\n".join(lines)


def build_report(events: list[dict], top: int) -> str:
    if not events:
        return "# Crash Report\n\nNo events found.\n"

    timestamps = [e.get("timestampMs", 0) for e in events]
    timestamps_valid = [t for t in timestamps if t > 0]

    total = len(events)
    packages = {e.get("packageName", "") for e in events}
    packages.discard("")
    apps_count = len(packages)

    if timestamps_valid:
        ts_min = min(timestamps_valid)
        ts_max = max(timestamps_valid)
        time_range = f"{fmt_ts(ts_min)} -- {fmt_ts(ts_max)}"
    else:
        time_range = "N/A"

    # Counters
    exc_counter: Counter = Counter()
    pkg_counter: Counter = Counter()
    daily_counter: Counter = Counter()
    source_counter: Counter = Counter()
    backend_counter: Counter = Counter()

    for e in events:
        exc_class = e.get("exceptionClass") or "Unknown"
        exc_counter[exc_class] += 1

        pkg = e.get("packageName") or "(unknown)"
        pkg_counter[pkg] += 1

        ts = e.get("timestampMs", 0)
        if ts > 0:
            daily_counter[daily_key(ts)] += 1

        ingested = e.get("ingestedFrom")
        if ingested:
            source_counter[ingested] += 1

        for backend in e.get("backendWritten") or []:
            backend_counter[backend] += 1

    # Build Markdown
    lines: list[str] = []
    lines.append("# Crash Aggregation Report")
    lines.append("")
    lines.append(f"- **Total crashes:** {total}")
    lines.append(f"- **Unique apps:** {apps_count}")
    lines.append(f"- **Time range:** {time_range}")
    lines.append("")

    # Top exception classes
    lines.append(f"## TOP {top} Exception Classes")
    lines.append("")
    exc_rows = [[name, str(cnt)] for name, cnt in top_n(exc_counter, top)]
    lines.append(render_table(["Exception Class", "Count"], exc_rows))
    lines.append("")

    # Top apps
    lines.append(f"## TOP {top} Crashed Apps")
    lines.append("")
    pkg_rows = [[name, str(cnt)] for name, cnt in top_n(pkg_counter, top)]
    lines.append(render_table(["Package Name", "Count"], pkg_rows))
    lines.append("")

    # Daily counts
    lines.append("## Daily Crash Counts")
    lines.append("")
    daily_sorted = sorted(daily_counter.items())
    daily_rows = [[day, str(cnt)] for day, cnt in daily_sorted]
    lines.append(render_table(["Date", "Count"], daily_rows))
    lines.append("")

    # Ingestion source distribution
    if source_counter:
        lines.append("## Ingestion Source Distribution")
        lines.append("")
        src_rows = [[name, str(cnt)] for name, cnt in source_counter.most_common()]
        lines.append(render_table(["Source", "Count"], src_rows))
        lines.append("")

    # Backend write distribution
    if backend_counter:
        lines.append("## Backend Write Distribution")
        lines.append("")
        be_rows = [[name, str(cnt)] for name, cnt in backend_counter.most_common()]
        lines.append(render_table(["Backend", "Events Written"], be_rows))
        lines.append("")

    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    input_path = args.input_flag or args.input_file
    if not input_path:
        print("Error: provide input file via positional arg or --input", file=sys.stderr)
        return 1

    events = load_events(input_path)
    report = build_report(events, args.top)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as fh:
            fh.write(report)
        print(f"Report written to {args.output} ({len(events)} events)", file=sys.stderr)
    else:
        print(report)

    return 0


if __name__ == "__main__":
    sys.exit(main())
