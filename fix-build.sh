#!/bin/bash
echo "Clearing Gradle caches..."
rm -rf ~/.gradle/caches
rm -rf .gradle
echo "Stopping Gradle daemon..."
./gradlew --stop
echo "Building APK..."
./gradlew clean assembleRelease
echo "Build completed successfully"