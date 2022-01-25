#!/usr/bin/env bash

DSL_DIR=$1
SCRIPT=$2
JMETER_RESOURCES_DIR=$3

CP=$(echo "${DSL_DIR}/lib"/*.jar | tr ' ' ':')

kotlinc -cp "${DSL_DIR}:$CP" -script "$SCRIPT" "$JMETER_RESOURCES_DIR"