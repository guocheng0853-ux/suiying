#!/bin/sh
# 先运行 build_ui.py（生成 build/classes），再运行全部单元测试。
# SpiderComponentRules 走 JNI：本机用 rust/rules-rs 编出的 librules_rs.so。
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/cinema-ui"
HOST_SO="$ROOT/rust/rules-rs/target/release/librules_rs.so"
if [ ! -f "$HOST_SO" ]; then
  echo "missing $HOST_SO — run: rust-anything build $ROOT/rust/rules-rs" >&2
  exit 1
fi
CP="test-libs/json-20240303.jar:build/classes:build/stubs:$(ls runtime-libs/*.jar|tr '\n' ':')build/runtime-classes:../build-tools/android-35/android.jar"
rm -rf build/tests; mkdir -p build/tests
javac -nowarn -encoding UTF-8 -d build/tests -cp "$CP" tests/*.java tests/fixtures/com/guocheng/mediapocket/*.java
fail=0
for t in tests/*Test.java; do n=$(basename "$t" .java)
  if out=$(timeout 180 java -Drust-anything.lib.rules_rs="$HOST_SO" -cp "build/tests:$CP" com.guocheng.mediapocket.$n 2>&1); then echo "$out" | grep -v JAVA_TOOL | tail -1; else echo "FAIL $n"; echo "$out" | tail -5; fail=1; fi
done
if [ "$fail" -eq 0 ]; then count=$(find tests -maxdepth 1 -name '*Test.java' | wc -l | tr -d ' '); mkdir -p build; printf '%s\n' "$count" > build/jvm-tests.passed; fi
exit $fail
