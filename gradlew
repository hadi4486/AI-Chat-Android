#!/bin/sh
APP_HOME=$( cd "${0%/[\\]*}" > /dev/null; pwd -P )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVACMD="java"
exec "$JAVACMD" -Xmx512m -Xms256m \
  "-Dorg.gradle.appname=gradlew" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
