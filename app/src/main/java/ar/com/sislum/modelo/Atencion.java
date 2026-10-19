package ar.com.sislum.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Atencion {
    private final int idAtencion;
    private final int idHistoria;
    private final Turno turno;
    private final LocalDate fecha;
    private final String informe;
    private final List<TratamientoRealizado> tratamientos;

    public Atencion(int idAtencion, int idHistoria, Turno turno, LocalDate fecha,
            String informe, List<TratamientoRealizado> tratamientos) {
        if (turno == null || fecha == null || informe == null || informe.isBlank()) {
            throw new IllegalArgumentException("Seleccioná un turno y completá el informe médico.");
        }
        if (tratamientos == null || tratamientos.isEmpty()) {
            throw new IllegalArgumentException("Agregá al menos un tratamiento realizado.");
        }
        if (informe.length() > 15000) {
            throw new IllegalArgumentException("El informe no puede superar los 15000 caracteres.");
        }
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        BigDecimal total = new BigDecimal("0.00");
        for (TratamientoRealizado detalle : tratamientos) {
            if (detalle == null || !ids.add(detalle.getTratamiento().getIdTratamiento())) {
                throw new IllegalArgumentException("No repitas el mismo tratamiento en la atención.");
            }
            total = total.add(detalle.getImporte());
        }
        if (total.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("El total supera el máximo permitido.");
        }
        this.idAtencion = idAtencion;
        this.idHistoria = idHistoria;
        this.turno = turno;
        this.fecha = fecha;
        this.informe = informe.trim();
        this.tratamientos = List.copyOf(tratamientos);
    }

    public BigDecimal getImporteTotal() {
        BigDecimal total = new BigDecimal("0.00");
        for (TratamientoRealizado detalle : tratamientos) { total = total.add(detalle.getImporte()); }
        return total;
    }

    public int getIdAtencion() { return idAtencion; }
    public int getIdHistoria() { return idHistoria; }
    public Turno getTurno() { return turno; }
    public LocalDate getFecha() { return fecha; }
    public String getInforme() { return informe; }
    public List<TratamientoRealizado> getTratamientos() { return tratamientos; }
}
