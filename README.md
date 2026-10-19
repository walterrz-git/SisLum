# SisLum

Este es el proyecto que estoy haciendo para el TP3 de Seminario de Práctica Informática. Es un sistema para un centro de medicina estética, hecho en Java con Swing y MySQL.

Por ahora se puede registrar un turno, cargar la atención del paciente y registrar el cobro. También tiene búsqueda y alta de pacientes, agenda e historia clínica. En Cobros se pueden consultar los pendientes y los que ya están pagados.

## Cómo abrirlo

Usé NetBeans para trabajar en el proyecto. Hay que tener Java 17 o superior y MySQL.

1. Ejecutar los tres archivos de [app/sql](app/sql) en orden: `01_esquema.sql`, `02_registrar_turno.sql` y `03_registrar_cobro.sql`.
2. Configurar la conexión con las variables `SISLUM_DB_URL`, `SISLUM_DB_USER` y `SISLUM_DB_PASSWORD`. La base se llama `SisLum` y la conexión por defecto usa `127.0.0.1:3306`.
3. En NetBeans, ir a **File > Open Project**, elegir la carpeta `app` y ejecutar **Run Project**.

La clase principal es `ar.com.sislum.SisLum`. La primera vez Maven descarga las dependencias, así que hace falta conexión a internet.

También se puede ejecutar desde una terminal, entrando a `app` y usando `mvn compile exec:java`, con la conexión a la base configurada.

## Carpetas

- [app](app): código, pantallas y SQL. En su [README](app/README.md) dejé más detalles para usarlo.
- [entorno](entorno): scripts que uso para ejecutar MySQL en mi Mac.
- [futuros-casos-de-uso](futuros-casos-de-uso): material que quedó separado para continuar más adelante.

Para esta entrega trabajé el circuito **turno → atención → cobro**. El login, los roles y la administración de profesionales y tratamientos quedan para otra etapa.

Los profesionales, tratamientos y precios iniciales son de ejemplo. No subí contraseñas ni datos de pacientes.
