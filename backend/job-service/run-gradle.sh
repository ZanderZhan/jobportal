#!/bin/bash
cd "$(dirname "$0")"
java -jar gradle/wrapper/gradle-wrapper.jar "$@"
