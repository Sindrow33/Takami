#!/bin/sh
set -e
cd "$(dirname "$0")"
M=https://repo1.maven.org/maven2
wget -q -O jsoup.jar  $M/org/jsoup/jsoup/1.18.1/jsoup-1.18.1.jar
wget -q -O kxser.jar  $M/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/1.7.3/kotlinx-serialization-core-jvm-1.7.3.jar
wget -q -O kxjson.jar $M/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/1.7.3/kotlinx-serialization-json-jvm-1.7.3.jar
wget -q -O kxcoro.jar $M/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.9.0/kotlinx-coroutines-core-jvm-1.9.0.jar
wget -q -O okhttp.jar $M/com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar
wget -q -O okio.jar   $M/com/squareup/okio/okio-jvm/3.6.0/okio-jvm-3.6.0.jar
ls -l *.jar
