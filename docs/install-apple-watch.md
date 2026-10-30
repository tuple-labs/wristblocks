# Install on Apple Watch

Apple only lets apps onto a watch through a signed install, so there are two routes.

## Route A: with a Mac (most reliable)

You need a Mac with Xcode, an iPhone paired with your Apple Watch, and a free Apple ID.

```bash
git clone https://github.com/<your-copy>/wristblocks
cd wristblocks
brew install xcodegen
WRISTBLOCKS_CLEAN_JSON=runtime/apple/App/app.json python3 tools/validate.py apps/tally.json
cd runtime/apple
APP_NAME=Tally APP_ID=tally xcodegen generate
open WristBlocks.xcodeproj
```

In Xcode:

1. Select the **WristApp** target, then **Signing & Capabilities**. Tick *Automatically manage signing* and pick your **Personal Team** (your Apple ID).
2. If Xcode says the bundle id is taken, change `org.tuplelabs.wristblocks.tally` to something unique, like `com.yourname.tally`.
3. Plug in the iPhone, choose your **Apple Watch** as the run destination and press **Run**.
4. First time only: on the watch, go to **Settings → Privacy & Security → Developer Mode** and turn it on. On the iPhone, trust your developer profile in **Settings → General → VPN & Device Management**.

Free Apple IDs: the app stops opening after 7 days. Press **Run** again to renew it.

**Just want to look?** Choose an *Apple Watch simulator* as the destination instead; no signing needed.

## Route B: no Mac (experimental)

1. Download `<name>-applewatch-unsigned.zip` from your copy's **Releases**.
2. Sign and install it with [xtool](https://github.com/xtool-org/xtool) on Linux or Windows (WSL). Apple Watch support in xtool is still an open pull request ([#350](https://github.com/xtool-org/xtool/pull/350)), so expect rough edges.

If you try this route, please [report how it went](../../../issues/new?template=device-test.yml).
