# SisLum - aplicación

El proyecto está hecho en Java con Swing y MySQL. Separé el código en modelo, vista y controlador, y las consultas a la base están en las clases DAO.

## Para ejecutarlo

En NetBeans, abrir esta carpeta con **File > Open Project** y usar **Run Project**. La clase principal es `ar.com.sislum.SisLum`.

Hace falta Java 17 o superior y tener MySQL funcionando. Maven descarga las dependencias la primera vez.

Antes de iniciar, ejecutar en MySQL los archivos de `sql` en este orden:

1. `01_esquema.sql`: crea las tablas del circuito.
2. `02_registrar_turno.sql`: agrega el tratamiento previsto y los datos iniciales.
3. `03_registrar_cobro.sql`: agrega las observaciones del cobro.

La conexión se configura con `SISLUM_DB_URL`, `SISLUM_DB_USER` y `SISLUM_DB_PASSWORD`. La URL por defecto es `jdbc:mysql://127.0.0.1:3306/SisLum`. En mi Mac también puede leer el archivo externo `client.cnf`. Las contraseñas no están en el proyecto.

Desde una terminal se puede usar `mvn compile exec:java`. Para usar los scripts de esta Mac, desde la carpeta principal:

```sh
cd app
chmod +x ./*.command ../entorno/mysql.command
./ejecutar.command
```

El script `./preparar-base.command` sirve para preparar la base con los tres SQL.

## Qué se puede hacer

### Registrar turno

Buscar al paciente por nombre, apellido o DNI. Si no está cargado, se puede dar de alta con **Nuevo paciente**. Después elegir profesional, tratamiento y fecha en el calendario. Los horarios disponibles se cargan solos y se confirma el turno.

Los horarios iniciales son de lunes a viernes, de 9 a 18, cada 30 minutos. No se pueden reservar fechas pasadas ni horarios ocupados.

### Consultar agenda

Elegir fecha y, si hace falta, filtrar por profesional y hora. Con **Consultar** se muestran los turnos. También se pueden consultar fechas anteriores.

### Registrar atención

En **Atenciones**, buscar los turnos por profesional y fecha. Seleccionar uno, revisar los antecedentes, agregar los tratamientos realizados y escribir el informe. Al guardar, el turno pasa a ATENDIDO y queda pendiente de cobro.

Se necesita al menos un tratamiento y un informe. El tratamiento previsto al reservar no se carga automáticamente como realizado.

### Registrar cobro

En **Cobros**, elegir la fecha del turno y el estado **Pendiente**, **Pagado** o **Todos**. También se puede buscar por paciente o DNI.

Para cobrar, seleccionar una atención pendiente, revisar el total y elegir el medio de pago. Se puede usar efectivo, transferencia, débito o crédito. Las observaciones son opcionales.

Cada atención se cobra una sola vez por el total. Los pagos ya registrados se pueden consultar con **Pagado** o **Todos**. La búsqueda usa la fecha del turno; la fecha del pago se muestra aparte.

Los datos se guardan en MySQL. Los importes de los tratamientos quedan guardados con la atención para que un cambio de precio posterior no modifique lo que se debe cobrar.

## Pantallas y clases

Las pantallas están en `ar.com.sislum.vista`. Para verlas en NetBeans, abrir la clase y elegir **Design**. Cada archivo `.java` tiene su `.form`, y hay que conservar ambos.

El calendario compartido es `SelectorFecha`, que usa LGoodDatePicker. Maven descarga esa biblioteca.

Dejé un recorrido de las clases de cada caso en estos archivos:

- [Registrar turno](docs/clases-caso-1.md)
- [Registrar atención](docs/clases-caso-2.md)
- [Registrar cobro](docs/clases-caso-3.md)

## Comprobaciones

Con la base configurada, se pueden ejecutar con:

```sh
./verificar.command
```

Revisan el registro de pacientes, los turnos, las atenciones y los cobros, incluyendo duplicados y operaciones simultáneas. Están en `src/test/java`, no usan JUnit y eliminan los datos que crean al terminar.

Para este TP se implementaron los tres casos del circuito: registrar turno, registrar atención y registrar cobro, junto con las consultas de pacientes, agenda e historia clínica.
