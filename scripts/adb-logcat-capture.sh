#!/usr/bin/env bash
# adb-logcat-capture.sh — Capture and optionally parse logcat for crash analysis.
#
# Usage:
#   ./scripts/adb-logcat-capture.sh [options]
#
# Options:
#   -n, --lines N       Capture last N lines (default: 5000)
#   -p, --package PKG   Filter output for a specific package name
#   -o, --output DIR    Output directory (default: dev/verification)
#   -f, --filter        Filter for crash-related entries only
#   -h, --help          Show this help
#
# Examples:
#   ./scripts/adb-logcat-capture.sh
#   ./scripts/adb-logcat-capture.sh --lines 10000 --package com.example.app
#   ./scripts/adb-logcat-capture.sh --filter

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

LINES=5000
PACKAGE=""
OUTPUT_DIR="$PROJECT_ROOT/dev/verification"
FILTER_CRASH=false

usage() {
    sed -n '2,/^$/p' "$0" | sed 's/^# \?//'
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -n|--lines)   LINES="$2"; shift 2 ;;
        -p|--package) PACKAGE="$2"; shift 2 ;;
        -o|--output)  OUTPUT_DIR="$2"; shift 2 ;;
        -f|--filter)  FILTER_CRASH=true; shift ;;
        -h|--help)    usage ;;
        *)            echo "Unknown option: $1" >&2; exit 1 ;;
    esac
done

if ! command -v adb &>/dev/null; then
    echo "Error: adb not found in PATH" >&2
    exit 1
fi

# Check device connected
if ! adb get-state &>/dev/null; then
    echo "Error: no adb device connected" >&2
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUTFILE="$OUTPUT_DIR/logcat_${TIMESTAMP}.txt"

echo "Capturing last $LINES logcat lines..."
adb logcat -d -t "$LINES" > "$OUTFILE"

if [[ -n "$PACKAGE" ]]; then
    echo "Filtering for package: $PACKAGE"
    grep -i "$PACKAGE" "$OUTFILE" > "${OUTFILE%.txt}_filtered.txt" 2>/dev/null || true
    echo "Filtered output: ${OUTFILE%.txt}_filtered.txt"
fi

if [[ "$FILTER_CRASH" == "true" ]]; then
    echo "Filtering for crash-related entries..."
    grep -E "(FATAL|AndroidRuntime|XposedBridge|Exception|Error:|catch package|selfCheck|Fatal signal|Process.*has died)" \
        "$OUTFILE" > "${OUTFILE%.txt}_crash.txt" 2>/dev/null || true
    echo "Crash-filtered output: ${OUTFILE%.txt}_crash.txt"
fi

TOTAL_LINES=$(wc -l < "$OUTFILE")
echo "Done. $TOTAL_LINES lines saved to: $OUTFILE"

# Summary of crash-related entries
CRASH_COUNT=$(grep -cE "(FATAL|AndroidRuntime|XposedBridge|Exception|catch package|selfCheck)" "$OUTFILE" 2>/dev/null || echo "0")
echo ""
echo "=== Logcat Summary ==="
echo "Total lines: $TOTAL_LINES"
echo "Crash-related entries: $CRASH_COUNT"

# Check for selfCheck
SELFCHECK_COUNT=$(grep -c "selfCheck" "$OUTFILE" 2>/dev/null || echo "0")
echo "Module selfCheck markers: $SELFCHECK_COUNT"

# Check for FATAL EXCEPTION
FATAL_COUNT=$(grep -c "FATAL EXCEPTION" "$OUTFILE" 2>/dev/null || echo "0")
echo "FATAL EXCEPTION entries: $FATAL_COUNT"

# Check for XposedBridge exceptions
XPOSED_COUNT=$(grep -c "XposedBridge" "$OUTFILE" 2>/dev/null || echo "0")
echo "XposedBridge entries: $XPOSED_COUNT"
