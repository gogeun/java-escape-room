#!/bin/sh
# 팀 시작 화면과 이 프로젝트에 포함된 방을 실행합니다 (JDK 17 이상).
set -eu
cd "$(dirname "$0")/../.."
mkdir -p bin
set -- src/common/MainFrame.java
for room in src/room2/Room2Panel.java src/room3/Room3Panel.java; do
    if [ -f "$room" ]; then set -- "$@" "$room"; fi
done
javac --release 17 -encoding UTF-8 -d bin -sourcepath src "$@"
exec java -cp "resources:bin" common.MainFrame
