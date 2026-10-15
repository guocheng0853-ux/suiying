#!/bin/sh
# 先运行 build_ui.py（生成 build/classes），再运行全部单元测试。
set -e
cd "$(dirname "$0")/../cinema-ui"
CP="test-libs/json-20240303.jar:build/classes:build/stubs:$(ls runtime-libs/*.jar|tr '\n' ':')build/runtime-classes:../build-tools/android-35/android.jar"
rm -rf build/tests; mkdir -p build/tests
javac -nowarn -encoding UTF-8 -d build/tests -cp "$CP" tests/*.java tests/fixtures/com/guocheng/mediapocket/*.java
fail=0
for t in tests/*Test.java; do n=$(basename "$t" .java)
  if out=$(timeout 180 java -cp "build/tests:$CP" com.guocheng.mediapocket.$n 2>&1); then echo "$out" | grep -v JAVA_TOOL | tail -1; else echo "FAIL $n"; echo "$out" | tail -5; fail=1; fi
done
exit $fail
