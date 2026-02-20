#!/usr/bin/env bash
set -euo pipefail

# Build yaba-web-components and copy dist to Core composeResources.
# Run this before building the app when web components have changed.
#
# Usage: ./Scripts/build_web_components.sh

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WEB_COMPONENTS_DIR="$ROOT/Extensions/yaba-web-components"
DIST_DIR="$WEB_COMPONENTS_DIR/dist"
TARGET_DIR="$ROOT/Core/YABACore/src/commonMain/composeResources/files/web-components"

if [[ ! -d "$WEB_COMPONENTS_DIR" ]]; then
    echo "Error: Web components directory not found: $WEB_COMPONENTS_DIR"
    exit 1
fi

echo "Building web components..."
cd "$WEB_COMPONENTS_DIR"
npm run build

if [[ ! -d "$DIST_DIR" ]]; then
    echo "Error: Build did not produce dist directory: $DIST_DIR"
    exit 1
fi

echo "Copying dist to Core composeResources..."
mkdir -p "$TARGET_DIR"
rm -rf "$TARGET_DIR"/*
cp -R "$DIST_DIR"/* "$TARGET_DIR"

echo "Done. Web components copied to $TARGET_DIR"
