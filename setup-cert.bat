@echo off
echo starting generating ssl cert for backend

call mkcert -install

call mkcert -pkcs12 -p12-file src\main\resources\local-dev.p12 localhost 127.0.0.1 ::1

echo done, cert created in w src/main/resources/local-dev.p12
pause

