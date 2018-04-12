#!/bin/bash

set -eu

java -jar target/yggdrasil-1.0-SNAPSHOT.jar --fn=generate-makefile > gen.mk
make -f gen.mk "$@"

