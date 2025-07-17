#!/bin/bash

# Validation script for LibrePods testing infrastructure
# This script validates the testing setup without requiring a full Android build

echo "🧪 LibrePods Testing Infrastructure Validation"
echo "=============================================="

# Check if required directories exist
echo "📁 Checking directory structure..."
required_dirs=(
    "app/src/test/java/me/kavishdevar/librepods"
    "app/src/androidTest/java/me/kavishdevar/librepods"
    "app/src/androidTest/java/me/kavishdevar/librepods/screenshots"
    "fastlane"
    "fastlane/metadata/android/en-US"
)

for dir in "${required_dirs[@]}"; do
    if [ -d "$dir" ]; then
        echo "  ✅ $dir"
    else
        echo "  ❌ $dir"
    fi
done

# Check if required files exist
echo ""
echo "📄 Checking required files..."
required_files=(
    "app/src/test/java/me/kavishdevar/librepods/MockData.kt"
    "app/src/test/java/me/kavishdevar/librepods/MainActivityTest.kt"
    "app/src/test/java/me/kavishdevar/librepods/RootBypassTest.kt"
    "app/src/androidTest/java/me/kavishdevar/librepods/LibrePodsUITest.kt"
    "app/src/androidTest/java/me/kavishdevar/librepods/NavigationTest.kt"
    "app/src/androidTest/java/me/kavishdevar/librepods/screenshots/ScreenshotTest.kt"
    "fastlane/Fastfile"
    "fastlane/Appfile"
    "gradle/libs.versions.toml"
    "Gemfile"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✅ $file"
    else
        echo "  ❌ $file"
    fi
done

# Check for test dependencies in libs.versions.toml
echo ""
echo "🔧 Checking test dependencies..."
deps_to_check=("junit" "mockk" "espresso" "robolectric" "screengrab")

for dep in "${deps_to_check[@]}"; do
    if grep -q "$dep" gradle/libs.versions.toml; then
        echo "  ✅ $dep dependency configured"
    else
        echo "  ❌ $dep dependency missing"
    fi
done

# Check for test source sets in build.gradle.kts
echo ""
echo "🏗️ Checking build configuration..."
if grep -q "testInstrumentationRunner" app/build.gradle.kts; then
    echo "  ✅ Test instrumentation runner configured"
else
    echo "  ❌ Test instrumentation runner missing"
fi

if grep -q "testImplementation" app/build.gradle.kts; then
    echo "  ✅ Unit test dependencies configured"
else
    echo "  ❌ Unit test dependencies missing"
fi

if grep -q "androidTestImplementation" app/build.gradle.kts; then
    echo "  ✅ Instrumented test dependencies configured"
else
    echo "  ❌ Instrumented test dependencies missing"
fi

# Check Fastlane configuration
echo ""
echo "🚀 Checking Fastlane configuration..."
if grep -q "fdroid_release" fastlane/Fastfile; then
    echo "  ✅ F-Droid release lane configured"
else
    echo "  ❌ F-Droid release lane missing"
fi

if grep -q "screenshots" fastlane/Fastfile; then
    echo "  ✅ Screenshot lane configured"
else
    echo "  ❌ Screenshot lane missing"
fi

if [ -f "fastlane/metadata/android/en-US/title.txt" ]; then
    echo "  ✅ F-Droid metadata present"
else
    echo "  ❌ F-Droid metadata missing"
fi

# Check CI/CD setup
echo ""
echo "⚙️ Checking CI/CD configuration..."
if [ -f "../.github/workflows/android.yml" ]; then
    echo "  ✅ GitHub Actions workflow configured"
else
    echo "  ❌ GitHub Actions workflow missing"
fi

# Validate mock data structure (simple syntax check)
echo ""
echo "🎭 Validating mock data structure..."
if grep -q "MockBatteryLevels" app/src/test/java/me/kavishdevar/librepods/MockData.kt; then
    echo "  ✅ MockBatteryLevels data class present"
else
    echo "  ❌ MockBatteryLevels data class missing"
fi

if grep -q "defaultMockState" app/src/test/java/me/kavishdevar/librepods/MockData.kt; then
    echo "  ✅ Default mock state configured"
else
    echo "  ❌ Default mock state missing"
fi

if grep -q "lowBatteryMockState" app/src/test/java/me/kavishdevar/librepods/MockData.kt; then
    echo "  ✅ Low battery mock state configured"
else
    echo "  ❌ Low battery mock state missing"
fi

# Check for root bypass strategy
echo ""
echo "🔓 Checking root bypass strategy..."
if grep -q "isHookOffsetAvailable" app/src/test/java/me/kavishdevar/librepods/RootBypassTest.kt; then
    echo "  ✅ Root bypass test implemented"
else
    echo "  ❌ Root bypass test missing"
fi

if grep -q "mockk" app/src/test/java/me/kavishdevar/librepods/RootBypassTest.kt; then
    echo "  ✅ Mocking framework used for root bypass"
else
    echo "  ❌ Mocking framework not used"
fi

echo ""
echo "📊 Validation Summary"
echo "==================="

# Count files
total_test_files=$(find app/src/test -name "*.kt" 2>/dev/null | wc -l)
total_androidtest_files=$(find app/src/androidTest -name "*.kt" 2>/dev/null | wc -l)

echo "  📱 Unit test files: $total_test_files"
echo "  🤖 Instrumented test files: $total_androidtest_files"
echo "  🚀 Fastlane lanes: $(grep -c "desc.*lane" fastlane/Fastfile 2>/dev/null || echo "0")"
echo "  📄 F-Droid metadata files: $(find fastlane/metadata -name "*.txt" 2>/dev/null | wc -l)"

echo ""
echo "🎯 Next Steps:"
echo "  1. Run './gradlew test' to execute unit tests"
echo "  2. Run './gradlew connectedAndroidTest' for UI tests"
echo "  3. Run 'fastlane screenshots' to generate F-Droid screenshots"
echo "  4. Run 'fastlane prepare_fdroid' for complete F-Droid pipeline"
echo ""
echo "📚 For more details, see TESTING.md and TESTING_SUMMARY.md"