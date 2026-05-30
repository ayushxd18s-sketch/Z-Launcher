#!/bin/bash

echo "==============================="
echo "     Z-Launcher Status"
echo "==============================="

GRADLE="ZalithLauncher/gradle.properties"

# Current Z-Launcher version
CURRENT_VERSION=$(grep "launcher_version_name" "$GRADLE" | cut -d'=' -f2)
CURRENT_CODE=$(grep "launcher_version_code" "$GRADLE" | cut -d'=' -f2)
echo "Z-Launcher version : v$CURRENT_VERSION (code $CURRENT_CODE)"

# Latest GitHub release
LATEST_RELEASE=$(git tag -l | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)
echo "Latest release tag : $LATEST_RELEASE"

# Check if local version is tagged/released
if [ "v$CURRENT_VERSION" = "$LATEST_RELEASE" ]; then
    echo "Release status     : Released"
else
    echo "Release status     : Unreleased changes (v$CURRENT_VERSION not yet tagged)"
fi

echo ""

# Upstream status
echo "Fetching upstream info..."
git fetch upstream --tags --quiet 2>/dev/null
LATEST_UPSTREAM=$(git tag -l | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)
CURRENT_BASE=$(grep "ZALITH_BASE_VERSION" ZalithLauncher/src/main/java/com/movtery/zalithlauncher/path/UrlManager.kt 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')

echo "Upstream latest    : $LATEST_UPSTREAM"
echo "Merged upstream    : $CURRENT_BASE"

if [ -n "$LATEST_UPSTREAM" ] && [ -n "$CURRENT_BASE" ]; then
    if [ "$LATEST_UPSTREAM" = "$CURRENT_BASE" ]; then
        echo "Merge status       : Up to date"
    else
        echo "Merge status       : Merge needed ($CURRENT_BASE → $LATEST_UPSTREAM)"
    fi
fi

echo ""

# Uncommitted changes
CHANGES=$(git status --short)
if [ -z "$CHANGES" ]; then
    echo "Working tree       : Clean"
else
    echo "Working tree       : Uncommitted changes"
    git status --short
fi

echo ""

# Last commit
LAST_COMMIT=$(git log -1 --format="%cr — %s")
echo "Last commit        : $LAST_COMMIT"

echo "==============================="
