#!/bin/bash
echo "starting generating ssl cert for backend"

call mkcert -install

mkcert -pkcs12 -out src/main/resources/local-dev.p12 localhost 127.0.0.1 ::1

echo "done, cert created in src/main/resources/local-dev.p12"
pause