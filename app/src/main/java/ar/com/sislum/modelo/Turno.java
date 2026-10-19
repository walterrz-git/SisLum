package ar.com.sislum.modelo;

import java.time.LocalDateTime;

public final class Turno {
    private final int idTurno;
    private final Paciente paciente;
    private final Agenda agenda;
    private final Tratamiento tratamientoPrevisto;
    private final LocalDateTime fechaHora;
    private final String estado;

    public Turno(int idTurno, Paciente paciente, Agenda agenda, Tratamiento tratamientoPrevisto,
            LocalDateTime fechaHora, String estado) {
        if (paciente == null || agenda == null || fechaHora == null || estado == null) {
            throw new IllegalArgumentException("Faltan datos del turno.");
        }
        this.idTurno = idTurno;
        this.paciente = paciente;
        this.agenda = agenda;
        this.tratamientoPrevisto = tratamientoPrevisto;
        this.fechaHora = fechaHora;
        this.estado = estado;
    }

    public int getIdTurno() { return idTurno; }
    public Paciente getPaciente() { return paciente; }
    public Agenda getAgenda() { return agenda; }
    public Tratamiento getTratamientoPrevisto() { return tratamientoPrevisto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
}
