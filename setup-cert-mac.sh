#!/bin/bash

echo "Starting generating SSL cert for backend..."

mkcert -install

mkcert -pkcs12 \
  -p12-file src/main/resources/local-dev.p12 \
  localhost 127.0.0.1 ::1

echo "Done, cert created in src/main/resources/local-dev.p12"