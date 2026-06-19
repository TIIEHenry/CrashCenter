#!/bin/bash
# 文档索引生成脚本（CrashCenter 项目）
# 用法: ./scripts/generate-docs-index.sh
#
# 自动生成 docs/INDEX.md，扫描以下目录：
#   docs/architecture/
#   docs/decisions/
#   docs/reference/
#   docs/guides/
#   dev/roadmap/
#   dev/plans/
#   dev/progress/

set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DOCS_DIR="$REPO_ROOT/docs"
DEV_DIR="$REPO_ROOT/dev"
INDEX_FILE="$DOCS_DIR/INDEX.md"

extract_frontmatter_field() {
    local file=$1
    local field=$2
    sed 's/\r$//' "$file" 2>/dev/null \
        | sed -n '/^---$/,/^---$/p' \
        | grep "^${field}:" \
        | head -1 \
        | sed "s/^${field}: *//; s/^\"//; s/\"$//"
}

get_title() {
    local file=$1
    local title
    title=$(extract_frontmatter_field "$file" "title")
    if [ -z "$title" ]; then
        title=$(head -1 "$file" 2>/dev/null | sed 's/^# *//')
    fi
    echo "$title"
}

get_summary() {
    local file=$1
    extract_frontmatter_field "$file" "summary"
}

generate_architecture_section() {
    echo "## 架构方案（\`docs/architecture/\`）"
    echo ""
    echo "| 文档 | 内容 |"
    echo "|------|------|"

    for f in "$DOCS_DIR/architecture"/*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local title=$(get_title "$f")
        local summary=$(get_summary "$f")
        echo "| [$name](architecture/$name) | ${summary:-$title} |"
    done

    if [ -d "$DOCS_DIR/architecture/concepts" ]; then
        echo ""
        echo "### 概念（\`docs/architecture/concepts/\`）"
        echo ""
        echo "| 文档 | 内容 |"
        echo "|------|------|"
        for f in "$DOCS_DIR/architecture/concepts"/*.md; do
            [ -f "$f" ] || continue
            local name=$(basename "$f")
            local title=$(get_title "$f")
            local summary=$(get_summary "$f")
            echo "| [$name](architecture/concepts/$name) | ${summary:-$title} |"
        done
    fi
}

generate_decisions_section() {
    echo ""
    echo "---"
    echo ""
    echo "## 架构决策（\`docs/decisions/\`）"
    echo ""
    echo "| ADR | 决策 | 状态 |"
    echo "|-----|------|------|"

    for f in "$DOCS_DIR/decisions"/*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local title=$(get_title "$f")
        local summary=$(get_summary "$f")
        local status=$(extract_frontmatter_field "$f" "status")
        local display_status=""
        case "$status" in
            archived) display_status="_(已归档)_" ;;
            *) display_status="" ;;
        esac
        echo "| [$name](decisions/$name) | ${summary:-$title} | $display_status |"
    done
}

generate_reference_section() {
    echo ""
    echo "---"
    echo ""
    echo "## 参考资料（\`docs/reference/\`）"
    echo ""
    echo "| 文档 | 内容 |"
    echo "|------|------|"
    for f in "$DOCS_DIR/reference"/*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local title=$(get_title "$f")
        local summary=$(get_summary "$f")
        echo "| [$name](reference/$name) | ${summary:-$title} |"
    done
}

generate_guides_section() {
    echo ""
    echo "---"
    echo ""
    echo "## 指南（\`docs/guides/\`）"
    echo ""
    echo "| 文档 | 内容 |"
    echo "|------|------|"
    for f in "$DOCS_DIR/guides"/*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local title=$(get_title "$f")
        local summary=$(get_summary "$f")
        echo "| [$name](guides/$name) | ${summary:-$title} |"
    done
}

generate_roadmap_section() {
    echo ""
    echo "---"
    echo ""
    echo "## 开发追踪（\`dev/\`）"
    echo ""
    echo "| 目录 | 说明 |"
    echo "|------|------|"
    echo "| [dev/README.md](../dev/README.md) | dev/ 使用说明 |"
    echo "| [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) | 开发速查手册 |"
    echo ""
    echo "### 路线图（\`dev/roadmap/\`）"
    echo ""
    echo "#### Active"
    echo ""
    echo "| Phase | 文档 | 说明 | 状态 |"
    echo "|-------|------|------|------|"

    for f in "$DEV_DIR/roadmap/active"/phase*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local relpath="../dev/roadmap/active/$name"
        local title=$(get_title "$f")
        local status=$(extract_frontmatter_field "$f" "status")
        local phase_num=$(echo "$name" | grep -oP 'phase\K\d+' || echo "—")
        local short_desc=$(echo "$title" | sed 's/^Phase [0-9]*[:：] *//')
        local display_status=""
        case "$status" in
            complete|completed|archived) display_status="✅" ;;
            in_progress|in-progress) display_status="🔄" ;;
            *) display_status="" ;;
        esac
        echo "| $phase_num | [$name]($relpath) | $short_desc | $display_status |"
    done

    echo ""
    echo "#### Archive"
    echo ""
    echo "| Phase | 文档 | 说明 | 状态 |"
    echo "|-------|------|------|------|"

    for f in "$DEV_DIR/roadmap/archive"/phase*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local relpath="../dev/roadmap/archive/$name"
        local title=$(get_title "$f")
        local phase_num=$(echo "$name" | grep -oP 'phase\K\d+' || echo "—")
        local short_desc=$(echo "$title" | sed 's/^Phase [0-9]*[:：] *//')
        echo "| $phase_num | [$name]($relpath) | $short_desc | ✅ |"
    done
}

generate_plans_section() {
    echo ""
    echo "### 实施计划（\`dev/plans/\`）"
    echo ""
    echo "| 文档 | 内容 |"
    echo "|------|------|"
    if [ -d "$DEV_DIR/plans" ]; then
        for f in "$DEV_DIR/plans"/*.md; do
            [ -f "$f" ] || continue
            local name=$(basename "$f")
            local title=$(get_title "$f")
            local summary=$(get_summary "$f")
            echo "| [$name](../dev/plans/$name) | ${summary:-$title} |"
        done
    fi
}

generate_progress_section() {
    echo ""
    echo "### 进度追踪（\`dev/progress/\`）"
    echo ""
    echo "| 文档 | 内容 |"
    echo "|------|------|"
    for f in "$DEV_DIR/progress"/*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        local title=$(get_title "$f")
        local summary=$(get_summary "$f")
        echo "| [$name](../dev/progress/$name) | ${summary:-$title} |"
    done
}

generate_verification_section() {
    echo ""
    echo "### 设备验收（\`dev/verification/\`）"
    echo ""
    echo "| 文档 | 内容 |"
    echo "|------|------|"
    if [ -f "$DEV_DIR/verification/README.md" ]; then
        local title=$(get_title "$DEV_DIR/verification/README.md")
        local summary=$(get_summary "$DEV_DIR/verification/README.md")
        echo "| [README.md](../dev/verification/README.md) | ${summary:-$title} |"
    fi
    for f in "$DEV_DIR/verification"/*.md; do
        [ -f "$f" ] || continue
        local name=$(basename "$f")
        [ "$name" = "README.md" ] && continue
        local title=$(get_title "$f")
        local summary=$(get_summary "$f")
        echo "| [$name](../dev/verification/$name) | ${summary:-$title} |"
    done
}

generate_reading_paths() {
    echo ""
    echo "---"
    echo ""
    echo "## 按阅读路径"
    echo ""
    echo "### 新人入门"
    echo "1. [AGENTS.md](../AGENTS.md) — 项目全貌"
    echo "2. [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) — 开发速查"
    echo "3. [architecture/overview.md](architecture/overview.md) — 系统总览"
    echo "4. [guides/usage.md](guides/usage.md) — 用户使用"
    echo ""
    echo "### 若关注 Xposed hook 机制"
    echo "1. [architecture/xposed-entry.md](architecture/xposed-entry.md)"
    echo "2. [architecture/crash-handler.md](architecture/crash-handler.md)"
    echo "3. [decisions/001-looper-loop-resurrection.md](decisions/001-looper-loop-resurrection.md)"
    echo ""
    echo "### 若关注配置与 scope"
    echo "1. [architecture/scope-and-prefs.md](architecture/scope-and-prefs.md)"
    echo "2. [architecture/configuration-ui.md](architecture/configuration-ui.md)"
    echo "3. [decisions/002-inverted-package-toggle.md](decisions/002-inverted-package-toggle.md)"
}

echo "=== 生成文档索引 ==="
echo "扫描目录: $DOCS_DIR, $DEV_DIR"

TMPFILE=$(mktemp)
trap "rm -f $TMPFILE" EXIT

{
    echo "---"
    echo "title: \"文档索引\""
    echo "type: concept"
    echo "status: accepted"
    echo "phase: N/A"
    echo "updated: $(date +%Y-%m-%d)"
    echo "summary: \"docs/ + dev/ 完整导航索引（自动生成）\""
    echo "---"
    echo ""
    echo "# CrashCenter 文档索引"
    echo ""
    echo "> **本文件由脚本自动生成，请勿手动编辑。**"
    echo "> Xposed 异常拦截模块文档集"
    echo ""
    echo "---"
    echo ""
    echo "## 文档系统"
    echo ""
    echo "| 文件 | 说明 |"
    echo "|------|------|"
    echo "| [DOCUMENTATION.md](DOCUMENTATION.md) | **LLM 维护规则** |"
    echo "| [DOC-SPEC.md](DOC-SPEC.md) | **文档系统规范** |"
    echo "| [glossary.md](glossary.md) | **术语表** |"
    echo "| [AGENTS.md](../AGENTS.md) | **项目权威入口** |"
    echo "| [dev/DEV_GUIDE.md](../dev/DEV_GUIDE.md) | **开发速查手册** |"
    echo ""
    echo "---"
    echo ""

    generate_architecture_section
    generate_decisions_section
    generate_reference_section
    generate_guides_section
    generate_roadmap_section
    generate_plans_section
    generate_progress_section
    generate_verification_section
    generate_reading_paths

    echo ""
    echo "---"
    echo ""
    echo "*索引生成日期：$(date +%Y-%m-%d)*"
} > "$TMPFILE"

cp "$TMPFILE" "$INDEX_FILE"

echo "=== 索引生成完成: $INDEX_FILE ==="
