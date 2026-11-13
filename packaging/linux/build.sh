#!/usr/bin/env bash
# Local Linux packaging: executable jar -> jlink runtime -> jpackage app-image -> .deb (+ tar.gz).
# Usage: JAVA_HOME=<jdk25> packaging/linux/build.sh [module=spike] [name=athena-spike] [version=0.1.0]
set -euo pipefail
cd "$(dirname "$0")/../.."
MOD=${1:-spike}; NAME=${2:-athena-spike}; VER=${3:-0.1.0}
ARCH=$(dpkg --print-architecture)
OUT=build/dist; rm -rf "$OUT"; mkdir -p "$OUT/in"
./kotlin package -m "$MOD" -f executable-jar
# Amper's executable-jar flattens libs by filename; same-named jars from different groups
# (e.g. androidx vs org.jetbrains runtime-desktop, the latter a stub) collide. Rebuild the lib set,
# preferring the largest jar per name.
unzip -q "build/tasks/_${MOD}_executableJarJvm/${MOD}-jvm-executable.jar" 'BOOT-INF/lib/*' -d "$OUT/fat"
cp "$OUT"/fat/BOOT-INF/lib/*.jar "$OUT/in/"
M=${KOTLIN_M2:-$HOME/.cache/JetBrains/Kotlin/.m2.cache}
for f in "$OUT"/in/*.jar; do
  b=$(basename "$f"); best=$f
  while IFS= read -r c; do [ "$(stat -c%s "$c")" -gt "$(stat -c%s "$best")" ] && best=$c; done < <(find "$M" -name "$b")
  [ "$best" != "$f" ] && cp "$best" "$f"
done
cp "build/tasks/_${MOD}_jarJvm/${MOD}-jvm.jar" "$OUT/in/app.jar"
MAIN=$(unzip -p "build/tasks/_${MOD}_executableJarJvm/${MOD}-jvm-executable.jar" META-INF/MANIFEST.MF | tr -d '\r' | sed -n 's/^Start-Class: //p')
echo "$MAIN" > "$OUT/mainclass"
MODS=$("$JAVA_HOME/bin/jdeps" --multi-release 25 --ignore-missing-deps --print-module-deps --class-path "$OUT/in/*" "$OUT/in/app.jar"),jdk.unsupported
"$JAVA_HOME/bin/jlink" --add-modules "$MODS" --strip-debug --no-header-files --no-man-pages --compress zip-6 --output "$OUT/rt"
"$JAVA_HOME/bin/jpackage" --type app-image --name "$NAME" --app-version "$VER" --input "$OUT/in" \
  --main-jar app.jar --main-class "$MAIN" --runtime-image "$OUT/rt" --dest "$OUT/img" \
  --java-options --enable-native-access=ALL-UNNAMED --java-options -Xss2m
# .deb by hand (jpackage --type deb needs fakeroot/rpm tooling)
D="$OUT/deb"; mkdir -p "$D/opt" "$D/usr/bin" "$D/DEBIAN" "$D/usr/share/applications"
cp -r "$OUT/img/$NAME" "$D/opt/$NAME"; ln -s "/opt/$NAME/bin/$NAME" "$D/usr/bin/$NAME"
cat > "$D/usr/share/applications/$NAME.desktop" <<D2
[Desktop Entry]
Type=Application
Name=Athena Reader (spike)
Exec=/opt/$NAME/bin/$NAME %f
Categories=Office;Viewer;
MimeType=application/pdf;application/epub+zip;
D2
cat > "$D/DEBIAN/control" <<C
Package: $NAME
Version: $VER
Architecture: $ARCH
Maintainer: Athena <noreply@example.com>
Depends: libc6, libx11-6, libxext6, libxrender1, libxtst6, libxi6, libfreetype6, libfontconfig1
Description: Athena book reader
C
dpkg-deb --root-owner-group --build "$D" "$OUT/${NAME}_${VER}_${ARCH}.deb"
tar -C "$OUT/img" -czf "$OUT/${NAME}-${VER}-linux-${ARCH}.tar.gz" "$NAME"
ls -la "$OUT"/*.deb "$OUT"/*.tar.gz; du -sh "$OUT/img/$NAME"
