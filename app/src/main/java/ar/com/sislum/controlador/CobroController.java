package ar.com.sislum.controlador;

import ar.com.sislum.modelo.*;
import ar.com.sislum.persistencia.CobroDAO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class CobroController {
    private final CobroDAO cobroDAO = new CobroDAO();
    private final PacienteController pacientes = new PacienteController();

    public List<Paciente> buscarPaciente(String consulta) throws SQLException {
        return pacientes.buscar(consulta);
    }

    public List<Atencion> consultarImporte(Paciente paciente) throws SQLException {
        if (paciente == null || paciente.getIdPaciente() <= 0) {
            throw new IllegalArgumentException("Seleccioná un paciente registrado.");
        }
        return cobroDAO.consultarPendientes(paciente);
    }

    public List<Atencion> consultarImporte(LocalDate fecha, String consulta) throws SQLException {
        if (fecha == null) { throw new IllegalArgumentException("Seleccioná la fecha del turno."); }
        return cobroDAO.consultarPendientes(fecha, consulta == null ? "" : consulta.trim());
    }

    public List<Atencion> consultarPorEstado(LocalDate fecha, String consulta, String estado) throws SQLException {
        if (fecha == null) { throw new IllegalArgumentException("Seleccioná la fecha del turno."); }
        if (!java.util.Set.of("PENDIENTE","PAGADO","TODOS").contains(estado == null ? "" : estado)) {
            throw new IllegalArgumentException("Seleccioná un estado válido.");
        }
        return cobroDAO.consultarPorEstado(fecha,consulta == null ? "" : consulta.trim(),estado);
    }

    public Cobro consultarCobro(int idAtencion) throws SQLException {
        return cobroDAO.buscarPorAtencion(idAtencion);
    }

    public Cobro registrarCobro(Atencion atencion, String medioPago, String observaciones) throws SQLException {
        if (atencion == null) { throw new IllegalArgumentException("Seleccioná una atención pendiente."); }
        Cobro cobro = new Cobro(0,atencion,atencion.getImporteTotal(),medioPago,LocalDate.now(),"PAGADO",observaciones);
        return cobroDAO.guardar(cobro);
    }
}
