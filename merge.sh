#!/bin/bash

# Usage: ./merge.sh
# Fetches latest upstream ZalithLauncher2 tag and starts the merge

echo "Fetching upstream tags..."
git fetch upstream --tags

# Get latest upstream version tag (numeric like 2.4.4)
LATEST=$(git tag -l | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)

if [ -z "$LATEST" ]; then
    echo "No upstream tags found. Make sure upstream remote is set."
    exit 1
fi

echo "Latest upstream version: $LATEST"
echo ""

# Check if already merged
CURRENT=$(grep "ZALITH_BASE_VERSION" ZalithLauncher/src/main/java/com/movtery/zalithlauncher/path/UrlManager.kt 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
if [ "$CURRENT" = "$LATEST" ]; then
    echo "Already on upstream $LATEST — nothing to merge."
    exit 0
fi

echo "Current upstream base: $CURRENT"
echo "Merging upstream $LATEST into main..."
echo ""

# Create backup branch
BACKUP="v$(grep 'launcher_version_name' ZalithLauncher/gradle.properties | cut -d'=' -f2)-before-merge-$LATEST"
git checkout -b "$BACKUP"
git push origin "$BACKUP"
echo "Backup branch created: $BACKUP"
echo ""

# Switch back and merge
git checkout main
git merge "$LATEST"

# Check for conflicts
CONFLICTS=$(git diff --name-only --diff-filter=U)

if [ -z "$CONFLICTS" ]; then
    echo ""
    echo "Merge completed with no conflicts!"
    echo "Run ./release.sh <version> to release."
else
    echo ""
    echo "Merge has conflicts in these files:"
    echo "$CONFLICTS"
    echo ""
    echo "Resolve the conflicts, then run:"
    echo "  git add <resolved files>"
    echo "  git commit -m \"Merge ZalithLauncher2 $LATEST into Z-Launcher\""
    echo "  git push origin main"
    echo "  ./release.sh <new-version>"
fi
