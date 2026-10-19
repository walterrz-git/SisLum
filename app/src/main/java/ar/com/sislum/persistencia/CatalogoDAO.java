package ar.com.sislum.persistencia;

import ar.com.sislum.modelo.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class CatalogoDAO {
    public List<Profesional> consultarProfesionales() throws SQLException {
        List<Profesional> profesionales = new ArrayList<>();
        String sql = "SELECT p.* FROM profesional p JOIN agenda a ON a.id_profesional = p.id_profesional "
                + "ORDER BY p.apellido, p.nombre";
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(sql);
                ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                profesionales.add(new Profesional(filas.getInt("id_profesional"), filas.getString("nombre"),
                        filas.getString("apellido"), filas.getString("matricula"), filas.getString("especialidad")));
            }
        }
        return profesionales;
    }

    public List<Tratamiento> consultarTratamientos() throws SQLException {
        List<Tratamiento> tratamientos = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM tratamiento ORDER BY nombre");
                ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                tratamientos.add(new Tratamiento(filas.getInt("id_tratamiento"), filas.getString("nombre"),
                        filas.getString("descripcion"), filas.getBigDecimal("precio")));
            }
        }
        return tratamientos;
    }

    public Agenda consultarAgenda(Profesional profesional) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(
                        "SELECT id_agenda FROM agenda WHERE id_profesional = ?")) {
            sentencia.setInt(1, profesional.getIdProfesional());
            try (ResultSet filas = sentencia.executeQuery()) {
                if (!filas.next()) { throw new IllegalArgumentException("El profesional no tiene agenda configurada."); }
                return new Agenda(filas.getInt(1), profesional);
            }
        }
    }
}
