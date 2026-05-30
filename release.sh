#!/bin/bash

# Usage: ./release.sh 1.5.0
# Bumps version, commits, and pushes tag to trigger automatic build

if [ -z "$1" ]; then
    echo "Usage: ./release.sh <version>"
    echo "Example: ./release.sh 1.5.0"
    exit 1
fi

VERSION="$1"
GRADLE="ZalithLauncher/gradle.properties"

# Get current version code and increment by 1
CURRENT_CODE=$(grep "launcher_version_code" "$GRADLE" | cut -d'=' -f2)
NEW_CODE=$((CURRENT_CODE + 1))

echo "Releasing v$VERSION (code $NEW_CODE)..."

# Update gradle.properties
sed -i "s/launcher_version_code=$CURRENT_CODE/launcher_version_code=$NEW_CODE/" "$GRADLE"
sed -i "s/launcher_version_name=.*/launcher_version_name=$VERSION/" "$GRADLE"

# Verify changes
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
