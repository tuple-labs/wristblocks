# Install on Wear OS (Pixel Watch, Galaxy Watch and others)

You need the `<name>-wearos.apk` file from your copy's **Releases**, and a computer on the same Wi-Fi as the watch.

## 1. Turn on developer mode on the watch (once)

1. **Settings → System → About → Versions**, tap **Build number** 7 times.
2. **Settings → Developer options**: turn on **ADB debugging** and **Wireless debugging**.

## 2. Pair and install

Easiest with the WristBlocks Docker box (see the main README), or with Android's `adb` tool:

```bash
adb pair WATCH_IP:PAIR_PORT 123456      # from "Wireless debugging → Pair new device" on the watch
adb connect WATCH_IP:PORT               # the address shown on the "Wireless debugging" screen
adb install -r tally-wearos.apk
```

The app appears in the watch's app list.

## Hands-free

On Pixel Watch 3 and newer with Wear OS 7, **double pinch** (tap thumb and index finger together twice) presses the block marked *double tap*. Turn it on in **Settings → Gestures → Hand gestures** if it isn't already.

Done testing? Turn **Wireless debugging** off again to save battery.
