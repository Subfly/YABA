#!/usr/bin/env bash
set -euo pipefail

# Publish the YABACore KMP library to mavenLocal so Compose can consume it.

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CORE_DIR="$ROOT/Core"

echo "Publishing YABACore from: $CORE_DIR"
cd "$CORE_DIR"
./gradlew :YABACore:publishToMavenLocal "$@"


