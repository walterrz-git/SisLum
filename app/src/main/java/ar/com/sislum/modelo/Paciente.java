package ar.com.sislum.modelo;

// Datos del paciente.
public final class Paciente {
    private final int idPaciente;
    private final String dni;
    private final String nombre;
    private final String apellido;
    private final String telefono;
    private final String email;

    public Paciente(String dni, String nombre, String apellido, String telefono, String email) {
        this(0, dni, nombre, apellido, telefono, email);
    }

    public Paciente(int idPaciente, String dni, String nombre, String apellido, String telefono, String email) {
        if (dni == null || !dni.trim().matches("[0-9]{7,8}")) {
            throw new IllegalArgumentException("El DNI debe contener 7 u 8 dígitos, sin puntos.");
        }
        if (nombre == null || nombre.isBlank() || apellido == null || apellido.isBlank()) {
            throw new IllegalArgumentException("Nombre y apellido son obligatorios.");
        }
        if (nombre.trim().length() > 100 || apellido.trim().length() > 100
                || (telefono != null && telefono.trim().length() > 30)
                || (email != null && email.trim().length() > 150)) {
            throw new IllegalArgumentException("Uno de los datos supera la longitud permitida.");
        }
        this.idPaciente = idPaciente;
        this.dni = dni.trim();
        this.nombre = nombre.trim();
        this.apellido = apellido.trim();
        this.telefono = telefono == null ? "" : telefono.trim();
        this.email = email == null ? "" : email.trim();
    }

    public int getIdPaciente() { return idPaciente; }
    public String getDni() { return dni; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
}
