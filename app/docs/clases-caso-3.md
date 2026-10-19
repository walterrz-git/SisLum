# Registrar cobro (CU006)

Con este caso queda implementado el circuito Registrar turno → Registrar atención → Registrar cobro.
El alcance de este prototipo incluye esos tres casos y sus consultas de soporte.
El login, los roles y los módulos de administración quedan fuera de esta etapa.

## Uso de la pantalla

1. Abrir Cobros y elegir la fecha del turno en el calendario. Se consulta automáticamente el estado elegido: Pendiente, Pagado o Todos.
2. Si hace falta, escribir nombre, apellido o DNI y pulsar Buscar para filtrar esa fecha.
3. Elegir una atención. Los pagos registrados muestran su fecha, medio de pago y observaciones, sin permitir otro cobro. Para cobrar, seleccionar una pendiente. Si solo hay una, se muestra directamente su detalle.
4. Revisar los tratamientos realizados y el total. El importe no se edita.
5. Seleccionar efectivo, transferencia, tarjeta de débito o tarjeta de crédito.
6. Completar observaciones si corresponde, pulsar Registrar cobro y confirmar el resumen.

El sistema informa el número de cobro y actualiza la lista de pendientes. El botón Actualizar permite volver a consultar si hubo cambios desde otra ventana. Cancelar en la
confirmación no guarda nada; el botón Cancelar de la pantalla limpia la búsqueda y vuelve a consultar la fecha elegida.

## Clases del TP2

| Clase | Paquete | Responsabilidad |
| --- | --- | --- |
| `Cobro` | modelo | Atención, importe, medio de pago, fecha, estado y observaciones. |
| `CobroController` | controlador | Búsqueda del paciente, consulta de importes y registro del cobro. |
| `CobroDAO` | persistencia | Consulta de pendientes, guardado y recuperación del cobro por atención. |
| `VistaCobros` | vista | Búsqueda, selección, detalle, resumen y confirmación del cobro. |

`AtencionDAO.buscarPorId` recupera una atención con sus tratamientos e importes históricos.
`VistaCobros.form` permite editar la pantalla en Design de NetBeans. Las operaciones con MySQL
se ejecutan con SwingWorker para mantener disponible la ventana.

## Reglas del caso

- Cada atención tiene un único cobro y se cobra por su importe completo.
- El total usa `BigDecimal` y suma los importes guardados en los tratamientos realizados.
- Cambiar el precio del catálogo no modifica lo adeudado por una atención anterior.
- Se exige atención y medio de pago válidos antes de registrar.
- La fecha del cobro corresponde al día en que se registra.
- La atención cobrada deja de aparecer como pendiente; la historia clínica no se modifica.
- El turno continúa ATENDIDO: el estado médico y el del cobro se mantienen separados.

Los medios se guardan como EFECTIVO, TRANSFERENCIA, DEBITO o CREDITO, según los valores definidos
en el esquema del TP2. Las observaciones son opcionales.

## Guardado e integridad

CobroDAO abre una transacción y bloquea la atención. Comprueba que pertenezca al paciente
seleccionado, que su turno esté ATENDIDO y que no exista un cobro PAGADO. Luego suma los
importes históricos y verifica que coincidan con el importe que el usuario confirmó.

Si el importe cambió, se solicita actualizar los pendientes y revisar nuevamente. Si otra ventana
ya registró el cobro, se rechaza el segundo intento. La restricción única de `cobro.id_atencion`
también impide duplicados. Los errores deshacen la operación.

Si ya existía una fila con estado PENDIENTE, se completa esa misma fila como PAGADO,
conservando su identificador. Si no existía, se crea un cobro nuevo.

La columna opcional `cobro.observaciones` se incorpora mediante `sql/03_registrar_cobro.sql`.
El script se puede ejecutar más de una vez y no borra registros. El resto de la estructura
de cobros del TP2 se conserva. Incorporar este atributo al DER y al modelo del informe final.

## Verificación realizada

`VerificarRegistroCobro`, en `src/test/java`, comprueba el circuito con pacientes y tratamientos
temporales propios: cuatro medios de pago, persistencia, importes históricos, validaciones,
salida de pendientes, errores sin cambios parciales y dos intentos de cobro simultáneos.
También comprueba la actualización de un registro PENDIENTE sin crear otra fila.
Los datos de esas pruebas se eliminan al terminar.

La prueba de interfaz cubrió búsqueda, detalle de tratamientos, cancelación de la confirmación,
guardado, actualización de pendientes y un intento de cobrar una atención pagada desde otra ventana.

## Alcance de esta entrega

Se implementan CU001 Registrar turno, CU007 Registrar atención y CU006 Registrar cobro,
con alta y búsqueda de pacientes, agenda, antecedentes y tratamientos como soporte.
No se incorpora login en esta etapa. El mantenimiento de usuarios, roles, catálogos,
agendas individuales, bloqueos y feriados queda para una etapa posterior.
