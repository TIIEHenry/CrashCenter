#!/bin/bash
# CrashCenter adb smoke 验收脚本
# 用法: ./scripts/adb-smoke-verification.sh [-s SERIAL] [--skip-build]

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE="nota.android.crash.xp.app"
ACTIVITY="${PACKAGE}/.shell.MainShellActivity"
LOCK_FILE="$REPO_ROOT/dev/verification/.adb-test.lock"
ADB_SERIAL=""
SKIP_BUILD=0

usage() {
    cat <<'EOF'
CrashCenter adb smoke verification

用法:
  ./scripts/adb-smoke-verification.sh [选项]

选项:
  -s SERIAL       adb 设备 serial（或 adb connect 地址）
  --skip-build    跳过 ./gradlew :app:assembleDebug
  -h, --help      显示帮助

步骤:
  1. flock 单会话锁
  2. 检查 adb 设备
  3. assembleDebug（可跳过）
  4. adb install -r
  5. 启动 MainShellActivity（LAUNCHER）
  6. 抓取 logcat 关键字（10s）

手动后续:
  菜单 → Test → 确认 Toast 且 app 不退出
  报告模板: docs/templates/verification-template.md
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -s) ADB_SERIAL="$2"; shift 2 ;;
        --skip-build) SKIP_BUILD=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "未知参数: $1" >&2; usage; exit 1 ;;
    esac
done

adb_cmd() {
    if [[ -n "$ADB_SERIAL" ]]; then
        adb -s "$ADB_SERIAL" "$@"
    else
        adb "$@"
    fi
}

acquire_lock() {
    mkdir -p "$(dirname "$LOCK_FILE")"
    exec 200>"$LOCK_FILE"
    if ! flock -n 200; then
        echo "ERROR: 另一个 adb 验收会话正在运行（$LOCK_FILE）" >&2
        exit 1
    fi
}

check_device() {
    local count
    if [[ -n "$ADB_SERIAL" ]]; then
        count=$(adb devices | grep -c "^${ADB_SERIAL}[[:space:]]*device$" || true)
    else
        count=$(adb devices | grep -c '[[:space:]]device$' || true)
    fi
    if [[ "$count" -lt 1 ]]; then
        echo "ERROR: 无可用 adb 设备" >&2
        adb devices
        exit 1
    fi
    echo "=== adb 设备 OK ==="
}

find_apk() {
    local apk
    apk=$(find "$REPO_ROOT/app/build/outputs/apk/debug" -name 'CrashCenter_v*_debug.apk' 2>/dev/null | head -1)
    if [[ -z "$apk" ]]; then
        apk=$(find "$REPO_ROOT/app/build/outputs/apk/debug" -name '*.apk' 2>/dev/null | head -1)
    fi
    echo "$apk"
}

build_if_needed() {
    if [[ "$SKIP_BUILD" -eq 1 ]]; then
        echo "=== 跳过编译 (--skip-build) ==="
        return
    fi
    echo "=== 编译 :app:assembleDebug ==="
    (cd "$REPO_ROOT" && ./gradlew :app:assembleDebug)
}

install_apk() {
    local apk
    apk=$(find_apk)
    if [[ -z "$apk" || ! -f "$apk" ]]; then
        echo "ERROR: 未找到 debug APK，请先编译" >&2
        exit 1
    fi
    echo "=== 安装 $apk ==="
    adb_cmd install -r "$apk"
}

launch_app() {
    echo "=== 启动 $ACTIVITY ==="
    adb_cmd shell am start -n "$ACTIVITY" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
}

check_logcat() {
    echo "=== 抓取 logcat（10s，关键字: catch package / XposedEntry / selfCheck）==="
    adb_cmd logcat -c || true
    sleep 2
    adb_cmd logcat -t 200 -s XposedBridge:V XposedEntry:E 2>/dev/null | grep -E "catch package|onCreate|selfCheck|XposedEntry" || true
    echo ""
    echo "=== Smoke 自动化部分完成 ==="
    echo "请手动：菜单 → Test → 确认 2 秒后 Toast 且 app 不退出"
    echo "完整报告见 dev/verification/README.md"
}

main() {
    acquire_lock
    check_device
    build_if_needed
    install_apk
    launch_app
    check_logcat
}

main
