#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -x ../entorno/mysql.command ]; then
    echo 'Ejecutá los archivos SQL de la carpeta sql en orden en MySQL Workbench.'
    exit 1
fi
../entorno/mysql.command < sql/01_esquema.sql
../entorno/mysql.command < sql/02_registrar_turno.sql
../entorno/mysql.command < sql/03_registrar_cobro.sql
echo 'Base preparada. Los registros existentes se conservan.'
