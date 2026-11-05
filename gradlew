#!/bin/sh
# Bootstrap-friendly fallback. CI generates the official Gradle wrapper before invoking this script.
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "Gradle is not installed. Install Gradle 8.9 or generate the official wrapper with: gradle wrapper --gradle-version 8.9" >&2
exit 1
