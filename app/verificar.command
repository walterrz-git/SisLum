#!/bin/sh
set -eu
cd "$(dirname "$0")"
if command -v mvn >/dev/null 2>&1; then
    SISLUM_MAVEN=$(command -v mvn)
else
    SISLUM_MAVEN='/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn'
fi
for SISLUM_PRUEBA in VerificarRegistroTurno VerificarRegistroAtencion VerificarRegistroCobro; do
    "$SISLUM_MAVEN" -q test-compile exec:exec -Dexec.executable=java \
        "-Dexec.args=-classpath %classpath ar.com.sislum.$SISLUM_PRUEBA" -Dexec.classpathScope=test
done
