#!/bin/sh
# Commands inside the WristBlocks box. The repo is mounted at /work.
set -e
cmd="${1:-help}"; shift 2>/dev/null || true

case "$cmd" in
  editor)
    echo "WristBlocks editor: open http://localhost:8080 in your browser (Ctrl+C to stop)"
    exec python3 -m http.server 8080 --bind 0.0.0.0 --directory /work/editor
    ;;
  build)
    app="${1:?usage: build apps/<name>.json}"
    clean=/tmp/app.json
    WRISTBLOCKS_CLEAN_JSON="$clean" python3 /work/tools/validate.py "$app" > /tmp/app.env
    . /tmp/app.env
    mkdir -p /work/out
    (cd /work/runtime/wearos && ./gradlew --no-daemon --no-watch-fs -q assembleRelease -PappJson="$clean")
    cp /work/runtime/wearos/app/build/outputs/apk/release/app-release.apk "/work/out/${APP_ID}-wearos.apk"
    echo "Built out/${APP_ID}-wearos.apk ($APP_NAME)"
    echo "Apple Watch version: push the app file to GitHub; it builds on GitHub's Macs."
    ;;
  pair)
    # Watch: Settings > Developer options > Wireless debugging > Pair new device
    adb pair "${1:?usage: pair <watch-ip:pair-port> <6-digit-code>}" "${2:?missing pairing code}"
    ;;
  install)
    apk="${1:?usage: install out/<name>-wearos.apk <watch-ip:port>}"
    adb connect "${2:?missing watch address (ip:port from Wireless debugging)}"
    adb -s "$2" install -r "$apk"
    echo "Installed. Find it in the watch's app list."
    ;;
  test)
    python3 /work/tools/validate.py --selftest
    ;;
  *)
    cat <<'HELP'
WristBlocks box
  editor                         open the blocks editor on http://localhost:8080
  build apps/<name>.json         build a Wear OS app into out/
  pair <ip:port> <code>          pair with a watch (once)
  install out/<file>.apk <ip:port>   install on the watch
  test                           run the app file checks
HELP
    ;;
esac
