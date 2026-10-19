package ar.com.sislum.modelo;

import java.math.BigDecimal;

public final class Tratamiento extends Catalogo {
    private final String descripcion;
    private final BigDecimal precio;

    public Tratamiento(int id, String nombre, String descripcion, BigDecimal precio) {
        super(id, nombre);
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        this.descripcion = descripcion;
        this.precio = precio;
    }

    public int getIdTratamiento() { return getIdCatalogo(); }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecio() { return precio; }
    @Override public String getTipo() { return "Tratamiento"; }
}
