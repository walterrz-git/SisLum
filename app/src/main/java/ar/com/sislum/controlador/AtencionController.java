package ar.com.sislum.controlador;

import ar.com.sislum.modelo.*;
import ar.com.sislum.persistencia.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AtencionController {
    private final AtencionDAO atencionDAO = new AtencionDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();

    public List<Profesional> consultarProfesionales() throws SQLException {
        return catalogoDAO.consultarProfesionales();
    }

    public List<Tratamiento> consultarTratamientos() throws SQLException {
        return catalogoDAO.consultarTratamientos();
    }

    public List<Turno> consultarTurnosAsignados(Profesional profesional, LocalDate fecha) throws SQLException {
        if (profesional == null || fecha == null) {
            throw new IllegalArgumentException("Seleccioná profesional y fecha.");
        }
        List<Turno> pendientes = new ArrayList<>();
        for (Turno turno : turnoDAO.consultarAgenda(profesional,fecha,null)) {
            if (turno.getEstado().equals("CONFIRMADO")) { pendientes.add(turno); }
        }
        return pendientes;
    }

    public HistoriaClinica consultarHistoria(Paciente paciente) throws SQLException {
        if (paciente == null) { throw new IllegalArgumentException("Seleccioná un paciente."); }
        return atencionDAO.buscarPorPaciente(paciente);
    }

    public Atencion registrarAtencion(Turno turno, String informe, List<TratamientoRealizado> tratamientos)
            throws SQLException {
        new Atencion(0,0,turno,LocalDate.now(),informe,tratamientos);
        Turno actual = turnoDAO.buscarPorId(turno.getIdTurno());
        if (actual.getPaciente().getIdPaciente() != turno.getPaciente().getIdPaciente()
                || actual.getAgenda().getProfesional().getIdProfesional()
                    != turno.getAgenda().getProfesional().getIdProfesional()) {
            throw new IllegalArgumentException("El turno cambió. Volvé a seleccionarlo.");
        }
        return atencionDAO.guardar(actual,informe,List.copyOf(tratamientos));
    }

    public BigDecimal consultarImportePendiente(Paciente paciente) throws SQLException {
        return atencionDAO.consultarImportePendiente(paciente.getIdPaciente());
    }
}
