package ar.com.sislum.persistencia;

import ar.com.sislum.modelo.Paciente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class PacienteDAO {
    public List<Paciente> buscar(String consulta) throws SQLException {
        String criterio = "%" + consulta.trim().replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return buscarLiteral(conexion, criterio);
        }
    }

    private List<Paciente> buscarLiteral(Connection conexion, String criterio) throws SQLException {
        String sql = "SELECT * FROM paciente WHERE dni LIKE ? ESCAPE '!' OR nombre LIKE ? ESCAPE '!' "
                + "OR apellido LIKE ? ESCAPE '!' OR CONCAT(nombre, ' ', apellido) LIKE ? ESCAPE '!' "
                + "ORDER BY apellido, nombre, id_paciente";
        List<Paciente> pacientes = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) { sentencia.setString(i, criterio); }
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) { pacientes.add(desdeFila(filas)); }
            }
        }
        return pacientes;
    }

    public Paciente buscarPorDni(String dni) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM paciente WHERE dni = ?")) {
            sentencia.setString(1, dni);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? desdeFila(filas) : null;
            }
        }
    }

    public Paciente guardar(Paciente paciente) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "INSERT INTO paciente (dni,nombre,apellido,telefono,email) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    sentencia.setString(1, paciente.getDni());
                    sentencia.setString(2, paciente.getNombre());
                    sentencia.setString(3, paciente.getApellido());
                    sentencia.setString(4, paciente.getTelefono());
                    sentencia.setString(5, paciente.getEmail());
                    sentencia.executeUpdate();
                    try (ResultSet claves = sentencia.getGeneratedKeys()) {
                        if (!claves.next()) { throw new SQLException("No se obtuvo el identificador del paciente."); }
                        id = claves.getInt(1);
                    }
                }
                // La historia queda lista para registrar las atenciones siguientes.
                try (PreparedStatement historia = conexion.prepareStatement(
                        "INSERT INTO historia_clinica (id_paciente) VALUES (?)")) {
                    historia.setInt(1, id);
                    historia.executeUpdate();
                }
                conexion.commit();
                return new Paciente(id, paciente.getDni(), paciente.getNombre(), paciente.getApellido(),
                        paciente.getTelefono(), paciente.getEmail());
            } catch (SQLException error) {
                conexion.rollback();
                if (error.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Ya existe un paciente con ese DNI.", error);
                }
                throw error;
            }
        }
    }

    private Paciente desdeFila(ResultSet filas) throws SQLException {
        return new Paciente(filas.getInt("id_paciente"), filas.getString("dni"), filas.getString("nombre"),
                filas.getString("apellido"), filas.getString("telefono"), filas.getString("email"));
    }
}
