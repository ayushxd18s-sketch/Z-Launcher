#!/bin/bash

# Usage: ./merge.sh
# Fetches latest upstream ZalithLauncher2 tag and merges it

echo "Fetching upstream tags..."
git fetch upstream --tags

# Get latest upstream version tag
LATEST=$(git tag -l | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)

if [ -z "$LATEST" ]; then
    echo "No upstream tags found. Make sure upstream remote is set:"
    echo "  git remote add upstream https://github.com/ZalithLauncher/ZalithLauncher2.git"
    exit 1
fi

echo "Latest upstream version: $LATEST"

# Check if already merged
CURRENT_BASE=$(grep "ZALITH_BASE_VERSION" ZalithLauncher/src/main/java/com/movtery/zalithlauncher/path/UrlManager.kt 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
if [ "$CURRENT_BASE" = "$LATEST" ]; then
    echo "Already on upstream $LATEST — nothing to merge."
    exit 0
fi

echo "Current upstream base: $CURRENT_BASE"
echo "Merging upstream $LATEST into main..."
echo ""

# Create backup branch
Z_VERSION=$(grep 'launcher_version_name' ZalithLauncher/gradle.properties | cut -d'=' -f2)
BACKUP="v${Z_VERSION}-before-merge-${LATEST}"
git checkout -b "$BACKUP"
git push origin "$BACKUP"
echo "Backup branch created: $BACKUP"
echo ""

git checkout main
git merge "$LATEST"

# Check for conflicts
CONFLICTS=$(git diff --name-only --diff-filter=U)

if [ -z "$CONFLICTS" ]; then
    echo ""
    echo "Merge completed with no conflicts!"
    echo "Remember to bump the version, then run: ./release.sh <version>"
    exit 0
fi

echo ""
echo "========================================="
echo "  CONFLICTS DETECTED — Resolution Guide"
echo "========================================="
echo ""

for FILE in $CONFLICTS; do
    echo "--- $FILE ---"

    case "$FILE" in

        ZalithLauncher/gradle.properties)
            echo "KEEP: launcher_version_code and launcher_version_name → YOUR values (Z-Launcher version)"
            echo "KEEP: applicationId signing passwords → YOUR values"
            echo "TAKE UPSTREAM: everything else (dependencies, SDK versions, etc.)"
            ;;

        ZalithLauncher/build.gradle.kts)
            echo "KEEP: applicationId = \"com.ayushxd.zlauncher\" → YOUR value (unique package ID)"
            echo "TAKE UPSTREAM: everything else"
            ;;

        */ZLApplication.kt)
            echo "KEEP: ely.by auth server block (lines setting default auth server)"
            echo "Your custom block looks like:"
            grep -n "ely.by\|elyby\|ELJUR\|authlib" "ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ZLApplication.kt" 2>/dev/null | head -10
            echo "TAKE UPSTREAM: everything else"
            ;;

        */AccountsManager.kt)
            echo "KEEP: offline account patch (allows offline accounts without Microsoft login)"
            echo "Your custom line looks like:"
            grep -n "offline\|isOnline\|skipOnline\|forceOffline" "ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/account/AccountsManager.kt" 2>/dev/null | head -5
            echo "TAKE UPSTREAM: everything else"
            ;;

        */MainActivity.kt)
            echo "Usually safe to TAKE UPSTREAM for all conflicts here."
            ;;

        *)
            echo "No specific hint — check git diff for this file and keep Z-Launcher customizations."
            ;;
    esac

    echo ""
done

echo "========================================="
echo "After resolving all conflicts, run:"
echo "  git add <resolved files>"
echo "  git commit -m \"Merge ZalithLauncher2 $LATEST into Z-Launcher\""
echo "  git push origin main"
echo "  ./release.sh <new-version>"
echo "========================================="
