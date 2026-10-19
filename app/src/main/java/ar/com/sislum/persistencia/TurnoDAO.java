package ar.com.sislum.persistencia;

import ar.com.sislum.modelo.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class TurnoDAO {
    public List<LocalTime> buscarOcupados(int idAgenda, LocalDate fecha) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return buscarOcupados(conexion, idAgenda, fecha);
        }
    }

    private List<LocalTime> buscarOcupados(Connection conexion, int idAgenda, LocalDate fecha)
            throws SQLException {
        List<LocalTime> ocupados = new ArrayList<>();
        String sql = "SELECT hora FROM turno WHERE id_agenda = ? AND fecha = ? "
                + "AND estado IN ('CONFIRMADO','ATENDIDO')";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idAgenda);
            sentencia.setDate(2, Date.valueOf(fecha));
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) { ocupados.add(filas.getTime("hora").toLocalTime()); }
            }
        }
        return ocupados;
    }

    public Turno guardar(Turno turno) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                // Dos reservas de la misma agenda se comprueban una después de la otra.
                try (PreparedStatement bloqueo = conexion.prepareStatement(
                        "SELECT id_agenda FROM agenda WHERE id_agenda = ? FOR UPDATE")) {
                    bloqueo.setInt(1, turno.getAgenda().getIdAgenda());
                    try (ResultSet filas = bloqueo.executeQuery()) {
                        if (!filas.next()) { throw new IllegalArgumentException("La agenda ya no existe."); }
                    }
                }
                LocalDate fecha = turno.getFechaHora().toLocalDate();
                LocalTime hora = turno.getFechaHora().toLocalTime();
                List<LocalTime> disponibles = turno.getAgenda().consultarDisponibilidad(fecha,
                        buscarOcupados(conexion, turno.getAgenda().getIdAgenda(), fecha));
                if (!disponibles.contains(hora)) {
                    throw new IllegalArgumentException("El horario ya no está disponible. Consultá la agenda nuevamente.");
                }
                int id;
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "INSERT INTO turno (id_paciente,id_agenda,id_tratamiento,fecha,hora,estado) "
                        + "VALUES (?,?,?,?,?,'CONFIRMADO')", Statement.RETURN_GENERATED_KEYS)) {
                    sentencia.setInt(1, turno.getPaciente().getIdPaciente());
                    sentencia.setInt(2, turno.getAgenda().getIdAgenda());
                    sentencia.setInt(3, turno.getTratamientoPrevisto().getIdTratamiento());
                    sentencia.setDate(4, Date.valueOf(fecha));
                    sentencia.setTime(5, Time.valueOf(hora));
                    sentencia.executeUpdate();
                    try (ResultSet claves = sentencia.getGeneratedKeys()) {
                        if (!claves.next()) { throw new SQLException("No se obtuvo el identificador del turno."); }
                        id = claves.getInt(1);
                    }
                }
                conexion.commit();
                return new Turno(id, turno.getPaciente(), turno.getAgenda(), turno.getTratamientoPrevisto(),
                        turno.getFechaHora(), "CONFIRMADO");
            } catch (SQLException | IllegalArgumentException error) {
                conexion.rollback();
                throw error;
            }
        }
    }

    public List<Turno> consultarTurnos(Agenda agenda, LocalDate fecha) throws SQLException {
        return consultarAgenda(agenda.getProfesional(), fecha, null);
    }

    public List<Turno> consultarAgenda(Profesional profesional, LocalDate fecha, LocalTime hora)
            throws SQLException {
        String sql = consultaBase() + " WHERE t.fecha=?"
                + (profesional == null ? "" : " AND pr.id_profesional=?")
                + (hora == null ? "" : " AND t.hora=?")
                + " ORDER BY t.hora,pr.apellido,pr.nombre,t.id_turno";
        List<Turno> turnos = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            int parametro = 1;
            sentencia.setDate(parametro++, Date.valueOf(fecha));
            if (profesional != null) { sentencia.setInt(parametro++, profesional.getIdProfesional()); }
            if (hora != null) { sentencia.setTime(parametro, Time.valueOf(hora)); }
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) { turnos.add(desdeFila(filas)); }
            }
        }
        return turnos;
    }

    public Turno buscarPorId(int idTurno) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(consultaBase() + " WHERE t.id_turno=?")) {
            sentencia.setInt(1,idTurno);
            try (ResultSet filas = sentencia.executeQuery()) {
                if (!filas.next()) { throw new IllegalArgumentException("El turno ya no existe."); }
                return desdeFila(filas);
            }
        }
    }

    private String consultaBase() {
        return "SELECT t.*,p.dni,p.nombre,p.apellido,p.telefono,p.email,"
                + "a.id_profesional,pr.nombre AS profesional_nombre,pr.apellido AS profesional_apellido,"
                + "pr.matricula,pr.especialidad,tr.nombre AS tratamiento_nombre,tr.descripcion,tr.precio "
                + "FROM turno t JOIN paciente p ON p.id_paciente=t.id_paciente "
                + "JOIN agenda a ON a.id_agenda=t.id_agenda "
                + "JOIN profesional pr ON pr.id_profesional=a.id_profesional "
                + "LEFT JOIN tratamiento tr ON tr.id_tratamiento=t.id_tratamiento";
    }

    private Turno desdeFila(ResultSet filas) throws SQLException {
        Paciente paciente = new Paciente(filas.getInt("id_paciente"),filas.getString("dni"),
                filas.getString("nombre"),filas.getString("apellido"),filas.getString("telefono"),filas.getString("email"));
        Profesional profesional = new Profesional(filas.getInt("id_profesional"),
                filas.getString("profesional_nombre"),filas.getString("profesional_apellido"),
                filas.getString("matricula"),filas.getString("especialidad"));
        Agenda agenda = new Agenda(filas.getInt("id_agenda"),profesional);
        int idTratamiento = filas.getInt("id_tratamiento");
        Tratamiento tratamiento = idTratamiento == 0 ? null : new Tratamiento(idTratamiento,
                filas.getString("tratamiento_nombre"),filas.getString("descripcion"),filas.getBigDecimal("precio"));
        LocalDateTime fechaHora = filas.getDate("fecha").toLocalDate().atTime(filas.getTime("hora").toLocalTime());
        return new Turno(filas.getInt("id_turno"),paciente,agenda,tratamiento,fechaHora,filas.getString("estado"));
    }
}
