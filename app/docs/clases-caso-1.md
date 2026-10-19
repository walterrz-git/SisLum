# Clases del caso 1: Registrar turno

Las clases de negocio, controladores, vistas y DAO de este caso conservan los nombres
indicados en el diagrama de clases del TP2. Se implementa el subconjunto necesario para CU001,
con CU002 Buscar paciente, CU003 Registrar paciente y CU004 Consultar agenda como soporte.

## Modelo

| Clase | Qué representa |
| --- | --- |
| `Paciente` | Identificador de base, DNI, nombre, apellido y datos de contacto. |
| `Profesional` | Profesional, matrícula y especialidad. |
| `Agenda` | Agenda de un profesional y cálculo de horarios disponibles. |
| `Turno` | Paciente, agenda, fecha, hora, estado y tratamiento previsto. |
| `Tratamiento` | Tratamiento disponible y su precio de referencia. |
| `Catalogo` | Clase abstracta común a Profesional y Tratamiento, según la herencia del diagrama. |

Los atributos son privados. Los constructores reciben los datos necesarios y validan lo
que corresponde. `Profesional` y `Tratamiento` heredan de `Catalogo` y redefinen `getTipo()`.
La vista recibe ambas clases como elementos de `Catalogo` para cargar las opciones.
Las clases de atención se incorporaron en CU007 y las de cobro en CU006, como se describe
en las guías de los casos 2 y 3.

## Controladores

| Clase | Operaciones de esta etapa |
| --- | --- |
| `PacienteController` | Registrar y buscar pacientes mediante PacienteDAO. |
| `TurnoController` | Buscar paciente, consultar profesionales y tratamientos, consultar disponibilidad y registrar turno. |

## Persistencia

| Clase | Responsabilidad |
| --- | --- |
| `ConexionBD` | Abrir la conexión JDBC con configuración externa. |
| `PacienteDAO` | Buscar y guardar pacientes. El alta crea su historia clínica en la misma transacción. |
| `CatalogoDAO` | Consultar profesionales, tratamientos y la agenda de cada profesional. |
| `TurnoDAO` | Consultar horarios ocupados, guardar el turno y recuperar los turnos del día. |

Las sentencias utilizan parámetros. Al registrar se bloquea la fila de la agenda dentro de
una transacción y se vuelve a comprobar el horario antes de insertar. De ese modo, dos
instancias de esta aplicación no pueden reservar simultáneamente el mismo horario del profesional.
Si falla la operación, se deshacen sus cambios. Cada conexión se cierra al terminar.
Esta protección requiere que las reservas se realicen mediante este DAO; la tabla original del TP2
no tiene una restricción única para los horarios activos.

## Vistas

| Clase | Uso |
| --- | --- |
| `VistaTurnos` | Pantalla de Nuevo turno siguiendo los tres pasos del prototipo del TP2. |
| `VistaAgenda` | Pantalla independiente con filtros por día, hora y profesional. |
| `SelectorFecha` | Calendario común de las pantallas; componente auxiliar de interfaz. |
| `VistaPacientes` | Búsqueda y consulta de pacientes; comparte el alta con VistaTurnos. |
| `FormularioPaciente` | Panel auxiliar de campos para el alta. |
| `VentanaPrincipal` | Ventana auxiliar con el menú y los contenedores de los módulos. |
| `SisLum` | Punto de entrada de la aplicación. |

`FormularioPaciente`, `VentanaPrincipal` y `SisLum` son clases auxiliares de interfaz e inicio;
no sustituyen ni agregan entidades al dominio del TP2. Cada pantalla editable tiene su `.form`.
Las consultas se ejecutan con SwingWorker para mantener disponible la interfaz.

## Ajustes concretos respecto del TP2

- Se conservan las tablas necesarias para los casos actuales. Las definiciones de seguridad
  se separaron en `futuros-casos-de-uso/sql/01_seguridad.sql`, sin cambiar la base existente.
  Se agrega `turno.id_tratamiento`, opcional
  para los turnos anteriores, como referencia al tratamiento previsto que aparece en el prototipo.
- `Turno` incorpora esa referencia. No se crean tratamientos realizados ni importes a cobrar al reservar.
- `Agenda` aplica por ahora una regla común: lunes a viernes, de 9 a 18, cada 30 minutos.
  Aún no hay configuración individual de horarios, bloqueos ni feriados.
- El identificador del paciente se recupera de MySQL para relacionarlo con el turno.
- El precio usa `BigDecimal`; la fecha y la hora usan `LocalDate`, `LocalTime` y `LocalDateTime`.

Al actualizar el informe acumulativo, incorporar la asociación entre Turno y el tratamiento
previsto en el diagrama de clases y el DER. La documentación de esta implementación no modifica
los archivos originales del TP2.

## Preparación de los casos siguientes

- El turno conserva su identificador, paciente, agenda, profesional y estado CONFIRMADO.
- El paciente tiene una historia clínica vacía creada al registrarlo.
- El modelo de datos existente ya relaciona atención con turno e historia clínica.
- Los tratamientos tienen precios, pero el importe definitivo se calculará a partir de los
  tratamientos efectivamente realizados en la atención.
- La tabla cobro existente permite un único cobro por atención.

Registrar atención y Registrar cobro ya están implementados; ver las guías de los casos 2 y 3.
El login y los roles quedan fuera del alcance acordado para esta etapa.

## Selección de fecha y disponibilidad

`SelectorFecha` utiliza LGoodDatePicker y devuelve `LocalDate`. Se usa el mismo componente
para Nuevo turno, Agenda y Atenciones, con presentación en español y formato dd/MM/aaaa.
En Nuevo turno se impide seleccionar días anteriores al actual. Al elegir fecha o cambiar
profesional, se consultan automáticamente los horarios. Se retiró el botón Consultar horarios.
Durante la consulta se deshabilita la confirmación y se descartan los horarios de la consulta
anterior. Si cambia la selección antes de terminar, se consulta la última fecha y profesional.
La validación definitiva sigue realizándose dentro de la transacción al guardar.
