package ar.com.sislum.modelo;

import java.util.List;

public final class HistoriaClinica {
    private final int idHistoria;
    private final Paciente paciente;
    private final List<Atencion> atenciones;

    public HistoriaClinica(int idHistoria, Paciente paciente, List<Atencion> atenciones) {
        this.idHistoria = idHistoria;
        this.paciente = paciente;
        this.atenciones = List.copyOf(atenciones);
    }

    public int getIdHistoria() { return idHistoria; }
    public Paciente getPaciente() { return paciente; }
    public List<Atencion> getAtenciones() { return atenciones; }
}
