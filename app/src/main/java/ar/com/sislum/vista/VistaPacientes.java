package ar.com.sislum.vista;

import ar.com.sislum.controlador.PacienteController;
import ar.com.sislum.modelo.Paciente;
import java.awt.Component;
import java.awt.Dialog;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class VistaPacientes extends JPanel {
    private final PacienteController controlador = new PacienteController();
    private boolean buscando;

    public VistaPacientes() {
        initComponents();
        tablaPacientes.setModel(new DefaultTableModel(
                new String[]{"DNI", "Nombre", "Apellido", "Teléfono", "Email"}, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        });
        tablaPacientes.getTableHeader().setReorderingAllowed(false);
        btnBuscar.addActionListener(evento -> consultarPaciente());
        txtBusqueda.addActionListener(evento -> consultarPaciente());
        btnNuevoPaciente.addActionListener(evento -> registrarPaciente(this, paciente -> {
            txtBusqueda.setText(paciente.getDni());
            consultarPaciente();
        }));
    }

    public void consultarPaciente() {
        if (buscando) { return; }
        String criterio = txtBusqueda.getText();
        buscando = true;
        btnBuscar.setEnabled(false);
        btnNuevoPaciente.setEnabled(false);
        new SwingWorker<List<Paciente>, Void>() {
            @Override protected List<Paciente> doInBackground() throws Exception {
                return controlador.buscar(criterio);
            }
            @Override protected void done() {
                buscando = false;
                btnBuscar.setEnabled(true);
                btnNuevoPaciente.setEnabled(true);
                try {
                    List<Paciente> pacientes = get();
                    DefaultTableModel modelo = (DefaultTableModel) tablaPacientes.getModel();
                    modelo.setRowCount(0);
                    for (Paciente paciente : pacientes) {
                        modelo.addRow(new Object[]{paciente.getDni(),paciente.getNombre(),paciente.getApellido(),
                            paciente.getTelefono(),paciente.getEmail()});
                    }
                    lblEstado.setText("Pacientes encontrados: " + pacientes.size());
                } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                catch (ExecutionException error) {
                    lblEstado.setText("No se pudo consultar MySQL.");
                    JOptionPane.showMessageDialog(VistaPacientes.this,
                            "No se pudieron consultar los pacientes. Revisá la conexión con MySQL.");
                }
            }
        }.execute();
    }

    // El mismo formulario se usa desde Pacientes y desde Registrar turno.
    public static void registrarPaciente(Component padre, Consumer<Paciente> alGuardar) {
        JDialog dialogo = new JDialog(SwingUtilities.getWindowAncestor(padre), "Registrar paciente",
                Dialog.ModalityType.APPLICATION_MODAL);
        FormularioPaciente formulario = new FormularioPaciente();
        JPanel contenido = new JPanel(new java.awt.BorderLayout(12, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contenido.add(formulario, java.awt.BorderLayout.CENTER);
        JButton guardar = new JButton("Guardar paciente");
        JButton cancelar = new JButton("Cancelar");
        JPanel botones = new JPanel();
        botones.add(cancelar);
        botones.add(guardar);
        contenido.add(botones, java.awt.BorderLayout.SOUTH);
        cancelar.addActionListener(evento -> dialogo.dispose());
        guardar.addActionListener(evento -> {
            String dni = formulario.getDni();
            String nombre = formulario.getNombre();
            String apellido = formulario.getApellido();
            String telefono = formulario.getTelefono();
            String email = formulario.getEmail();
            guardar.setEnabled(false);
            cancelar.setEnabled(false);
            dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            new SwingWorker<Paciente, Void>() {
                @Override protected Paciente doInBackground() throws Exception {
                    return new PacienteController().registrar(dni,nombre,apellido,telefono,email);
                }
                @Override protected void done() {
                    guardar.setEnabled(true);
                    cancelar.setEnabled(true);
                    dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    try {
                        Paciente paciente = get();
                        dialogo.dispose();
                        alGuardar.accept(paciente);
                    } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException error) {
                        Throwable causa = error.getCause();
                        String mensaje = causa instanceof IllegalArgumentException ? causa.getMessage()
                                : "No se pudo guardar el paciente. Revisá la conexión con MySQL.";
                        JOptionPane.showMessageDialog(dialogo,mensaje,"Revisá los datos",JOptionPane.WARNING_MESSAGE);
                    }
                }
            }.execute();
        });
        dialogo.setContentPane(contenido);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.pack();
        dialogo.setLocationRelativeTo(padre);
        dialogo.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        this.setLayout(new java.awt.BorderLayout(12, 12));
        lblTitulo = new javax.swing.JLabel();
        lblTitulo.setText("Pacientes");
        this.add(lblTitulo, "North");
        panelListado = new javax.swing.JPanel();
        panelListado.setLayout(new java.awt.BorderLayout(8, 8));
        panelBusqueda = new javax.swing.JPanel();
        panelBusqueda.setLayout(new java.awt.FlowLayout(0, 8, 8));
        lblBusqueda = new javax.swing.JLabel();
        lblBusqueda.setText("Nombre, apellido o DNI");
        panelBusqueda.add(lblBusqueda);
        txtBusqueda = new javax.swing.JTextField();
        txtBusqueda.setColumns(22);
        panelBusqueda.add(txtBusqueda);
        btnBuscar = new javax.swing.JButton();
        btnBuscar.setText("Buscar");
        panelBusqueda.add(btnBuscar);
        btnNuevoPaciente = new javax.swing.JButton();
        btnNuevoPaciente.setText("Nuevo paciente");
        panelBusqueda.add(btnNuevoPaciente);
        panelListado.add(panelBusqueda, "North");
        scrollPacientes = new javax.swing.JScrollPane();
        tablaPacientes = new javax.swing.JTable();
        tablaPacientes.setRowHeight(28);
        scrollPacientes.setViewportView(tablaPacientes);
        panelListado.add(scrollPacientes, "Center");
        this.add(panelListado, "Center");
        lblEstado = new javax.swing.JLabel();
        lblEstado.setText("Los pacientes se guardan en MySQL.");
        this.add(lblEstado, "South");
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnNuevoPaciente;
    private javax.swing.JLabel lblBusqueda;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel panelBusqueda;
    private javax.swing.JPanel panelListado;
    private javax.swing.JScrollPane scrollPacientes;
    private javax.swing.JTable tablaPacientes;
    private javax.swing.JTextField txtBusqueda;
    // End of variables declaration//GEN-END:variables

}
