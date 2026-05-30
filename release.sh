#!/bin/bash

# Usage: ./release.sh 1.5.0

if [ -z "$1" ]; then
    echo "Usage: ./release.sh <version>"
    echo "Example: ./release.sh 1.5.0"
    exit 1
fi

VERSION="$1"
GRADLE="ZalithLauncher/gradle.properties"

# Check for uncommitted changes
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Error: You have uncommitted changes. Commit or stash them first."
    git status --short
    exit 1
fi

# Check if local main is behind origin
git fetch origin --quiet
BEHIND=$(git rev-list HEAD..origin/main --count 2>/dev/null)
if [ "$BEHIND" -gt 0 ]; then
    echo "Error: Your local main is $BEHIND commit(s) behind origin. Run 'git pull' first."
    exit 1
fi

# Check if upstream has a newer version not yet merged
git fetch upstream --tags --quiet 2>/dev/null
LATEST_UPSTREAM=$(git tag -l | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)
CURRENT_BASE=$(grep "ZALITH_BASE_VERSION" ZalithLauncher/src/main/java/com/movtery/zalithlauncher/path/UrlManager.kt 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')

if [ -n "$LATEST_UPSTREAM" ] && [ -n "$CURRENT_BASE" ] && [ "$LATEST_UPSTREAM" != "$CURRENT_BASE" ]; then
    echo "Warning: Upstream ZalithLauncher2 has a newer version ($LATEST_UPSTREAM) that hasn't been merged yet."
    echo "Run ./merge.sh first, or type 'skip' to release anyway."
    read -r ANSWER
    if [ "$ANSWER" != "skip" ]; then
        echo "Cancelled. Run ./merge.sh to merge upstream first."
        exit 1
    fi
fi

# Get current version code and increment by 1
CURRENT_CODE=$(grep "launcher_version_code" "$GRADLE" | cut -d'=' -f2)
NEW_CODE=$((CURRENT_CODE + 1))

echo "Releasing v$VERSION (code $NEW_CODE)..."

# Update gradle.properties
sed -i "s/launcher_version_code=$CURRENT_CODE/launcher_version_code=$NEW_CODE/" "$GRADLE"
sed -i "s/launcher_version_name=.*/launcher_version_name=$VERSION/" "$GRADLE"

echo ""
echo "Version set to:"
grep "launcher_version" "$GRADLE"
echo ""

# Commit and push
git add "$GRADLE"
git commit -m "Bump version to v$VERSION"
git push origin main

# Tag and push
git tag "v$VERSION"
git push origin "v$VERSION"

echo ""
echo "Done! GitHub Actions is now building v$VERSION."
echo "Check progress at: https://github.com/ayushxd18s-sketch/Z-Launcher/actions"
