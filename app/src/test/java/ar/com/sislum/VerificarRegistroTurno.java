package ar.com.sislum;

import ar.com.sislum.controlador.*;
import ar.com.sislum.modelo.*;
import ar.com.sislum.persistencia.*;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;

// Ejecutar sobre la base local de demostración. Elimina únicamente sus registros de prueba.
public final class VerificarRegistroTurno {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) { throw new AssertionError(mensaje); }
    }

    private static void debeRechazar(RunnableConError operacion) throws Exception {
        try { operacion.ejecutar(); }
        catch (IllegalArgumentException esperado) { return; }
        throw new AssertionError("Se aceptó una operación inválida.");
    }

    private interface RunnableConError { void ejecutar() throws Exception; }

    public static void main(String[] args) throws Exception {
        Paciente paciente = null;
        long atencionesAntes = contar("atencion");
        long cobrosAntes = contar("cobro");
        try {
            PacienteController pacientes = new PacienteController();
            TurnoController turnos = new TurnoController();
            List<Profesional> profesionales = turnos.consultarProfesionales();
            List<Tratamiento> tratamientos = turnos.consultarTratamientos();
            comprobar(profesionales.size() >= 3 && tratamientos.size() >= 3, "Faltan datos iniciales.");
            Profesional profesional = profesionales.get(0);
            Tratamiento tratamiento = tratamientos.get(0);
            debeRechazar(() -> pacientes.registrar("abc", "Prueba", "Turnos", "", ""));
            debeRechazar(() -> pacientes.registrar("12345678", "", "Turnos", "", ""));
            String dni = null;
            PacienteDAO pacienteDAO = new PacienteDAO();
            for (int numero = 99000000; numero < 99999999; numero++) {
                if (pacienteDAO.buscarPorDni(String.valueOf(numero)) == null) {
                    dni = String.valueOf(numero); break;
                }
            }
            comprobar(dni != null, "No se encontró un DNI de prueba libre.");
            paciente = pacientes.registrar(dni, "Prueba", "RegistroTurno", "", "");
            final Paciente registrado = paciente;
            debeRechazar(() -> pacientes.registrar(registrado.getDni(), "Otro", "Paciente", "", ""));
            comprobar(new PacienteDAO().buscarPorDni(dni).getIdPaciente() == paciente.getIdPaciente(),
                    "El paciente no persistió.");
            comprobar(pacientes.buscar("RegistroTurno").stream().anyMatch(p -> p.getIdPaciente() == registrado.getIdPaciente()),
                    "La búsqueda no encontró al paciente.");
            comprobar(pacientes.buscar("' OR 1=1 --").isEmpty(), "La búsqueda interpreta SQL.");
            try (Connection c = ConexionBD.obtenerConexion();
                    PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM historia_clinica WHERE id_paciente=?")) {
                s.setInt(1,paciente.getIdPaciente());
                try (ResultSet r = s.executeQuery()) { r.next(); comprobar(r.getInt(1)==1,"Falta la historia clínica."); }
            }
            LocalDate sabado = LocalDate.now().plusWeeks(1).with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            comprobar(turnos.consultarDisponibilidad(profesional,sabado).isEmpty(),"Hay horarios en fin de semana.");
            debeRechazar(() -> turnos.registrarTurno(registrado,profesional,tratamiento,sabado,LocalTime.of(10,0)));
            debeRechazar(() -> turnos.consultarDisponibilidad(profesional,LocalDate.now().minusDays(1)));
            LocalDate fecha = LocalDate.now().plusWeeks(2).with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            List<LocalTime> horarios = turnos.consultarDisponibilidad(profesional,fecha);
            comprobar(horarios.size() >= 2,"No hay horarios de prueba suficientes.");
            LocalTime hora = horarios.get(0);
            debeRechazar(() -> turnos.registrarTurno(null,profesional,tratamiento,fecha,hora));
            debeRechazar(() -> turnos.registrarTurno(registrado,profesional,tratamiento,fecha,LocalTime.of(8,0)));
            debeRechazar(() -> turnos.registrarTurno(registrado,profesional,tratamiento,fecha,LocalTime.of(9,15)));
            Turno guardado = turnos.registrarTurno(paciente,profesional,tratamiento,fecha,hora);
            comprobar(guardado.getIdTurno()>0,"No se generó el turno.");
            comprobar(!new TurnoController().consultarDisponibilidad(profesional,fecha).contains(hora),
                    "Un horario reservado sigue disponible.");
            comprobar(new TurnoController().consultarTurnos(profesional,fecha).stream()
                    .anyMatch(t -> t.getIdTurno()==guardado.getIdTurno()
                        && t.getPaciente().getIdPaciente()==registrado.getIdPaciente()
                        && t.getTratamientoPrevisto().getIdTratamiento()==tratamiento.getIdTratamiento()),
                    "No se recuperó el turno con sus relaciones.");
            debeRechazar(() -> turnos.registrarTurno(registrado,profesional,tratamiento,fecha,hora));

            // Dos conexiones intentan reservar el mismo horario a la vez.
            LocalTime simultanea = horarios.get(1);
            ExecutorService hilos = Executors.newFixedThreadPool(2);
            CountDownLatch inicio = new CountDownLatch(1);
            Callable<Boolean> reserva = () -> {
                inicio.await();
                try { new TurnoController().registrarTurno(registrado,profesional,tratamiento,fecha,simultanea); return true; }
                catch (IllegalArgumentException esperado) { return false; }
            };
            try {
                Future<Boolean> uno = hilos.submit(reserva);
                Future<Boolean> dos = hilos.submit(reserva);
                inicio.countDown();
                comprobar(uno.get(20,TimeUnit.SECONDS) != dos.get(20,TimeUnit.SECONDS),
                        "Se permitió una doble reserva o fallaron ambas.");
            } finally { hilos.shutdownNow(); hilos.awaitTermination(20,TimeUnit.SECONDS); }

            // Un error de integridad no debe consumir el horario ni dejar cambios parciales.
            LocalTime libre = turnos.consultarDisponibilidad(profesional,fecha).get(0);
            Paciente inexistente = new Paciente(Integer.MAX_VALUE,"12345678","No","Existe","","");
            try {
                turnos.registrarTurno(inexistente,profesional,tratamiento,fecha,libre);
                throw new AssertionError("Se aceptó un paciente inexistente.");
            } catch (SQLException esperado) { }
            comprobar(turnos.consultarDisponibilidad(profesional,fecha).contains(libre),"Falló el rollback.");
            comprobar(contar("atencion")==atencionesAntes && contar("cobro")==cobrosAntes,
                    "Se modificaron casos fuera del alcance.");
            System.out.println("OK: alta, historia clínica, duplicados, búsqueda, disponibilidad, persistencia, concurrencia y rollback.");
        } finally {
            if (paciente != null) {
                try (Connection c = ConexionBD.obtenerConexion()) {
                    c.setAutoCommit(false);
                    for (String tabla : new String[]{"turno","historia_clinica","paciente"}) {
                        try (PreparedStatement s = c.prepareStatement("DELETE FROM " + tabla + " WHERE id_paciente=?")) {
                            s.setInt(1,paciente.getIdPaciente()); s.executeUpdate();
                        }
                    }
                    c.commit();
                }
            }
        }
    }

    private static long contar(String tabla) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexion(); Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            r.next(); return r.getLong(1);
        }
    }
}
