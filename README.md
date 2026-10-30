# WristBlocks

**Build smartwatch apps once, run them on any watch. No Mac, no Xcode, no Android Studio required.**

> Status: early proof of concept. Right now this repo proves the hardest link: a real Apple Watch app built entirely on GitHub's macOS machines.

## The idea

Every watch platform has its own language, SDK and build tools: Swift and Xcode for Apple Watch, Kotlin and Gradle for Wear OS, Monkey C for Garmin. That keeps watch apps out of reach for most people.

WristBlocks aims to fix that:

1. **Blocks editor**: drag blocks like *Counter*, *Timer*, *Vibrate*, *Heart rate*, *Steps* or *Send to a web link* to describe an app.
2. **One Docker image**: open the editor on `localhost`, press Build.
3. **Translators per watch**: the same app becomes a Wear OS app, an Apple Watch app and later Garmin and others.
4. **Apple Watch builds in the cloud**: GitHub Actions runs the Apple toolchain on real Macs (free for public repos), so it works from Windows, Linux or macOS and stays within Apple's license terms.

## What works today

- `apple/`: a minimal Apple Watch app (a counter, with the watch's double tap mapped to +1 on watchOS 11+).
- `.github/workflows/watchos.yml`: builds it for the watch and the simulator on `macos-latest` and publishes the unsigned app as a build artifact.

Builds are **unsigned on purpose**: signing and installing will happen on your own machine, so no Apple ID or certificates ever touch CI.

## Roadmap

- [x] Apple Watch build on GitHub Actions
- [ ] Wear OS translator (Kotlin + Compose for Wear OS) inside the Docker image
- [ ] Blocks editor (browser based) and app description format
- [ ] Local signing and install helper for Apple Watch (free Apple ID; installs through the paired iPhone)
- [ ] Garmin Connect IQ translator

## Contributing

Ideas, issues and pull requests are welcome. Please never include personal data, API keys, Apple IDs or signing files in issues, logs or commits.

## License

MIT
