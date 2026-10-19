package ar.com.sislum.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

// La atención se cobra completa, una sola vez.
public final class Cobro {
    private final int idCobro;
    private final Atencion atencion;
    private final BigDecimal importeTotal;
    private final String medioPago;
    private final LocalDate fecha;
    private final String estado;
    private final String observaciones;

    public Cobro(int idCobro, Atencion atencion, BigDecimal importeTotal, String medioPago,
            LocalDate fecha, String estado, String observaciones) {
        if (atencion == null || atencion.getIdAtencion() <= 0) {
            throw new IllegalArgumentException("Seleccioná una atención registrada.");
        }
        if (!Set.of("EFECTIVO","TRANSFERENCIA","DEBITO","CREDITO").contains(medioPago == null ? "" : medioPago)) {
            throw new IllegalArgumentException("Seleccioná un medio de pago válido.");
        }
        if (importeTotal == null || importeTotal.signum() < 0
                || importeTotal.compareTo(new BigDecimal("9999999999.99")) > 0
                || importeTotal.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El importe del cobro no es válido.");
        }
        if (fecha == null || !("PAGADO".equals(estado) || "PENDIENTE".equals(estado))) {
            throw new IllegalArgumentException("La fecha o el estado del cobro no son válidos.");
        }
        if (observaciones != null && observaciones.length() > 15000) {
            throw new IllegalArgumentException("Las observaciones no pueden superar los 15000 caracteres.");
        }
        this.idCobro = idCobro;
        this.atencion = atencion;
        this.importeTotal = importeTotal.setScale(2);
        this.medioPago = medioPago;
        this.fecha = fecha;
        this.estado = estado;
        this.observaciones = observaciones == null ? "" : observaciones.trim();
    }

    public int getIdCobro() { return idCobro; }
    public Atencion getAtencion() { return atencion; }
    public BigDecimal getImporteTotal() { return importeTotal; }
    public String getMedioPago() { return medioPago; }
    public LocalDate getFecha() { return fecha; }
    public String getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }
}
