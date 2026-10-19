package ar.com.sislum.modelo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class Agenda {
    private final int idAgenda;
    private final Profesional profesional;
    private static final LocalTime APERTURA = LocalTime.of(9, 0);
    private static final LocalTime CIERRE = LocalTime.of(18, 0);
    private static final int MINUTOS_TURNO = 30;

    public Agenda(int idAgenda, Profesional profesional) {
        if (idAgenda <= 0 || profesional == null) {
            throw new IllegalArgumentException("La agenda debe tener un profesional.");
        }
        this.idAgenda = idAgenda;
        this.profesional = profesional;
    }

    // En esta etapa todos atienden de lunes a viernes, de 9 a 18.
    public List<LocalTime> consultarDisponibilidad(LocalDate fecha, List<LocalTime> ocupados) {
        List<LocalTime> disponibles = new ArrayList<>();
        if (fecha == null || fecha.isBefore(LocalDate.now())
                || fecha.getDayOfWeek() == DayOfWeek.SATURDAY
                || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return disponibles;
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (LocalTime hora = APERTURA; hora.isBefore(CIERRE); hora = hora.plusMinutes(MINUTOS_TURNO)) {
            if (fecha.atTime(hora).isAfter(ahora) && !ocupados.contains(hora)) {
                disponibles.add(hora);
            }
        }
        return disponibles;
    }

    public int getIdAgenda() { return idAgenda; }
    public Profesional getProfesional() { return profesional; }
}
