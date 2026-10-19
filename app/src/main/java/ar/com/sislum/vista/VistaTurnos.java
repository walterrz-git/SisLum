package ar.com.sislum.vista;

import ar.com.sislum.controlador.TurnoController;
import ar.com.sislum.modelo.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class VistaTurnos extends JPanel {
    private final TurnoController controlador = new TurnoController();
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private List<Paciente> pacientesEncontrados = List.of();
    private Paciente pacienteSeleccionado;
    private Runnable alCancelar = () -> { };
    private boolean ocupado;
    private boolean catalogosCargados;
    private boolean actualizarHorariosPendiente;

    public VistaTurnos() {
        initComponents();
        prepararVista();
        conectarAcciones();
    }

    private void prepararVista() {
        lblTitulo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 23));
        scrollPacientes.setPreferredSize(new java.awt.Dimension(750, 65));
        tablaPacientes.setModel(modeloTabla("DNI", "Nombre", "Apellido", "Teléfono", "Email"));
        tablaPacientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPacientes.getTableHeader().setReorderingAllowed(false);
        for (JTextField campo : new JTextField[]{txtDni,txtNombre,txtApellido,txtTelefono,txtEmail}) {
            campo.setEditable(false);
        }
        selectorFecha.getSettings().setVetoPolicy(fecha -> !fecha.isBefore(LocalDate.now()));
        comboHorario.setPreferredSize(new java.awt.Dimension(100, 28));
        btnConfirmar.setEnabled(false);
        java.awt.Color fondo = new java.awt.Color(250,248,245);
        setBackground(fondo);
        for (JPanel panel : new JPanel[]{panelBusqueda,panelDatos,panelSeleccion}) {
            panel.setBackground(java.awt.Color.WHITE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new java.awt.Color(221,211,202)),
                    BorderFactory.createEmptyBorder(14,14,14,14)));
        }
        for (JPanel panel : new JPanel[]{panelFormulario,panelCampos,panelOpciones,
                panelAccionesPaciente,panelDisponibilidad,panelPie,panelBotones}) { panel.setOpaque(false); }

    }

    private DefaultTableModel modeloTabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }

    private void conectarAcciones() {
        btnBuscar.addActionListener(evento -> buscarPaciente());
        txtBusqueda.addActionListener(evento -> buscarPaciente());
        btnNuevoPaciente.addActionListener(evento -> VistaPacientes.registrarPaciente(this,
                this::mostrarPacienteCreado));
        tablaPacientes.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                int fila = tablaPacientes.getSelectedRow();
                seleccionarPaciente(fila >= 0 && fila < pacientesEncontrados.size() ? pacientesEncontrados.get(fila) : null);
            }
        });
        btnConfirmar.addActionListener(evento -> registrarTurno());
        btnCancelar.addActionListener(evento -> { limpiar(); alCancelar.run(); });
        comboProfesional.addActionListener(evento -> actualizarDisponibilidad());
        comboTratamiento.addActionListener(evento -> actualizarConfirmacion());
        comboHorario.addActionListener(evento -> actualizarConfirmacion());
        selectorFecha.addDateChangeListener(evento -> actualizarDisponibilidad());
    }

    public void cargarDatos() {
        if (ocupado) { return; }
        if (catalogosCargados) { actualizarDisponibilidad(); return; }
        ejecutar(() -> {
            List<Catalogo> datos = new ArrayList<>();
            datos.addAll(controlador.consultarProfesionales());
            datos.addAll(controlador.consultarTratamientos());
            return datos;
        }, datos -> {
            comboProfesional.removeAllItems();
            comboTratamiento.removeAllItems();
            for (Catalogo dato : datos) {
                if (dato instanceof Profesional) { comboProfesional.addItem(dato); }
                else if (dato instanceof Tratamiento) { comboTratamiento.addItem(dato); }
            }
            catalogosCargados = comboProfesional.getItemCount() > 0 && comboTratamiento.getItemCount() > 0;
            lblMensaje.setText(catalogosCargados ? "Buscá un paciente y elegí la fecha del turno."
                    : "Faltan profesionales o tratamientos. Ejecutá preparar-base.command.");
            if (catalogosCargados) { actualizarDisponibilidad(); }
        });
    }

    public void buscarPaciente() {
        if (ocupado) { return; }
        if (!catalogosCargados) { cargarDatos(); return; }
        String consulta = txtBusqueda.getText();
        ejecutar(() -> controlador.buscarPaciente(consulta), pacientes -> {
            seleccionarPaciente(null);
            tablaPacientes.clearSelection();
            pacientesEncontrados = pacientes;
            DefaultTableModel modelo = (DefaultTableModel) tablaPacientes.getModel();
            modelo.setRowCount(0);
            for (Paciente paciente : pacientes) {
                modelo.addRow(new Object[]{paciente.getDni(), paciente.getNombre(), paciente.getApellido(),
                    paciente.getTelefono(), paciente.getEmail()});
            }
            if (pacientes.size() == 1) { tablaPacientes.setRowSelectionInterval(0, 0); }
            lblMensaje.setText(pacientes.isEmpty() ? "No hay coincidencias. Podés dar de alta al paciente."
                    : "Seleccioná el paciente de la lista.");
        });
    }

    private void mostrarPacienteCreado(Paciente paciente) {
        tablaPacientes.clearSelection();
        pacientesEncontrados = List.of(paciente);
        DefaultTableModel modelo = (DefaultTableModel) tablaPacientes.getModel();
        modelo.setRowCount(0);
        modelo.addRow(new Object[]{paciente.getDni(),paciente.getNombre(),paciente.getApellido(),
            paciente.getTelefono(),paciente.getEmail()});
        tablaPacientes.setRowSelectionInterval(0, 0);
        lblMensaje.setText("Paciente registrado y seleccionado. Podés continuar con el turno.");
    }

    private void seleccionarPaciente(Paciente paciente) {
        pacienteSeleccionado = paciente;
        txtDni.setText(paciente == null ? "" : paciente.getDni());
        txtNombre.setText(paciente == null ? "" : paciente.getNombre());
        txtApellido.setText(paciente == null ? "" : paciente.getApellido());
        txtTelefono.setText(paciente == null ? "" : paciente.getTelefono());
        txtEmail.setText(paciente == null ? "" : paciente.getEmail());
        actualizarConfirmacion();
    }

    public void consultarAgenda() {
        if (ocupado) { return; }
        if (!catalogosCargados) { cargarDatos(); return; }
        try {
            Profesional profesional = (Profesional) comboProfesional.getSelectedItem();
            LocalDate fecha = leerFecha();
            invalidarAgenda();
            actualizarHorariosPendiente = false;
            lblMensaje.setText("Buscando horarios disponibles...");
            ejecutar(() -> controlador.consultarDisponibilidad(profesional, fecha), horarios -> {
                Profesional actual = (Profesional) comboProfesional.getSelectedItem();
                if (actual == null || actual.getIdProfesional() != profesional.getIdProfesional()
                        || !fecha.equals(selectorFecha.getDate())) {
                    actualizarHorariosPendiente = true;
                    return;
                }
                for (LocalTime hora : horarios) { comboHorario.addItem(hora); }
                lblMensaje.setText(horarios.isEmpty() ? "No hay horarios disponibles para esa fecha."
                        : "Seleccioná un horario para confirmar el turno.");
            });
        } catch (IllegalArgumentException error) { mostrarError(error); }
    }

    public void setAlCancelar(Runnable accion) { alCancelar = accion; }

    public void registrarTurno() {
        if (ocupado) { return; }
        try {
            Paciente paciente = pacienteSeleccionado;
            Profesional profesional = (Profesional) comboProfesional.getSelectedItem();
            Tratamiento tratamiento = (Tratamiento) comboTratamiento.getSelectedItem();
            LocalDate fecha = leerFecha();
            LocalTime hora = (LocalTime) comboHorario.getSelectedItem();
            if (paciente == null || profesional == null || tratamiento == null || hora == null) {
                throw new IllegalArgumentException("Seleccioná paciente, profesional, tratamiento y horario.");
            }
            String resumen = paciente.getNombre() + " " + paciente.getApellido() + "\n"
                    + profesional + "\n" + tratamiento + "\n" + fecha.format(formatoFecha) + " · " + hora;
            if (JOptionPane.showConfirmDialog(this, resumen, "Confirmar turno",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) { return; }
            ejecutar(() -> controlador.registrarTurno(paciente, profesional, tratamiento, fecha, hora), turno -> {
                // El guardado terminó. La consulta siguiente solo actualiza la pantalla.
                JOptionPane.showMessageDialog(this, "Turno N.º " + turno.getIdTurno() + " registrado correctamente.");
                consultarAgenda();
            });
        } catch (IllegalArgumentException error) { mostrarError(error); }
    }

    private LocalDate leerFecha() {
        return selectorFecha.leerFecha();
    }

    private void actualizarDisponibilidad() {
        invalidarAgenda();
        if (!catalogosCargados) { return; }
        if (ocupado) { actualizarHorariosPendiente = true; return; }
        consultarAgenda();
    }

    private void invalidarAgenda() {
        comboHorario.removeAllItems();
        actualizarConfirmacion();
    }

    private void limpiar() {
        txtBusqueda.setText("");
        seleccionarPaciente(null);
        ((DefaultTableModel) tablaPacientes.getModel()).setRowCount(0);
        pacientesEncontrados = List.of();
        invalidarAgenda();
        lblMensaje.setText("Buscá un paciente y elegí la fecha del turno.");
    }

    private void actualizarConfirmacion() {
        btnConfirmar.setEnabled(!ocupado && pacienteSeleccionado != null
                && comboHorario.getSelectedItem() != null && comboTratamiento.getSelectedItem() != null);
    }

    // Las consultas se hacen en segundo plano para no bloquear la ventana.
    private <T> void ejecutar(Callable<T> operacion, Consumer<T> alTerminar) {
        cambiarOcupado(true);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return operacion.call(); }
            @Override protected void done() {
                cambiarOcupado(false);
                try { alTerminar.accept(get()); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                catch (ExecutionException error) {
                    actualizarHorariosPendiente = false;
                    invalidarAgenda();
                    mostrarError(error.getCause());
                } finally {
                    if (actualizarHorariosPendiente && !ocupado) {
                        actualizarHorariosPendiente = false;
                        consultarAgenda();
                    }
                }
            }
        }.execute();
    }

    private void cambiarOcupado(boolean valor) {
        ocupado = valor;
        for (JComponent componente : new JComponent[]{btnBuscar,btnNuevoPaciente,btnCancelar,
                txtBusqueda,selectorFecha,comboProfesional,comboTratamiento,comboHorario,tablaPacientes}) {
            componente.setEnabled(!valor);
        }
        actualizarConfirmacion();
    }

    private void mostrarError(Throwable error) {
        String mensaje = error instanceof IllegalArgumentException ? error.getMessage()
                : "No se pudo completar la operación con MySQL. Revisá la conexión y la preparación de la base.";
        lblMensaje.setText(mensaje);
        JOptionPane.showMessageDialog(this, mensaje, "Revisá la operación", JOptionPane.WARNING_MESSAGE);
    }


    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints distribucion;
        this.setLayout(new java.awt.BorderLayout(12, 12));
        lblTitulo = new javax.swing.JLabel();
        lblTitulo.setText("01 - Registrar turno (CU001)");
        this.add(lblTitulo, "North");
        panelFormulario = new javax.swing.JPanel();
        panelFormulario.setLayout(new java.awt.GridBagLayout());
        panelBusqueda = new javax.swing.JPanel();
        panelBusqueda.setLayout(new java.awt.BorderLayout(12, 12));
        panelAccionesPaciente = new javax.swing.JPanel();
        panelAccionesPaciente.setLayout(new java.awt.FlowLayout(0, 8, 6));
        lblBuscar = new javax.swing.JLabel();
        lblBuscar.setText("1. Buscar paciente");
        panelAccionesPaciente.add(lblBuscar);
        txtBusqueda = new javax.swing.JTextField();
        txtBusqueda.setColumns(22);
        panelAccionesPaciente.add(txtBusqueda);
        btnBuscar = new javax.swing.JButton();
        btnBuscar.setText("Buscar");
        panelAccionesPaciente.add(btnBuscar);
        btnNuevoPaciente = new javax.swing.JButton();
        btnNuevoPaciente.setText("+ Nuevo paciente");
        panelAccionesPaciente.add(btnNuevoPaciente);
        panelBusqueda.add(panelAccionesPaciente, "North");
        scrollPacientes = new javax.swing.JScrollPane();
        tablaPacientes = new javax.swing.JTable();
        tablaPacientes.setRowHeight(26);
        scrollPacientes.setViewportView(tablaPacientes);
        panelBusqueda.add(scrollPacientes, "Center");
        distribucion = new java.awt.GridBagConstraints();
        distribucion.gridx = 0;
        distribucion.gridy = 0;
        distribucion.weightx = 1.0;
        distribucion.weighty = 1.0;
        distribucion.fill = java.awt.GridBagConstraints.BOTH;
        distribucion.insets = new java.awt.Insets(0, 0, 12, 0);
        panelFormulario.add(panelBusqueda, distribucion);
        panelDatos = new javax.swing.JPanel();
        panelDatos.setLayout(new java.awt.BorderLayout(12, 12));
        lblDatos = new javax.swing.JLabel();
        lblDatos.setText("2. Datos del paciente");
        panelDatos.add(lblDatos, "North");
        panelCampos = new javax.swing.JPanel();
        panelCampos.setPreferredSize(new java.awt.Dimension(0, 64));
        panelCampos.setLayout(new java.awt.GridLayout(2, 5, 12, 8));
        lblDni = new javax.swing.JLabel();
        lblDni.setText("DNI");
        panelCampos.add(lblDni);
        lblNombre = new javax.swing.JLabel();
        lblNombre.setText("Nombre");
        panelCampos.add(lblNombre);
        lblApellido = new javax.swing.JLabel();
        lblApellido.setText("Apellido");
        panelCampos.add(lblApellido);
        lblTelefono = new javax.swing.JLabel();
        lblTelefono.setText("Teléfono");
        panelCampos.add(lblTelefono);
        lblEmail = new javax.swing.JLabel();
        lblEmail.setText("Email");
        panelCampos.add(lblEmail);
        txtDni = new javax.swing.JTextField();
        txtDni.setColumns(10);
        panelCampos.add(txtDni);
        txtNombre = new javax.swing.JTextField();
        txtNombre.setColumns(10);
        panelCampos.add(txtNombre);
        txtApellido = new javax.swing.JTextField();
        txtApellido.setColumns(10);
        panelCampos.add(txtApellido);
        txtTelefono = new javax.swing.JTextField();
        txtTelefono.setColumns(10);
        panelCampos.add(txtTelefono);
        txtEmail = new javax.swing.JTextField();
        txtEmail.setColumns(10);
        panelCampos.add(txtEmail);
        panelDatos.add(panelCampos, "Center");
        distribucion = new java.awt.GridBagConstraints();
        distribucion.gridx = 0;
        distribucion.gridy = 1;
        distribucion.weightx = 1.0;
        distribucion.weighty = 0.0;
        distribucion.fill = java.awt.GridBagConstraints.BOTH;
        distribucion.insets = new java.awt.Insets(0, 0, 12, 0);
        panelFormulario.add(panelDatos, distribucion);
        panelSeleccion = new javax.swing.JPanel();
        panelSeleccion.setLayout(new java.awt.BorderLayout(12, 12));
        lblSeleccion = new javax.swing.JLabel();
        lblSeleccion.setText("3. Seleccionar turno");
        panelSeleccion.add(lblSeleccion, "North");
        panelOpciones = new javax.swing.JPanel();
        panelOpciones.setPreferredSize(new java.awt.Dimension(0, 64));
        panelOpciones.setLayout(new java.awt.GridLayout(2, 3, 12, 8));
        lblProfesional = new javax.swing.JLabel();
        lblProfesional.setText("Profesional");
        panelOpciones.add(lblProfesional);
        lblTratamiento = new javax.swing.JLabel();
        lblTratamiento.setText("Tratamiento previsto");
        panelOpciones.add(lblTratamiento);
        lblFecha = new javax.swing.JLabel();
        lblFecha.setText("Fecha (dd/MM/aaaa)");
        panelOpciones.add(lblFecha);
        comboProfesional = new javax.swing.JComboBox<>();
        panelOpciones.add(comboProfesional);
        comboTratamiento = new javax.swing.JComboBox<>();
        panelOpciones.add(comboTratamiento);
        selectorFecha = new ar.com.sislum.vista.SelectorFecha();
        panelOpciones.add(selectorFecha);
        panelSeleccion.add(panelOpciones, "Center");
        panelDisponibilidad = new javax.swing.JPanel();
        panelDisponibilidad.setLayout(new java.awt.FlowLayout(0, 8, 6));
        lblHorario = new javax.swing.JLabel();
        lblHorario.setText("Horario disponible");
        panelDisponibilidad.add(lblHorario);
        comboHorario = new javax.swing.JComboBox<>();
        panelDisponibilidad.add(comboHorario);
        panelSeleccion.add(panelDisponibilidad, "South");
        distribucion = new java.awt.GridBagConstraints();
        distribucion.gridx = 0;
        distribucion.gridy = 2;
        distribucion.weightx = 1.0;
        distribucion.weighty = 0.0;
        distribucion.fill = java.awt.GridBagConstraints.BOTH;
        distribucion.insets = new java.awt.Insets(0, 0, 0, 0);
        panelFormulario.add(panelSeleccion, distribucion);
        this.add(panelFormulario, "Center");
        panelPie = new javax.swing.JPanel();
        panelPie.setLayout(new java.awt.BorderLayout(12, 12));
        lblMensaje = new javax.swing.JLabel();
        lblMensaje.setText("Seleccioná un paciente y elegí la fecha del turno.");
        panelPie.add(lblMensaje, "Center");
        panelBotones = new javax.swing.JPanel();
        panelBotones.setLayout(new java.awt.FlowLayout(0, 8, 6));
        btnCancelar = new javax.swing.JButton();
        btnCancelar.setText("Cancelar");
        panelBotones.add(btnCancelar);
        btnConfirmar = new javax.swing.JButton();
        btnConfirmar.setText("Confirmar turno");
        panelBotones.add(btnConfirmar);
        panelPie.add(panelBotones, "East");
        this.add(panelPie, "South");
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnConfirmar;
    private javax.swing.JButton btnNuevoPaciente;
    private javax.swing.JComboBox<Object> comboHorario;
    private javax.swing.JComboBox<Object> comboProfesional;
    private javax.swing.JComboBox<Object> comboTratamiento;
    private javax.swing.JLabel lblApellido;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblDatos;
    private javax.swing.JLabel lblDni;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JLabel lblHorario;
    private javax.swing.JLabel lblMensaje;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblProfesional;
    private javax.swing.JLabel lblSeleccion;
    private javax.swing.JLabel lblTelefono;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblTratamiento;
    private javax.swing.JPanel panelAccionesPaciente;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelBusqueda;
    private javax.swing.JPanel panelCampos;
    private javax.swing.JPanel panelDatos;
    private javax.swing.JPanel panelDisponibilidad;
    private javax.swing.JPanel panelFormulario;
    private javax.swing.JPanel panelOpciones;
    private javax.swing.JPanel panelPie;
    private javax.swing.JPanel panelSeleccion;
    private javax.swing.JScrollPane scrollPacientes;
    private javax.swing.JTable tablaPacientes;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextField txtDni;
    private javax.swing.JTextField txtEmail;
    private ar.com.sislum.vista.SelectorFecha selectorFecha;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables

}
