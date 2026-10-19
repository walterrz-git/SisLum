# SisLum

Aplicación de escritorio en Java y Swing, organizada en Modelo, Vista, Controlador y DAO.
Proyecto Maven para Java 17 o superior. En este equipo utiliza el JDK 23 y NetBeans 29.

## Abrir y ejecutar en NetBeans

1. Elegir **File > Open Project** y seleccionar la carpeta `app` dentro de `SisLum`.
2. Ejecutar **Run Project**. La clase principal es `ar.com.sislum.SisLum`.
3. La aplicación abre directamente **Nuevo turno**. Usar el menú para ir a **Agenda**, **Pacientes**, **Atenciones** o **Cobros**.

La primera compilación necesita conexión para descargar las dependencias de Maven.
La conexión local utiliza la configuración de MySQL ya existente fuera del proyecto.
No hay que ingresar ni copiar la contraseña al código.

También se puede ejecutar desde Terminal, ubicado en `SisLum`:

```sh
cd app
./ejecutar.command
```

## Registrar turno

Abrir **Nuevo turno** desde el menú lateral.

1. Buscar al paciente por nombre, apellido o DNI y seleccionar una fila.
2. Si no existe, elegir **Nuevo paciente**, completar sus datos y guardar.
   El paciente queda seleccionado para continuar.
3. Elegir profesional, tratamiento previsto y fecha desde el calendario, que muestra `dd/MM/aaaa`.
4. Los horarios se cargan automáticamente. Elegir uno de los disponibles.
5. Pulsar **Confirmar turno**, revisar el resumen y aceptar.

Los pacientes y turnos quedan guardados en MySQL al cerrar la aplicación.
La disponibilidad inicial es de lunes a viernes, de 9 a 18, cada 30 minutos.
Se excluyen las fechas pasadas, los horarios ya transcurridos y las reservas confirmadas o atendidas.
Cambiar profesional o fecha actualiza automáticamente la disponibilidad. Durante la consulta
no se puede confirmar el turno. Si no hay horarios, se muestra un mensaje y la confirmación queda deshabilitada.
**Cancelar** limpia el formulario y vuelve a la pantalla Agenda.
Los turnos cancelados no ocupan un horario. La cancelación todavía no tiene pantalla.

## Consultar agenda

Abrir **Agenda**, elegir el día en el calendario y seleccionar profesional y hora. Se puede consultar con
**Todos los profesionales** y **Todos los horarios**. Pulsar **Consultar** para actualizar
la tabla. También se pueden consultar días pasados. Al cambiar un filtro se limpia el
resultado anterior para evitar confundirlo con la consulta nueva.

## Registrar atención

1. Abrir **Atenciones**, elegir profesional y fecha desde el calendario y pulsar **Consultar turnos**.
2. Seleccionar el turno del paciente atendido. Solo aparecen turnos CONFIRMADOS.
3. Revisar sus antecedentes, que se cargan a la derecha.
4. Elegir cada tratamiento efectivamente realizado, completar dosis o detalle si corresponde
   y pulsar **Agregar tratamiento**. Se puede quitar un tratamiento antes de guardar.
5. Escribir el informe médico y pulsar **Guardar atención**.
6. Revisar el resumen y confirmar.

El sistema guarda la atención y todos sus tratamientos en una transacción, actualiza la
historia clínica y marca el turno como ATENDIDO. El total queda pendiente de cobro.
No se registra un pago en esta operación. Se requiere al menos un tratamiento y un informe.
El tratamiento previsto del turno no se agrega automáticamente como realizado.
La fecha de la atención es la fecha en que se registra.

Si se intenta cambiar de turno o volver a consultar con datos sin guardar, se solicita
confirmar que se descarten. Cambiar de módulo conserva el borrador en la pantalla de atención.
Después de guardar, el turno deja de aparecer en la lista de pendientes y permanece visible
en Agenda con estado ATENDIDO. Para consultar los antecedentes, seleccionar otro turno del paciente.

## Registrar cobro

1. Abrir **Cobros** y elegir la fecha del turno y el estado **Pendiente**, **Pagado** o **Todos**. La consulta se actualiza al cambiar fecha o estado.
2. Si hace falta, filtrar por nombre, apellido o DNI y pulsar **Buscar**. Seleccionar la atención pendiente que se va a cobrar.
3. Revisar los tratamientos realizados y el total; el importe no se edita.
4. Elegir efectivo, transferencia, tarjeta de débito o tarjeta de crédito.
5. Completar las observaciones si corresponde, pulsar **Registrar cobro** y confirmar.

El cobro queda PAGADO y la atención deja de aparecer como pendiente. Cada atención tiene
un único cobro por su importe completo. El total utiliza los importes históricos guardados
al registrar la atención, aunque después cambien los precios del catálogo.
La fecha del cobro es la del día de registro. **Actualizar** vuelve a consultar
la base, y **Cancelar** limpia la búsqueda y consulta la fecha elegida. Cancelar el resumen de confirmación no guarda nada.
Los pagos registrados se consultan con **Pagado** o **Todos**. Se muestran fecha y medio de pago, y se deshabilita Registrar cobro para la atención pagada.

## Editar las pantallas

En **Source Packages > ar.com.sislum.vista**, abrir la clase y seleccionar **Design**:

- `VistaTurnos`: pantalla de Nuevo turno, caso CU001.
- `VistaAgenda`: filtros y consulta de la agenda.
- `VistaAtencion`: registro de atención, tratamientos e historia clínica, caso CU007.
- `VistaCobros`: atención pendiente, detalle, medio de pago y confirmación, caso CU006.
- `VistaPacientes`: búsqueda y lista de pacientes.
- `FormularioPaciente`: campos del alta de paciente.
- `VentanaPrincipal`: menú y contenedores de las pantallas.

Las pantallas se incorporan a los contenedores desde el constructor de VentanaPrincipal.
Para editar sus componentes, abrir sus propios formularios.
El calendario común es `SelectorFecha`, un componente auxiliar basado en LGoodDatePicker.
Está declarado en los `.form`; no hace falta instalarlo manualmente en la paleta. Si el
diseñador estaba abierto al actualizar el proyecto, compilar y volver a abrir el formulario.

Cada `.java` debe conservar su `.form`. NetBeans administra los bloques `initComponents`
y de variables; las acciones y las llamadas al controlador están fuera de ellos.

## Base de datos

La base local ya está preparada. Para volver a preparar un entorno con la estructura del TP2:

```sh
./preparar-base.command
```

El script conserva los registros existentes y prepara únicamente el alcance actual. Ejecuta:

- `sql/01_esquema.sql`: las nueve tablas del circuito actual y sus datos de soporte, si no existen.
- `sql/02_registrar_turno.sql`: tratamiento previsto del turno y datos ficticios iniciales.
- `sql/03_registrar_cobro.sql`: observaciones opcionales del cobro.

En otro equipo, ejecutar los tres archivos en orden en MySQL Workbench y configurar las variables
`SISLUM_DB_URL`, `SISLUM_DB_USER` y `SISLUM_DB_PASSWORD` antes de iniciar la aplicación.
La URL local predeterminada es `jdbc:mysql://127.0.0.1:3306/SisLum`.
En esta Mac, `ConexionBD` puede leer el archivo `client.cnf` del entorno existente.
Las variables de entorno tienen prioridad sobre ese archivo.

Los datos iniciales son tres profesionales con matrícula `DEMO` y los tratamientos
Consulta médica, Peeling facial y Plasma rico en plaquetas. Sus precios son ficticios.
No se agregan pacientes de demostración a la base.

## Código y alcance

Ver [Registrar turno](docs/clases-caso-1.md), [Registrar atención](docs/clases-caso-2.md)
y [Registrar cobro](docs/clases-caso-3.md) para recorrer las clases de cada caso.

Implementado: alta y búsqueda de pacientes, historia clínica inicial, consulta de agenda,
registro del turno, agenda con filtros, registro de atención y tratamientos,
consulta de antecedentes, cálculo del importe pendiente, registro del cobro y persistencia con JDBC/MySQL.

El alcance acordado para esta etapa es Registrar turno → Registrar atención → Registrar cobro,
con las consultas de soporte. El login y los roles no se implementan todavía.
También quedan fuera de esta etapa el mantenimiento de catálogos, la configuración individual
de agendas, los bloqueos y los feriados. El material de funcionalidades postergadas está
en [futuros-casos-de-uso](../futuros-casos-de-uso/README.md), fuera del proyecto Maven.
Las tablas de usuarios, roles y permisos ya no se incluyen en la preparación activa.

## Verificación

```sh
./verificar.command
```

Ejecuta una comprobación de integración sobre la base local: alta y duplicados, historia clínica,
búsqueda, disponibilidad, persistencia, dos reservas simultáneas y rollback ante un error.
Incluye además filtros de agenda, atención, antecedentes, importes históricos,
rechazo de atenciones duplicadas y concurrencia sobre el mismo turno.
La verificación de Cobros cubre los cuatro medios de pago, el total histórico, la persistencia,
la actualización de pendientes y el rechazo de cobros duplicados o simultáneos.
Los registros que crean estas comprobaciones se eliminan al terminar. No utilizan JUnit.
La comprobación está en `src/test/java` y no forma parte de las clases del dominio.

La dependencia JDBC está declarada en `pom.xml` siguiendo la
[documentación de Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-installing-maven.html).

## Calendario

Nuevo turno, Agenda, Atenciones y Cobros comparten `SelectorFecha`. La selección se hace desde el calendario,
en español y con formato `dd/MM/aaaa`; no se escribe la fecha a mano.
Nuevo turno bloquea los días pasados. Agenda y Atenciones permiten consultar fechas anteriores.
La dependencia está en `pom.xml` y Maven la descarga automáticamente.

Biblioteca utilizada: [LGoodDatePicker](https://github.com/LGoodDatePicker/LGoodDatePicker).
