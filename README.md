# PianoMan

A scrollable, full 88-key (A0–C8) piano keyboard for Android. Each key is
rendered by a custom `View` inside a `HorizontalScrollView` and plays a note
when pressed, supporting multitouch chords.

Audio isn't synthesized — `PianoSynth` pitch-shifts (via resampling) the
nearest recorded `.wav` sample to whatever key you press, so every note uses
real recorded audio. It currently plays 1st-violin pizzicato samples from
`app/src/main/assets/Samples/1st Violins`; a full orchestral sample library
and the original piano samples (`app/src/main/res/raw`) are also bundled for
future instruments.

## Requirements

- JDK 17
- Android SDK (`compileSdk` / `targetSdk` 36, `minSdk` 21)
- No other setup — the Gradle wrapper downloads Gradle 8.14.3 itself

## Building

From the project root:

```bash
# Debug build
./gradlew assembleDebug

# Install straight to a connected device/emulator
./gradlew installDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

If `./gradlew` fails with a `javac`/toolchain error, your `JAVA_HOME` is
pointed at a JRE instead of a full JDK. Point it at a JDK 17 install, e.g.:

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
```

### Release builds

Release builds are signed using credentials from a `release.properties` file
at the project root (gitignored — it isn't checked in since it holds real
signing secrets). Create one with:

```properties
storeFile=release.keystore
storePassword=<your keystore password>
keyAlias=<your key alias>
keyPassword=<your key password>
```

along with the matching `release.keystore` file, also at the project root.
Without this file, `./gradlew assembleRelease` will build an unsigned APK.

## Running

Build via AndroidIDE, or from the CLI:

```bash
./gradlew installDebug
adb shell am start -n com.example.pianoman/.MainActivity
```

The app locks to landscape orientation; scroll horizontally to reach keys
outside the current view.
