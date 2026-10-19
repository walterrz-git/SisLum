package ar.com.sislum.persistencia;

import ar.com.sislum.modelo.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CobroDAO {
    public List<Atencion> consultarPendientes(Paciente paciente) throws SQLException {
        List<Atencion> pendientes = new ArrayList<>();
        String sql = "SELECT a.id_atencion FROM atencion a "
                + "JOIN historia_clinica h ON h.id_historia=a.id_historia "
                + "WHERE h.id_paciente=? AND NOT EXISTS "
                + "(SELECT 1 FROM cobro c WHERE c.id_atencion=a.id_atencion AND c.estado='PAGADO') "
                + "ORDER BY a.fecha,a.id_atencion";
        try (Connection conexion = ConexionBD.obtenerConexion(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1,paciente.getIdPaciente());
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) { pendientes.add(new AtencionDAO().buscarPorId(filas.getInt(1))); }
            }
        }
        return pendientes;
    }

    public List<Atencion> consultarPendientes(LocalDate fecha, String consulta) throws SQLException {
        return consultarPorEstado(fecha,consulta,"PENDIENTE");
    }

    public List<Atencion> consultarPorEstado(LocalDate fecha, String consulta, String estado) throws SQLException {
        List<Atencion> pendientes = new ArrayList<>();
        String sql = "SELECT a.id_atencion FROM atencion a "
                + "JOIN turno t ON t.id_turno=a.id_turno JOIN paciente p ON p.id_paciente=t.id_paciente "
                + "WHERE t.fecha=? "
                + "AND (p.dni LIKE ? OR p.nombre LIKE ? OR p.apellido LIKE ?) "
                + ("TODOS".equals(estado) ? "" : "AND " + ("PAGADO".equals(estado) ? "" : "NOT ")
                    + "EXISTS (SELECT 1 FROM cobro c WHERE c.id_atencion=a.id_atencion AND c.estado='PAGADO') ")
                + "ORDER BY t.hora,a.id_atencion";
        try (Connection conexion = ConexionBD.obtenerConexion(); PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setDate(1,Date.valueOf(fecha));
            String filtro="%"+consulta+"%";
            for (int indice=2; indice<=4; indice++) { sentencia.setString(indice,filtro); }
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) { pendientes.add(new AtencionDAO().buscarPorId(filas.getInt(1))); }
            }
        }
        return pendientes;
    }

    public Cobro guardar(Cobro solicitado) throws SQLException {
        int idAtencion = solicitado.getAtencion().getIdAtencion();
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                // Dos ventanas no pueden cobrar la misma atención al mismo tiempo.
                try (PreparedStatement bloqueo = conexion.prepareStatement(
                        "SELECT h.id_paciente,t.estado FROM atencion a "
                        + "JOIN historia_clinica h ON h.id_historia=a.id_historia "
                        + "JOIN turno t ON t.id_turno=a.id_turno WHERE a.id_atencion=? FOR UPDATE")) {
                    bloqueo.setInt(1,idAtencion);
                    try (ResultSet fila = bloqueo.executeQuery()) {
                        if (!fila.next()) { throw new IllegalArgumentException("La atención ya no existe."); }
                        if (!"ATENDIDO".equals(fila.getString("estado"))
                                || fila.getInt("id_paciente") != solicitado.getAtencion().getTurno().getPaciente().getIdPaciente()) {
                            throw new IllegalArgumentException("Los datos de la atención cambiaron. Actualizá los pendientes.");
                        }
                    }
                }
                int idCobro = 0;
                try (PreparedStatement existente = conexion.prepareStatement(
                        "SELECT id_cobro,estado FROM cobro WHERE id_atencion=? FOR UPDATE")) {
                    existente.setInt(1,idAtencion);
                    try (ResultSet fila = existente.executeQuery()) {
                        if (fila.next()) {
                            if ("PAGADO".equals(fila.getString("estado"))) {
                                throw new IllegalArgumentException("La atención ya está cobrada. Actualizá los pendientes.");
                            }
                            idCobro = fila.getInt("id_cobro");
                        }
                    }
                }
                BigDecimal total = new BigDecimal("0.00");
                int cantidad = 0;
                try (PreparedStatement detalles = conexion.prepareStatement(
                        "SELECT importe FROM tratamiento_realizado WHERE id_atencion=? ORDER BY id_detalle FOR SHARE")) {
                    detalles.setInt(1,idAtencion);
                    try (ResultSet filas = detalles.executeQuery()) {
                        while (filas.next()) { total = total.add(filas.getBigDecimal(1)); cantidad++; }
                    }
                }
                if (cantidad == 0) { throw new IllegalArgumentException("La atención no tiene tratamientos para cobrar."); }
                if (total.compareTo(solicitado.getImporteTotal()) != 0) {
                    throw new IllegalArgumentException("El importe cambió. Actualizá los pendientes y revisá el total.");
                }
                LocalDate fecha = LocalDate.now();
                if (idCobro == 0) {
                    try (PreparedStatement sentencia = conexion.prepareStatement(
                            "INSERT INTO cobro (id_atencion,fecha,importe_total,medio_pago,estado,observaciones) "
                            + "VALUES (?,?,?,?,'PAGADO',?)",Statement.RETURN_GENERATED_KEYS)) {
                        sentencia.setInt(1,idAtencion);
                        sentencia.setDate(2,Date.valueOf(fecha));
                        sentencia.setBigDecimal(3,total);
                        sentencia.setString(4,solicitado.getMedioPago());
                        sentencia.setString(5,solicitado.getObservaciones());
                        sentencia.executeUpdate();
                        try (ResultSet claves = sentencia.getGeneratedKeys()) {
                            if (!claves.next()) { throw new SQLException("No se obtuvo el identificador del cobro."); }
                            idCobro = claves.getInt(1);
                        }
                    }
                } else {
                    // Se completa el registro si ya existía como PENDIENTE.
                    try (PreparedStatement sentencia = conexion.prepareStatement(
                            "UPDATE cobro SET fecha=?,importe_total=?,medio_pago=?,estado='PAGADO',observaciones=? WHERE id_cobro=?")) {
                        sentencia.setDate(1,Date.valueOf(fecha));
                        sentencia.setBigDecimal(2,total);
                        sentencia.setString(3,solicitado.getMedioPago());
                        sentencia.setString(4,solicitado.getObservaciones());
                        sentencia.setInt(5,idCobro);
                        sentencia.executeUpdate();
                    }
                }
                conexion.commit();
                return new Cobro(idCobro,solicitado.getAtencion(),total,solicitado.getMedioPago(),fecha,
                        "PAGADO",solicitado.getObservaciones());
            } catch (SQLException | RuntimeException error) {
                conexion.rollback();
                if (error instanceof SQLException sql && sql.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("La atención ya tiene un cobro registrado.",error);
                }
                throw error;
            }
        }
    }

    public Cobro buscarPorAtencion(int idAtencion) throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement("SELECT * FROM cobro WHERE id_atencion=?")) {
            sentencia.setInt(1,idAtencion);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) { return null; }
                Atencion atencion = new AtencionDAO().buscarPorId(idAtencion);
                return new Cobro(fila.getInt("id_cobro"),atencion,fila.getBigDecimal("importe_total"),
                        fila.getString("medio_pago"),fila.getDate("fecha").toLocalDate(),
                        fila.getString("estado"),fila.getString("observaciones"));
            }
        }
    }
}
