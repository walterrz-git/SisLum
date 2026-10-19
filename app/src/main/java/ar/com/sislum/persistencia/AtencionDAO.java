package ar.com.sislum.persistencia;

import ar.com.sislum.modelo.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AtencionDAO {
    public Atencion guardar(Turno turno, String informe, List<TratamientoRealizado> solicitados)
            throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                int idPaciente;
                int idProfesional;
                // El turno se bloquea hasta terminar de guardar toda la atención.
                try (PreparedStatement bloqueo = conexion.prepareStatement(
                        "SELECT t.id_paciente,t.estado,a.id_profesional FROM turno t "
                        + "JOIN agenda a ON a.id_agenda=t.id_agenda WHERE t.id_turno=? FOR UPDATE")) {
                    bloqueo.setInt(1,turno.getIdTurno());
                    try (ResultSet fila = bloqueo.executeQuery()) {
                        if (!fila.next() || !"CONFIRMADO".equals(fila.getString("estado"))) {
                            throw new IllegalArgumentException("El turno ya fue atendido o no está confirmado. Actualizá la lista.");
                        }
                        idPaciente = fila.getInt("id_paciente");
                        idProfesional = fila.getInt("id_profesional");
                        if (idPaciente != turno.getPaciente().getIdPaciente()
                                || idProfesional != turno.getAgenda().getProfesional().getIdProfesional()) {
                            throw new IllegalArgumentException("Los datos del turno cambiaron. Volvé a seleccionarlo.");
                        }
                    }
                }
                int idHistoria = obtenerHistoria(conexion,idPaciente);
                List<TratamientoRealizado> detalles = new ArrayList<>();
                for (TratamientoRealizado solicitado : solicitados) {
                    // El precio vigente se guarda como importe histórico del tratamiento realizado.
                    try (PreparedStatement precio = conexion.prepareStatement(
                            "SELECT * FROM tratamiento WHERE id_tratamiento=? FOR SHARE")) {
                        precio.setInt(1,solicitado.getTratamiento().getIdTratamiento());
                        try (ResultSet fila = precio.executeQuery()) {
                            if (!fila.next()) { throw new IllegalArgumentException("Un tratamiento ya no está disponible."); }
                            Tratamiento tratamiento = new Tratamiento(fila.getInt("id_tratamiento"),
                                    fila.getString("nombre"),fila.getString("descripcion"),fila.getBigDecimal("precio"));
                            detalles.add(new TratamientoRealizado(0,tratamiento,tratamiento.getPrecio(),
                                    solicitado.getObservaciones()));
                        }
                    }
                }
                Atencion nueva = new Atencion(0,idHistoria,turno,LocalDate.now(),informe,detalles);
                int idAtencion;
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "INSERT INTO atencion (id_turno,id_historia,id_profesional,fecha,informe) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    sentencia.setInt(1,turno.getIdTurno());
                    sentencia.setInt(2,idHistoria);
                    sentencia.setInt(3,idProfesional);
                    sentencia.setDate(4,Date.valueOf(nueva.getFecha()));
                    sentencia.setString(5,nueva.getInforme());
                    sentencia.executeUpdate();
                    try (ResultSet claves = sentencia.getGeneratedKeys()) {
                        if (!claves.next()) { throw new SQLException("No se obtuvo el identificador de la atención."); }
                        idAtencion = claves.getInt(1);
                    }
                }
                try (PreparedStatement detalle = conexion.prepareStatement(
                        "INSERT INTO tratamiento_realizado (id_atencion,id_tratamiento,importe,observaciones) VALUES (?,?,?,?)")) {
                    for (TratamientoRealizado realizado : detalles) {
                        detalle.setInt(1,idAtencion);
                        detalle.setInt(2,realizado.getTratamiento().getIdTratamiento());
                        detalle.setBigDecimal(3,realizado.getImporte());
                        detalle.setString(4,realizado.getObservaciones());
                        detalle.addBatch();
                    }
                    detalle.executeBatch();
                }
                try (PreparedStatement estado = conexion.prepareStatement(
                        "UPDATE turno SET estado='ATENDIDO' WHERE id_turno=?")) {
                    estado.setInt(1,turno.getIdTurno());
                    estado.executeUpdate();
                }
                conexion.commit();
                Turno atendido = new Turno(turno.getIdTurno(),turno.getPaciente(),turno.getAgenda(),
                        turno.getTratamientoPrevisto(),turno.getFechaHora(),"ATENDIDO");
                return new Atencion(idAtencion,idHistoria,atendido,nueva.getFecha(),informe,detalles);
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                throw error;
            }
        }
    }

    private int obtenerHistoria(Connection conexion, int idPaciente) throws SQLException {
        try (PreparedStatement crear = conexion.prepareStatement(
                "INSERT INTO historia_clinica (id_paciente) VALUES (?) "
                + "ON DUPLICATE KEY UPDATE id_historia=id_historia")) {
            crear.setInt(1,idPaciente);
            crear.executeUpdate();
        }
        try (PreparedStatement buscar = conexion.prepareStatement(
                "SELECT id_historia FROM historia_clinica WHERE id_paciente=?")) {
            buscar.setInt(1,idPaciente);
            try (ResultSet fila = buscar.executeQuery()) {
                if (!fila.next()) { throw new SQLException("No se pudo obtener la historia clínica."); }
                return fila.getInt(1);
            }
        }
    }

    public HistoriaClinica buscarPorPaciente(Paciente paciente) throws SQLException {
        List<Atencion> atenciones = new ArrayList<>();
        int idHistoria = 0;
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            try (PreparedStatement historia = conexion.prepareStatement(
                    "SELECT id_historia FROM historia_clinica WHERE id_paciente=?")) {
                historia.setInt(1,paciente.getIdPaciente());
                try (ResultSet fila = historia.executeQuery()) { if (fila.next()) { idHistoria = fila.getInt(1); } }
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT * FROM atencion WHERE id_historia=? ORDER BY fecha DESC,id_atencion DESC")) {
                sentencia.setInt(1,idHistoria);
                try (ResultSet filas = sentencia.executeQuery()) {
                    while (filas.next()) {
                        int id = filas.getInt("id_atencion");
                        Turno turno = new TurnoDAO().buscarPorId(filas.getInt("id_turno"));
                        atenciones.add(new Atencion(id,idHistoria,turno,filas.getDate("fecha").toLocalDate(),
                                filas.getString("informe"),consultarDetalles(conexion,id)));
                    }
                }
            }
        }
        return new HistoriaClinica(idHistoria,paciente,atenciones);
    }

    public Atencion buscarPorId(int idAtencion) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM atencion WHERE id_atencion=?")) {
            sentencia.setInt(1,idAtencion);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) { throw new IllegalArgumentException("La atención ya no existe."); }
                Turno turno = new TurnoDAO().buscarPorId(fila.getInt("id_turno"));
                return new Atencion(idAtencion,fila.getInt("id_historia"),turno,
                        fila.getDate("fecha").toLocalDate(),fila.getString("informe"),
                        consultarDetalles(conexion,idAtencion));
            }
        }
    }

    private List<TratamientoRealizado> consultarDetalles(Connection conexion, int idAtencion) throws SQLException {
        List<TratamientoRealizado> detalles = new ArrayList<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT d.*,t.nombre,t.descripcion,t.precio FROM tratamiento_realizado d "
                + "JOIN tratamiento t ON t.id_tratamiento=d.id_tratamiento WHERE d.id_atencion=? ORDER BY d.id_detalle")) {
            sentencia.setInt(1,idAtencion);
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    Tratamiento tratamiento = new Tratamiento(filas.getInt("id_tratamiento"),filas.getString("nombre"),
                            filas.getString("descripcion"),filas.getBigDecimal("precio"));
                    detalles.add(new TratamientoRealizado(filas.getInt("id_detalle"),tratamiento,
                            filas.getBigDecimal("importe"),filas.getString("observaciones")));
                }
            }
        }
        return detalles;
    }

    public BigDecimal consultarImportePendiente(int idPaciente) throws SQLException {
        String sql = "SELECT COALESCE(SUM(d.importe),0) FROM tratamiento_realizado d "
                + "JOIN atencion a ON a.id_atencion=d.id_atencion "
                + "JOIN historia_clinica h ON h.id_historia=a.id_historia WHERE h.id_paciente=? "
                + "AND NOT EXISTS (SELECT 1 FROM cobro c WHERE c.id_atencion=a.id_atencion AND c.estado='PAGADO')";
        try (Connection conexion = ConexionBD.obtenerConexion(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1,idPaciente);
            try (ResultSet fila = sentencia.executeQuery()) { fila.next(); return fila.getBigDecimal(1); }
        }
    }
}
