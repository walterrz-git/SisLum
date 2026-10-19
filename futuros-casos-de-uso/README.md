# Futuros casos de uso

Esta carpeta queda fuera del proyecto Maven `app`. Conserva material para retomar
las funcionalidades que no forman parte del prototipo actual del TP3.

## Material separado

`sql/01_seguridad.sql` contiene las definiciones originales de las tablas `usuario`,
`rol`, `permiso` y `rol_permiso`, retiradas del script de preparación activo.
No se ejecuta al preparar ni iniciar la aplicación actual. Para utilizarlo más adelante,
primero debe existir la base del proyecto activo, incluida la tabla `profesional`.

No había clases Java de login, usuarios, roles o administración implementadas para mover.
No se crean clases vacías ni pantallas de esos módulos.

## Funcionalidades postergadas

- Iniciar sesión y controlar el acceso.
- Administrar usuarios, roles y permisos.
- Administrar profesionales, tratamientos y precios.
- Configurar agendas individuales, días y horarios.
- Bloquear días y horarios y registrar feriados.
- Administrar parámetros generales.
- Modificar y cancelar turnos desde una pantalla.

Las clases Profesional, Tratamiento, Catalogo y Agenda permanecen en la aplicación actual
porque son necesarias para los tres casos implementados. Sus futuras pantallas de administración
son las que quedan postergadas.

## Proyecto activo

El proyecto `app` mantiene Registrar turno → Registrar atención → Registrar cobro,
con alta y búsqueda de pacientes, consulta de agenda e historia clínica como soporte.
La separación modifica archivos de preparación y documentación; no elimina tablas ni
registros de la base MySQL existente.
