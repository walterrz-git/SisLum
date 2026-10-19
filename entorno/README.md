# Entorno SisLum en macOS Intel

MySQL 8.4.11 instalado de forma nativa para este usuario, sin máquina virtual.
Base de la aplicación: `SisLum`. El proyecto activo prepara las tablas de turno, atención
y cobro con sus datos de soporte. La base puede conservar tablas anteriores de seguridad;
la aplicación actual no las utiliza.
Java JDK 23.0.1 y Apache NetBeans 29 ya estaban instalados.

## Conectar desde MySQL Workbench

Abrir MySQLWorkbench desde Aplicaciones. Crear una conexión con el botón `+`:

- Connection Name: `SisLum local`
- Connection Method: `Standard TCP/IP`
- Hostname: `127.0.0.1`
- Port: `3306`
- Username: `sislum`
- Default Schema: `SisLum`

La contraseña está en `~/Library/Application Support/SisLum/Acceso-Workbench.txt`.
Introducirla en Workbench y, si se desea, guardarla en el llavero con `Store in Keychain`.
No compartir ese archivo ni incluirlo en Git o las capturas del TP.
Pulsar `Test Connection` y abrir la conexión.

## Terminal

Abrir `mysql.command` con doble clic, o ejecutar desde la carpeta del proyecto:

```sh
./entorno/mysql.command
./entorno/mysql.command < entorno/verificar.sql
```

## Servicio y datos

MySQL inicia al ingresar con este usuario a macOS y solo escucha en `127.0.0.1:3306`.
Los datos y binarios están fuera del repositorio, en `~/Library/Application Support/SisLum/`.
No borrar esa carpeta: contiene la base de datos y las credenciales.

Configuración: `my.cnf`. Registro: `mysql.log`. Credenciales administrativas: `admin.cnf`.
El usuario `sislum` tiene permisos sobre la base `SisLum`; `root` se reserva para administración.

Para detenerlo:

```sh
launchctl bootout gui/$(id -u) "$HOME/Library/LaunchAgents/local.sislum.mysql.plist"
```

Para iniciarlo después de detenerlo:

```sh
launchctl bootstrap gui/$(id -u) "$HOME/Library/LaunchAgents/local.sislum.mysql.plist"
```

## Java

La aplicación Java utiliza Swing y MySQL Connector/J para conectarse a
`jdbc:mysql://127.0.0.1:3306/SisLum`. Las dependencias se declaran en el proyecto Maven `app`.
Las contraseñas permanecen fuera del código fuente.

Los scripts para casos futuros se guardan en `futuros-casos-de-uso` y no se ejecutan
con la preparación actual.

## Descargas oficiales

- https://dev.mysql.com/downloads/mysql/8.4.html
- https://dev.mysql.com/downloads/workbench/8.0.html

Se utilizaron paquetes Intel x86_64 y se compararon las sumas MD5 con las publicadas.
Workbench requiere comprobar su funcionamiento en Tahoe; abrirlo no verifica todas
sus funciones de modelado o administración con MySQL 8.4.
