#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)"
GRADLE_WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "gradle-wrapper.jar not found. Run 'gradle wrapper' once with a local Gradle install," \
       "or open this project in Android Studio, which will generate it automatically."
  exit 1
fi
exec java -jar "$GRADLE_WRAPPER_JAR" "$@"
