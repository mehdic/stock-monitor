#!/bin/bash

# Script to install Linux-specific binaries for Claude Code environment
# Run this on Mac after npm install to add Linux binaries alongside Mac binaries

set -e  # Exit on error

echo "🔧 Installing Linux-specific binaries for Claude Code..."

cd "$(dirname "$0")"

# Array of packages: "package-name|target-directory|version"
PACKAGES=(
    "@rollup/rollup-linux-x64-gnu|node_modules/@rollup/rollup-linux-x64-gnu|4.52.5"
    "@esbuild/linux-x64|node_modules/@esbuild/linux-x64|0.21.5"
)

for entry in "${PACKAGES[@]}"; do
    IFS='|' read -r PACKAGE TARGET VERSION <<< "$entry"

    echo ""
    echo "📦 Installing ${PACKAGE}@${VERSION}..."

    # Download package
    npm pack "${PACKAGE}@${VERSION}"

    # Find the downloaded tarball
    TARBALL=$(ls | grep "^$(echo $PACKAGE | sed 's/@//g' | sed 's/\//-/g')" | grep ".tgz$")

    if [ -z "$TARBALL" ]; then
        echo "❌ Failed to download ${PACKAGE}"
        exit 1
    fi

    echo "   Extracting ${TARBALL}..."
    tar -xzf "${TARBALL}"

    # Remove existing directory if present
    if [ -d "${TARGET}" ]; then
        echo "   Removing existing ${TARGET}..."
        rm -rf "${TARGET}"
    fi

    # Create parent directory if needed
    mkdir -p "$(dirname "${TARGET}")"

    # Move extracted package to target location
    echo "   Moving to ${TARGET}..."
    mv package "${TARGET}"

    # Clean up tarball
    rm "${TARBALL}"

    echo "   ✅ ${PACKAGE} installed successfully"
done

echo ""
echo "🎉 All Linux binaries installed successfully!"
echo ""
echo "📋 Installed binaries:"
ls -la node_modules/@rollup/ 2>/dev/null | grep -E "(darwin|linux)" || true
ls -la node_modules/@esbuild/ 2>/dev/null | grep -E "(darwin|linux)" || true

echo ""
echo "🚀 Now commit and push:"
echo "   git add -f node_modules/@rollup/rollup-linux-x64-gnu/"
echo "   git add -f node_modules/@esbuild/linux-x64/"
echo "   git commit -m 'Add Linux binaries for Rollup and esbuild'"
echo "   git push origin claude/fix-remaining-test-failures-011CUpXsSLGez6dF9oRyEit8"
