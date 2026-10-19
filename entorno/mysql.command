#!/bin/zsh
set -eu
SISLUM_RUNTIME="$HOME/Library/Application Support/SisLum"
exec "$SISLUM_RUNTIME/mysql/bin/mysql" --defaults-file="$SISLUM_RUNTIME/client.cnf" --protocol=TCP --database=SisLum "$@"
