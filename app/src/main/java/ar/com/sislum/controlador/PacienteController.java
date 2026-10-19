package ar.com.sislum.controlador;

import ar.com.sislum.modelo.Paciente;
import ar.com.sislum.persistencia.PacienteDAO;
import java.sql.SQLException;
import java.util.List;

public final class PacienteController {
    private final PacienteDAO pacienteDAO = new PacienteDAO();

    public Paciente registrar(String dni, String nombre, String apellido, String telefono, String email)
            throws SQLException {
        return pacienteDAO.guardar(new Paciente(dni, nombre, apellido, telefono, email));
    }

    public List<Paciente> buscar(String consulta) throws SQLException {
        return pacienteDAO.buscar(consulta == null ? "" : consulta);
    }
}
