#!/bin/sh
set -e
export PATH=/opt/kotlinc/bin:$PATH
export JAVA_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=384m"
CP="libs/jsoup.jar:libs/kxser.jar:libs/kxcoro.jar:libs/kxjson.jar"
STD=/opt/kotlinc/lib/kotlin-stdlib.jar
PLUGIN=/opt/kotlinc/lib/kotlinx-serialization-compiler-plugin.jar
kotlinc -cp "$CP" -Xplugin="$PLUGIN" -d out \
  $(find src/main/kotlin -name '*.kt' ! -path '*core/net/*')
java -cp "out:$CP:$STD" app.MainKt
