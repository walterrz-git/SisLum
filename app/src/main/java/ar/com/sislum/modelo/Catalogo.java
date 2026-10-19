package ar.com.sislum.modelo;

// Datos comunes de profesionales y tratamientos.
public abstract class Catalogo {
    private final int idCatalogo;
    private final String nombre;

    protected Catalogo(int idCatalogo, String nombre) {
        if (idCatalogo <= 0 || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Los datos del catálogo están incompletos.");
        }
        this.idCatalogo = idCatalogo;
        this.nombre = nombre.trim();
    }

    public int getIdCatalogo() { return idCatalogo; }
    public String getNombre() { return nombre; }
    public abstract String getTipo();

    @Override
    public String toString() { return nombre; }
}
