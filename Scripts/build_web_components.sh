#!/usr/bin/env bash
set -euo pipefail

# Build yaba-web-components and copy dist into the Android app assets and Extensions/yaba-web-components/WebComponents.
# Run this before building the app when web components have changed.
#
# Usage: ./Scripts/build_web_components.sh

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WEB_COMPONENTS_DIR="$ROOT/Extensions/yaba-web-components"
DIST_DIR="$WEB_COMPONENTS_DIR/dist"
COMPOSE_TARGET_DIR="$ROOT/Compose/YABA/app/src/main/assets/files/web-components"
WEB_COMPONENTS_EXPORT_DIR="$WEB_COMPONENTS_DIR/WebComponents"

if [[ ! -d "$WEB_COMPONENTS_DIR" ]]; then
    echo "Error: Web components directory not found: $WEB_COMPONENTS_DIR"
    exit 1
fi

echo "Deleting previous build files..."
rm -rf "$COMPOSE_TARGET_DIR"
rm -rf "$WEB_COMPONENTS_EXPORT_DIR"
rm -rf "$DIST_DIR"

echo "Building web components..."
cd "$WEB_COMPONENTS_DIR"
npm run build

if [[ ! -d "$DIST_DIR" ]]; then
    echo "Error: Build did not produce dist directory: $DIST_DIR"
    exit 1
fi

echo "Copying dist to Compose app assets..."
mkdir -p "$COMPOSE_TARGET_DIR"
cp -R "$DIST_DIR"/* "$COMPOSE_TARGET_DIR"

echo "Copying dist to Extensions/yaba-web-components/WebComponents..."
mkdir -p "$WEB_COMPONENTS_EXPORT_DIR"
cp -R "$DIST_DIR"/. "$WEB_COMPONENTS_EXPORT_DIR/"

echo "Done. Web components copied to:"
echo "  - $COMPOSE_TARGET_DIR"
echo "  - $WEB_COMPONENTS_EXPORT_DIR"
