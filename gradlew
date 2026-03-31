#!/bin/sh
# Gradle wrapper stub - replace with actual gradle wrapper from `gradle wrapper` command
# Download from https://services.gradle.org/distributions/gradle-8.2-bin.zip

APP_BASE_NAME=`basename "$0"`
APP_HOME=`pwd -P`
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec java \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
