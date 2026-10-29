#!/bin/sh
# Host-side tests. Written by review; NOT executed by the reviewer. Requires JDK 8+ and python3.
# tests/stubs exist only for these host tests and must never be compiled into the APK.
set -eu
cd "$(dirname "$0")/.."
rm -rf build/host-tests
mkdir -p build/host-tests
javac -d build/host-tests $(find tests/stubs -name '*.java') src/local/emby/subtitles/*.java \
  tests/SelectorTest.java tests/PolicyTest.java tests/BridgeTest.java
java -ea -cp build/host-tests SelectorTest
java -ea -cp build/host-tests PolicyTest
java -ea -cp build/host-tests BridgeTest
python3 -I tests/test_patch.py -v
