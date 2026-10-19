package ar.com.sislum.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TratamientoRealizado {
    private final int idDetalle;
    private final Tratamiento tratamiento;
    private final BigDecimal importe;
    private final String observaciones;

    public TratamientoRealizado(int idDetalle, Tratamiento tratamiento, BigDecimal importe, String observaciones) {
        if (tratamiento == null || importe == null || importe.signum() < 0) {
            throw new IllegalArgumentException("El tratamiento y su importe son obligatorios.");
        }
        if (importe.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("El importe supera el máximo permitido.");
        }
        if (observaciones != null && observaciones.length() > 15000) {
            throw new IllegalArgumentException("El detalle no puede superar los 15000 caracteres.");
        }
        this.idDetalle = idDetalle;
        this.tratamiento = tratamiento;
        this.importe = importe.setScale(2, RoundingMode.UNNECESSARY);
        this.observaciones = observaciones == null ? "" : observaciones.trim();
    }

    public int getIdDetalle() { return idDetalle; }
    public Tratamiento getTratamiento() { return tratamiento; }
    public BigDecimal getImporte() { return importe; }
    public String getObservaciones() { return observaciones; }
}
