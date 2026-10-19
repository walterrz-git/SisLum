# Registrar atención y separar la consulta de agenda

Esta etapa implementa CU007 Registrar atención, con consulta de turnos asignados,
antecedentes, tratamientos realizados, informe médico y cálculo del importe a cobrar.
También separa Nuevo turno de Agenda según la navegación acordada.

## Pantallas

- `VistaTurnos` mantiene exclusivamente el registro del turno. Cancelar limpia los campos y vuelve a Agenda.
- `VistaAgenda` es la clase nueva para consultar por día, hora y profesional. Admite todos los profesionales
  y todos los horarios. Permite consultar fechas anteriores y muestra el estado de cada turno.
- `VistaAtencion` utiliza el nombre del diagrama del TP2. Permite seleccionar el turno, consultar la historia
  clínica, cargar uno o varios tratamientos e informe y confirmar la atención.

Las tres pantallas tienen archivos `.form` para el diseñador de NetBeans. Las tres pantallas comparten el calendario `SelectorFecha`, configurado en español;
Agenda y Atenciones admiten fechas pasadas. La lista de tratamientos
realizados y la consulta de antecedentes son parte de VistaAtencion. Los campos del informe y detalle
permanecen deshabilitados hasta seleccionar un turno. Los filtros de atención incluyen profesional y fecha;
la lista muestra únicamente sus turnos CONFIRMADOS. El login queda fuera del alcance de esta etapa, según lo acordado.

## Clases incorporadas del TP2

| Clase | Paquete | Función |
| --- | --- | --- |
| `Atencion` | modelo | Turno atendido, historia, fecha de registro, informe, tratamientos y total. |
| `TratamientoRealizado` | modelo | Tratamiento, detalle o dosis e importe histórico de esa realización. |
| `HistoriaClinica` | modelo | Paciente y atenciones anteriores, con sus informes y tratamientos. |
| `AtencionController` | controlador | Consulta de turnos confirmados, historia clínica, validación y registro. |
| `AtencionDAO` | persistencia | Guarda la atención completa, actualiza el turno y recupera los antecedentes. |
| `VistaAtencion` | vista | Interfaz del caso CU007. |

`VistaAgenda` es la incorporación acordada respecto del diagrama previo. No agrega una entidad al negocio.
`TurnoController` y `TurnoDAO` amplían la consulta de agenda para admitir filtros y días pasados;
las validaciones que impiden reservar en fechas pasadas permanecen en el registro de turnos.

## Cómo se guarda

1. Se verifica que haya turno, informe y al menos un tratamiento, sin tratamientos repetidos.
2. Se abre una transacción y se bloquea el turno para evitar que dos ventanas lo atiendan a la vez.
3. Se verifica que siga CONFIRMADO y se obtiene su paciente, profesional e historia clínica.
4. Se consultan los precios vigentes. Esos importes se guardan en `tratamiento_realizado.importe`.
5. Se guarda la atención, el informe y todos los tratamientos realizados.
6. Se cambia el estado del turno a ATENDIDO y se confirman los cambios.

Ante un error, se deshacen los cambios de la operación. La restricción única sobre `atencion.id_turno`
también impide que se creen dos atenciones para el mismo turno. La fecha de atención corresponde
al día en que se registra; no se copia la fecha prevista del turno.

No fue necesario modificar las tablas del TP2. Se utilizan `atencion`, `historia_clinica`,
`tratamiento_realizado`, `tratamiento`, `turno` y `agenda` con sus relaciones existentes.

## Importes y caso de cobro

Los importes usan `BigDecimal`. El total mostrado antes de confirmar es una estimación con los precios
cargados en pantalla. El guardado usa los precios vigentes en la base y devuelve el total definitivo.
Un cambio de precios posterior no altera los importes de las atenciones ya registradas.

La atención queda pendiente mientras no tenga un cobro PAGADO. `AtencionDAO.consultarImportePendiente`
calcula ese saldo del paciente a partir de los tratamientos realizados. Esta operación no crea un pago
ni solicita su medio: el pago se registra por separado en CU006, descrito en `clases-caso-3.md`.

El tratamiento previsto al reservar es una referencia. El profesional debe agregar los que efectivamente
realizó: la atención no los copia automáticamente del turno.

## Evidencia de funcionamiento

Las comprobaciones en `src/test/java` cubren filtros de agenda, antecedentes, atención con varios
tratamientos, total, rechazo de duplicados, conservación de importes históricos, rollback y dos
registros simultáneos del mismo turno. Los datos de prueba se eliminan al terminar.

Para el informe acumulativo, actualizar el menú y el diagrama de clases con VistaAgenda y mostrar
capturas de Nuevo turno, Agenda y Registrar atención funcionando. Los archivos originales del TP2
no se modifican en esta etapa.
