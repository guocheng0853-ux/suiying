#!/bin/sh
# 在仓库根目录生成 build-tools/（build_ui.py 需要的工具布局），只从 GitHub 下载。
# 需要：JDK 17+、python3、curl、unzip。Linux x86_64。
set -e
R="$(cd "$(dirname "$0")/.." && pwd)"; T="$R/build-tools"; S="$R/scripts"
mkdir -p "$T/dl" "$T/android-15" "$T/android-35" "$T/jadx" "$T/tmp"
cd "$T/dl"
[ -f jadx-1.5.3.zip ] || curl -fsSLO https://github.com/skylot/jadx/releases/download/v1.5.3/jadx-1.5.3.zip
[ -f apktool_2.12.1.jar ] || curl -fsSLO https://github.com/iBotPeaches/Apktool/releases/download/v2.12.1/apktool_2.12.1.jar
[ -f uber-apk-signer-1.3.0.jar ] || curl -fsSLO https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar
[ -f "$T/android-35/android.jar" ] || curl -fsSL -o "$T/android-35/android.jar" https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-35/android.jar
[ -f "$T/jadx/lib/jadx-1.5.3-all.jar" ] || unzip -q -o jadx-1.5.3.zip -d "$T/jadx"
# ecj 兼容层（Maven/Eclipse 站点不可用时用 JDK javac 代替）
mkdir -p "$T/tmp/shim" "$T/tmp/stubs"
javac -d "$T/tmp/shim" "$S/EcjShim.java"
printf 'Main-Class: EcjShim\n' > "$T/tmp/m.txt"; jar cfm "$T/ecj.jar" "$T/tmp/m.txt" -C "$T/tmp/shim" .
javac -source 8 -target 8 -Xlint:-options -bootclasspath "$T/android-35/android.jar" -d "$T/tmp/stubs" "$S/LambdaMetafactory.java"
jar cf "$T/android-15/core-lambda-stubs.jar" -C "$T/tmp/stubs" .
# aapt2（apktool 内置）、zipalign（Python 版，支持 -P 16）、d8（jadx 内置 R8）、apksigner（uber-apk-signer 内置）
unzip -p apktool_2.12.1.jar prebuilt/linux/aapt2_64 > "$T/android-15/aapt2"; chmod +x "$T/android-15/aapt2"
cp "$S/zipalign.py" "$T/android-15/zipalign.py"
printf '#!/bin/sh\nexec python3 "$(dirname "$0")/zipalign.py" "$@"\n' > "$T/android-15/zipalign"
printf '#!/bin/sh\nT="$(cd "$(dirname "$0")/.." && pwd)"\nexec java -Xmx3g -cp "$T/jadx/lib/jadx-1.5.3-all.jar" com.android.tools.r8.D8 "$@"\n' > "$T/android-15/d8"
printf '#!/bin/sh\nT="$(cd "$(dirname "$0")/.." && pwd)"\nexec java -cp "$T/dl/uber-apk-signer-1.3.0.jar" com.android.apksigner.ApkSignerTool "$@"\n' > "$T/android-15/apksigner"
chmod +x "$T/android-15/"*
rm -rf "$T/tmp"; echo "build-tools ready: $T"
