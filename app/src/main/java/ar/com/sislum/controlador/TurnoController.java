package ar.com.sislum.controlador;

import ar.com.sislum.modelo.*;
import ar.com.sislum.persistencia.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TurnoController {
    private final CatalogoDAO catalogoDAO = new CatalogoDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final PacienteController pacienteController = new PacienteController();

    public List<Paciente> buscarPaciente(String consulta) throws SQLException {
        return pacienteController.buscar(consulta);
    }

    public List<Profesional> consultarProfesionales() throws SQLException {
        return catalogoDAO.consultarProfesionales();
    }

    public List<Tratamiento> consultarTratamientos() throws SQLException {
        return catalogoDAO.consultarTratamientos();
    }

    public List<LocalTime> consultarDisponibilidad(Profesional profesional, LocalDate fecha) throws SQLException {
        validarAgenda(profesional, fecha);
        Agenda agenda = catalogoDAO.consultarAgenda(profesional);
        return agenda.consultarDisponibilidad(fecha, turnoDAO.buscarOcupados(agenda.getIdAgenda(), fecha));
    }

    public List<Turno> consultarTurnos(Profesional profesional, LocalDate fecha) throws SQLException {
        if (profesional == null || fecha == null) {
            throw new IllegalArgumentException("Seleccioná profesional y fecha.");
        }
        return turnoDAO.consultarTurnos(catalogoDAO.consultarAgenda(profesional), fecha);
    }

    public List<Turno> consultarAgenda(Profesional profesional, LocalDate fecha, LocalTime hora)
            throws SQLException {
        if (fecha == null) { throw new IllegalArgumentException("Seleccioná el día de la agenda."); }
        return turnoDAO.consultarAgenda(profesional,fecha,hora);
    }

    public Turno registrarTurno(Paciente paciente, Profesional profesional, Tratamiento tratamiento,
            LocalDate fecha, LocalTime hora) throws SQLException {
        validarAgenda(profesional, fecha);
        if (paciente == null || paciente.getIdPaciente() <= 0) {
            throw new IllegalArgumentException("Buscá y seleccioná un paciente registrado.");
        }
        if (tratamiento == null || hora == null) {
            throw new IllegalArgumentException("Seleccioná el tratamiento y un horario disponible.");
        }
        Agenda agenda = catalogoDAO.consultarAgenda(profesional);
        Turno turno = new Turno(0, paciente, agenda, tratamiento, fecha.atTime(hora), "CONFIRMADO");
        return turnoDAO.guardar(turno);
    }

    private void validarAgenda(Profesional profesional, LocalDate fecha) {
        if (profesional == null || fecha == null) {
            throw new IllegalArgumentException("Seleccioná profesional y fecha.");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden registrar turnos en fechas pasadas.");
        }
    }
}
