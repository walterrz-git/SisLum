#!/bin/sh
set -eu
cd "$(dirname "$0")"
if command -v mvn >/dev/null 2>&1; then
    SISLUM_MAVEN=$(command -v mvn)
elif [ -x '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' ]; then
    SISLUM_MAVEN='/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn'
else
    echo 'Abrí el proyecto en NetBeans y elegí Run Project, o instalá Maven.'
    exit 1
fi
exec "$SISLUM_MAVEN" -q compile exec:java
