#!/usr/bin/env bash
# Build an AppImage from the app-image made by build.sh (same arch), or cross-assemble x86_64.
# Usage: appimage.sh <arch: aarch64|x86_64> [name] [version]
# Env: APPIMAGETOOL_DIR (dir with appimagetool-<arch>.AppImage),
#      for cross x86_64: X64_JRE (extracted Temurin x64 JRE dir) and X64_SKIKO (skiko-awt-runtime-linux-x64 jar).
set -euo pipefail
cd "$(dirname "$0")/../.."
ARCH=${1:?arch}; NAME=${2:-athena-spike}; VER=${3:-0.1.0}
OUT=build/dist; A="$OUT/AppDir-$ARCH"; rm -rf "$A"; mkdir -p "$A/usr/lib/app" "$A/usr/share/applications"
HOSTARCH=$(uname -m)
if [ "$ARCH" = "$HOSTARCH" ]; then
  cp -r "$OUT/img/$NAME/." "$A/usr/"                         # jpackage image: bin/, lib/ (native launcher + runtime)
  cat > "$A/AppRun" <<'R'
#!/bin/sh
H=$(dirname "$(readlink -f "$0")")
exec "$H/usr/bin/NAME" "$@"
R
  sed -i "s/NAME/$NAME/" "$A/AppRun"
else
  # cross-arch: jpackage can't cross, so ship a plain JRE + jars + a launcher script.
  cp "$OUT"/in/*.jar "$A/usr/lib/app/"
  rm -f "$A"/usr/lib/app/skiko-awt-runtime-linux-*.jar; cp "$X64_SKIKO" "$A/usr/lib/app/"
  cp -r "$X64_JRE"/*/. "$A/usr/jre"
  MAIN=$(cat "$OUT/mainclass")
  cat > "$A/AppRun" <<R
#!/bin/sh
H=\$(dirname "\$(readlink -f "\$0")")
exec "\$H/usr/jre/bin/java" --enable-native-access=ALL-UNNAMED -Xss2m -cp "\$H/usr/lib/app/*" $MAIN "\$@"
R
fi
chmod +x "$A/AppRun"
cat > "$A/$NAME.desktop" <<D
[Desktop Entry]
Type=Application
Name=Athena Reader (spike)
Exec=$NAME %f
Icon=$NAME
Categories=Office;Viewer;
MimeType=application/pdf;application/epub+zip;
D
# placeholder icon (1x1 PNG) until real artwork exists
base64 -d > "$A/$NAME.png" <<'P'
iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==
P
ARCH=$ARCH "${APPIMAGETOOL_DIR:?}/appimagetool-$HOSTARCH.AppImage" --appimage-extract-and-run --comp zstd ${APPIMAGE_RUNTIME_DIR:+--runtime-file "$APPIMAGE_RUNTIME_DIR/runtime-$ARCH"} "$A" "$OUT/${NAME}-${VER}-${ARCH}.AppImage"
ls -la "$OUT"/*.AppImage
