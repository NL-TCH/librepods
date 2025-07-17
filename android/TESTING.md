# LibrePods Android Testing & F-Droid Setup

This directory contains comprehensive testing infrastructure and F-Droid deployment configuration for the LibrePods Android app.

## Testing Infrastructure

### Overview
The testing setup includes:
- **Unit Tests**: Test core functionality with mock data
- **Instrumented Tests**: UI tests that bypass root setup
- **Screenshot Tests**: Automated screenshot generation for F-Droid
- **Mock Data Providers**: Simulate various AirPods states without hardware

### Root Setup Bypass
The key innovation in this testing setup is bypassing the root requirement for testing:

1. **Mock RadareOffsetFinder**: Tests mock `isHookOffsetAvailable()` to return `true`
2. **Skip Onboarding**: Navigation starts at "settings" instead of "onboarding"
3. **Mock AirPods State**: Use `MockData` class to simulate various device states

### Running Tests

#### Unit Tests
```bash
cd android
./gradlew test
```

#### Instrumented Tests
```bash
cd android
./gradlew connectedAndroidTest
```

#### Screenshot Generation
```bash
cd android
fastlane screenshots
```

### Test Structure

```
app/src/
├── test/java/me/kavishdevar/librepods/
│   ├── MockData.kt                 # Mock data providers
│   ├── MainActivityTest.kt         # Activity unit tests
│   └── RootBypassTest.kt          # Root bypass validation
├── androidTest/java/me/kavishdevar/librepods/
│   ├── LibrePodsUITest.kt         # UI component tests
│   ├── NavigationTest.kt          # Navigation flow tests
│   └── screenshots/
│       └── ScreenshotTest.kt      # Automated screenshot generation
```

### Mock Data

The `MockData` object provides various AirPods states for testing:

- `defaultMockState`: Normal connected state with good battery
- `lowBatteryMockState`: Low battery warning scenario
- `disconnectedMockState`: Disconnected AirPods
- `oneEarbudOutMockState`: One earbud removed

## F-Droid Setup

### Fastlane Configuration

The app includes Fastlane configuration optimized for F-Droid:

#### Available Lanes
- `fastlane test`: Run all tests
- `fastlane debug`: Build debug APK
- `fastlane fdroid_release`: Build F-Droid optimized release APK
- `fastlane screenshots`: Generate automated screenshots
- `fastlane prepare_fdroid`: Complete F-Droid preparation pipeline

#### F-Droid Specific Features
- Unsigned APK generation for F-Droid signing
- Screenshot automation for app store listings
- Metadata generation in F-Droid format
- APK validation and size checking

### Metadata Structure

```
fastlane/metadata/android/en-US/
├── title.txt                  # App title
├── short_description.txt      # Brief description
├── full_description.txt       # Detailed description
├── changelogs/
│   └── 7.txt                 # Version 7 changelog
└── images/                   # Generated screenshots
    ├── phoneScreenshots/
    └── tenInchScreenshots/
```

### CI/CD Integration

GitHub Actions workflow includes:
- Automated testing on push/PR
- F-Droid APK builds on main branch
- Screenshot generation with Android emulator
- Artifact uploads for releases

### Build Variants

The build configuration supports:
- **Debug**: Development builds with debugging enabled
- **Release**: F-Droid optimized builds (unsigned)

### Dependencies

Testing dependencies added:
- JUnit 4 for unit testing
- Espresso for UI testing
- MockK for mocking
- Robolectric for Android unit tests
- Screengrab for automated screenshots
- Compose UI testing framework

## Usage for F-Droid Submission

1. **Run full pipeline**:
   ```bash
   cd android
   fastlane prepare_fdroid
   ```

2. **Review generated files**:
   - APK: `fastlane/outputs/app-release-unsigned.apk`
   - Screenshots: `fastlane/metadata/android/en-US/images/`
   - Metadata: `fastlane/metadata/android/en-US/`

3. **Submit to F-Droid**:
   - Use the generated metadata and APK
   - Screenshots are automatically optimized for F-Droid format

## Development Notes

### Testing Without Root
- Tests use mocked `RadareOffsetFinder` to bypass root checks
- UI tests can access all app screens without actual root access
- Mock data simulates real AirPods behavior patterns

### Screenshot Automation
- Screenshots are generated using real UI components
- Mock data ensures consistent visual state
- Multiple device orientations and screen sizes supported
- Automatic localization support (currently en-US)

### F-Droid Compliance
- No proprietary dependencies
- Reproducible builds
- Proper AGPL v3 licensing
- No tracking or telemetry in F-Droid builds

## Troubleshooting

### Common Issues

1. **Gradle sync fails**: Check Android SDK and JDK versions
2. **Screenshot tests fail**: Ensure emulator has sufficient resources
3. **Mock data not working**: Verify MockK setup in test dependencies

### Debug Commands

```bash
# Check test configuration
./gradlew tasks --all | grep test

# Verbose test output
./gradlew test --info

# Clean build
./gradlew clean build

# Check APK details
aapt dump badging app/build/outputs/apk/release/app-release-unsigned.apk
```