package ar.com.sislum.modelo;

public final class Profesional extends Catalogo {
    private final String apellido;
    private final String matricula;
    private final String especialidad;

    public Profesional(int id, String nombre, String apellido, String matricula, String especialidad) {
        super(id, nombre);
        this.apellido = apellido;
        this.matricula = matricula;
        this.especialidad = especialidad == null ? "" : especialidad;
    }

    public int getIdProfesional() { return getIdCatalogo(); }
    public String getApellido() { return apellido; }
    public String getMatricula() { return matricula; }
    public String getEspecialidad() { return especialidad; }
    @Override public String getTipo() { return "Profesional"; }
    @Override public String toString() { return getNombre() + " " + apellido + " · " + especialidad; }
}
