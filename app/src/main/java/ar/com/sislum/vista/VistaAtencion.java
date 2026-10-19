package ar.com.sislum.vista;

import ar.com.sislum.controlador.AtencionController;
import ar.com.sislum.modelo.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class VistaAtencion extends JPanel {
    private final AtencionController controlador=new AtencionController();
    private final DateTimeFormatter formato=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final NumberFormat moneda=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
    private final List<TratamientoRealizado> tratamientos=new ArrayList<>();
    private List<Turno> turnos=List.of();
    private Turno turnoSeleccionado;
    private boolean ocupado;
    private boolean cargado;
    private boolean cambiandoSeleccion;

    public VistaAtencion() {
        initComponents();
        prepararVista();
        conectarAcciones();
    }

    private void prepararVista() {
        lblTitulo.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,23));
        scrollTurnos.setPreferredSize(new java.awt.Dimension(950,110));
        scrollInforme.setPreferredSize(new java.awt.Dimension(450,100));
        txtHistoria.setEditable(false);
        for(JTextArea area:new JTextArea[]{txtHistoria,txtInforme}) { area.setLineWrap(true);area.setWrapStyleWord(true); }
        tablaTurnos.setModel(modelo("Hora","Paciente","DNI","Tratamiento previsto"));
        tablaTratamientos.setModel(modelo("Tratamiento","Detalle","Importe"));
        tablaTurnos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTratamientos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTurnos.getTableHeader().setReorderingAllowed(false);
        tablaTratamientos.getTableHeader().setReorderingAllowed(false);
        actualizarAcciones();
    }

    private DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas,0) {
            @Override public boolean isCellEditable(int fila,int columna) { return false; }
        };
    }

    private void conectarAcciones() {
        btnConsultar.addActionListener(evento -> consultarTurnos());
        btnAgregar.addActionListener(evento -> agregarTratamiento());
        btnQuitar.addActionListener(evento -> quitarTratamiento());
        btnGuardar.addActionListener(evento -> registrarAtencion());
        btnCancelar.addActionListener(evento -> {
            if(descartarCambios()) { limpiarAtencion();tablaTurnos.clearSelection(); }
        });
        tablaTurnos.getSelectionModel().addListSelectionListener(evento -> {
            if(evento.getValueIsAdjusting() || cambiandoSeleccion) { return; }
            seleccionarTurno();
        });
        txtInforme.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarAcciones(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarAcciones(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarAcciones(); }
        });
    }

    public void cargarDatos() {
        if(ocupado) { return; }
        if(cargado) { if(!hayCambios()) { consultarTurnos(); } return; }
        ejecutar(() -> {
            List<Catalogo> datos=new ArrayList<>();
            datos.addAll(controlador.consultarProfesionales());
            datos.addAll(controlador.consultarTratamientos());
            return datos;
        }, datos -> {
            comboProfesional.removeAllItems();comboTratamiento.removeAllItems();
            for(Catalogo dato:datos) {
                if(dato instanceof Profesional) { comboProfesional.addItem(dato); }
                else if(dato instanceof Tratamiento) { comboTratamiento.addItem(dato); }
            }
            cargado=comboProfesional.getItemCount()>0 && comboTratamiento.getItemCount()>0;
            if(cargado) { consultarTurnos(); }
            else { lblEstado.setText("Faltan profesionales o tratamientos en la base."); }
        });
    }

    public void consultarTurnos() {
        if(ocupado) { return; }
        try {
            LocalDate fecha=selectorFecha.leerFecha();
            Profesional profesional=(Profesional)comboProfesional.getSelectedItem();
            if(!descartarCambios()) { return; }
            cambiandoSeleccion=true;
            tablaTurnos.clearSelection();
            limpiarAtencion();
            cambiandoSeleccion=false;
            ((DefaultTableModel)tablaTurnos.getModel()).setRowCount(0);
            turnos=List.of();
            ejecutar(() -> controlador.consultarTurnosAsignados(profesional,fecha), encontrados -> {
                turnos=encontrados;
                DefaultTableModel tabla=(DefaultTableModel)tablaTurnos.getModel();
                for(Turno turno:turnos) {
                    Paciente p=turno.getPaciente();
                    tabla.addRow(new Object[]{turno.getFechaHora().toLocalTime(),p.getNombre()+" "+p.getApellido(),
                        p.getDni(),turno.getTratamientoPrevisto()==null ? "Sin especificar":turno.getTratamientoPrevisto()});
                }
                lblEstado.setText("Turnos pendientes de atención: "+turnos.size());
            });
        } catch(IllegalArgumentException error) { mostrarError(error); }
    }

    private void seleccionarTurno() {
        int fila=tablaTurnos.getSelectedRow();
        Turno elegido=fila>=0 && fila<turnos.size() ? turnos.get(fila):null;
        if(elegido==turnoSeleccionado) { return; }
        if(!descartarCambios()) {
            cambiandoSeleccion=true;
            int anterior=turnos.indexOf(turnoSeleccionado);
            if(anterior>=0) { tablaTurnos.setRowSelectionInterval(anterior,anterior); }
            cambiandoSeleccion=false;
            return;
        }
        limpiarAtencion();
        turnoSeleccionado=elegido;
        if(elegido==null) { return; }
        Paciente paciente=elegido.getPaciente();
        lblPaciente.setText("Paciente: "+paciente.getNombre()+" "+paciente.getApellido()+" · DNI "+paciente.getDni()
                +" · Turno N.º "+elegido.getIdTurno()+" · "+elegido.getFechaHora().toLocalDate().format(formato));
        // La propuesta del turno no se agrega automáticamente como tratamiento realizado.
        if(elegido.getTratamientoPrevisto()!=null) {
            for(int i=0;i<comboTratamiento.getItemCount();i++) {
                if(((Tratamiento)comboTratamiento.getItemAt(i)).getIdTratamiento()==elegido.getTratamientoPrevisto().getIdTratamiento()) {
                    comboTratamiento.setSelectedIndex(i);break;
                }
            }
        }
        ejecutar(() -> controlador.consultarHistoria(paciente), historia -> mostrarHistoria(historia));
    }

    private void mostrarHistoria(HistoriaClinica historia) {
        StringBuilder texto=new StringBuilder();
        if(historia.getAtenciones().isEmpty()) { texto.append("Sin atenciones anteriores registradas."); }
        for(Atencion atencion:historia.getAtenciones()) {
            Profesional medico=atencion.getTurno().getAgenda().getProfesional();
            texto.append(atencion.getFecha().format(formato)).append(" · ")
                    .append(medico.getNombre()).append(" ").append(medico.getApellido()).append("\n")
                    .append(atencion.getInforme()).append("\n");
            for(TratamientoRealizado detalle:atencion.getTratamientos()) {
                texto.append(" • ").append(detalle.getTratamiento().getNombre());
                if(!detalle.getObservaciones().isBlank()) { texto.append(": ").append(detalle.getObservaciones()); }
                texto.append("\n");
            }
            texto.append("\n");
        }
        txtHistoria.setText(texto.toString());txtHistoria.setCaretPosition(0);
        lblEstado.setText("Antecedentes cargados. Registrá los tratamientos y el informe.");
    }

    public void agregarTratamiento() {
        if(ocupado || turnoSeleccionado==null) { return; }
        Tratamiento tratamiento=(Tratamiento)comboTratamiento.getSelectedItem();
        if(tratamiento==null) { return; }
        for(TratamientoRealizado detalle:tratamientos) {
            if(detalle.getTratamiento().getIdTratamiento()==tratamiento.getIdTratamiento()) {
                mostrarError(new IllegalArgumentException("Ese tratamiento ya está agregado."));return;
            }
        }
        try {
            tratamientos.add(new TratamientoRealizado(0,tratamiento,tratamiento.getPrecio(),txtDetalle.getText()));
            txtDetalle.setText("");actualizarTratamientos();
        } catch(IllegalArgumentException error) { mostrarError(error); }
    }

    private void quitarTratamiento() {
        int fila=tablaTratamientos.getSelectedRow();
        if(fila>=0 && fila<tratamientos.size()) { tratamientos.remove(fila);actualizarTratamientos(); }
    }

    private void actualizarTratamientos() {
        DefaultTableModel modelo=(DefaultTableModel)tablaTratamientos.getModel();modelo.setRowCount(0);
        BigDecimal total=new BigDecimal("0.00");
        for(TratamientoRealizado detalle:tratamientos) {
            modelo.addRow(new Object[]{detalle.getTratamiento().getNombre(),detalle.getObservaciones(),moneda.format(detalle.getImporte())});
            total=total.add(detalle.getImporte());
        }
        lblTotal.setText("Importe a cobrar: "+moneda.format(total));actualizarAcciones();
    }

    public void registrarAtencion() {
        if(ocupado) { return; }
        try {
            Turno turno=turnoSeleccionado;
            List<TratamientoRealizado> detalles=List.copyOf(tratamientos);
            String informe=txtInforme.getText();
            new Atencion(0,0,turno,LocalDate.now(),informe,detalles);
            String resumen="Paciente: "+turno.getPaciente().getNombre()+" "+turno.getPaciente().getApellido()
                    +"\nTratamientos realizados: "+detalles.size()+"\n"+lblTotal.getText()+"\n¿Confirmás la atención?";
            if(JOptionPane.showConfirmDialog(this,resumen,"Guardar atención",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) { return; }
            ejecutar(() -> controlador.registrarAtencion(turno,informe,detalles), atencion -> {
                // El cobro se registrará en el caso siguiente.
                cambiandoSeleccion=true;limpiarAtencion();tablaTurnos.clearSelection();cambiandoSeleccion=false;
                lblEstado.setText("Atención N.º "+atencion.getIdAtencion()+" guardada · Pendiente de cobro: "+moneda.format(atencion.getImporteTotal()));
                JOptionPane.showMessageDialog(this,"Atención registrada e historia clínica actualizada.\nImporte pendiente: "+moneda.format(atencion.getImporteTotal()));
                consultarTurnos();
            });
        } catch(IllegalArgumentException error) { mostrarError(error); }
    }

    private boolean hayCambios() { return !tratamientos.isEmpty() || !txtInforme.getText().isBlank() || !txtDetalle.getText().isBlank(); }
    private boolean descartarCambios() {
        return !hayCambios() || JOptionPane.showConfirmDialog(this,"Hay datos sin guardar. ¿Querés descartarlos?",
                "Cambiar atención",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
    }

    private void limpiarAtencion() {
        turnoSeleccionado=null;tratamientos.clear();txtInforme.setText("");txtDetalle.setText("");txtHistoria.setText("");
        lblPaciente.setText("Seleccioná el turno del paciente atendido.");actualizarTratamientos();
    }

    private void actualizarAcciones() {
        boolean editable=!ocupado && turnoSeleccionado!=null;
        for(JComponent c:new JComponent[]{btnAgregar,btnQuitar,comboTratamiento,txtDetalle,txtInforme}) { c.setEnabled(editable); }
        btnGuardar.setEnabled(editable && !tratamientos.isEmpty() && !txtInforme.getText().isBlank());
    }

    private <T> void ejecutar(Callable<T> operacion,Consumer<T> alTerminar) {
        cambiarOcupado(true);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return operacion.call(); }
            @Override protected void done() {
                cambiarOcupado(false);
                try { alTerminar.accept(get()); }
                catch(InterruptedException error) { Thread.currentThread().interrupt(); }
                catch(ExecutionException error) { mostrarError(error.getCause()); }
            }
        }.execute();
    }

    private void cambiarOcupado(boolean valor) {
        ocupado=valor;
        for(JComponent c:new JComponent[]{comboProfesional,selectorFecha,btnConsultar,btnCancelar,tablaTurnos}) { c.setEnabled(!valor); }
        actualizarAcciones();
    }

    private void mostrarError(Throwable error) {
        String mensaje=error instanceof IllegalArgumentException ? error.getMessage()
                : "No se pudo completar la operación. Revisá la conexión con MySQL.";
        lblEstado.setText(mensaje);JOptionPane.showMessageDialog(this,mensaje,"Revisá la atención",JOptionPane.WARNING_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        this.setLayout(new java.awt.BorderLayout(12, 12));
        lblTitulo = new javax.swing.JLabel();
        lblTitulo.setText("02 - Registrar atención (CU007)");
        this.add(lblTitulo, "North");
        panelContenido = new javax.swing.JPanel();
        panelContenido.setLayout(new java.awt.BorderLayout(12, 12));
        panelTurno = new javax.swing.JPanel();
        panelTurno.setLayout(new java.awt.BorderLayout(12, 12));
        panelFiltros = new javax.swing.JPanel();
        panelFiltros.setLayout(new java.awt.FlowLayout(0, 8, 6));
        lblProfesional = new javax.swing.JLabel();
        lblProfesional.setText("Profesional");
        panelFiltros.add(lblProfesional);
        comboProfesional = new javax.swing.JComboBox<>();
        panelFiltros.add(comboProfesional);
        lblFecha = new javax.swing.JLabel();
        lblFecha.setText("Fecha (dd/MM/aaaa)");
        panelFiltros.add(lblFecha);
        selectorFecha = new ar.com.sislum.vista.SelectorFecha();
        panelFiltros.add(selectorFecha);
        btnConsultar = new javax.swing.JButton();
        btnConsultar.setText("Consultar turnos");
        panelFiltros.add(btnConsultar);
        panelTurno.add(panelFiltros, "North");
        scrollTurnos = new javax.swing.JScrollPane();
        tablaTurnos = new javax.swing.JTable();
        tablaTurnos.setRowHeight(26);
        scrollTurnos.setViewportView(tablaTurnos);
        panelTurno.add(scrollTurnos, "Center");
        lblPaciente = new javax.swing.JLabel();
        lblPaciente.setText("Seleccioná el turno del paciente atendido.");
        panelTurno.add(lblPaciente, "South");
        panelContenido.add(panelTurno, "North");
        panelConsulta = new javax.swing.JPanel();
        panelConsulta.setLayout(new java.awt.GridLayout(1, 2, 12, 12));
        panelRegistro = new javax.swing.JPanel();
        panelRegistro.setLayout(new java.awt.BorderLayout(12, 12));
        panelTratamientos = new javax.swing.JPanel();
        panelTratamientos.setLayout(new java.awt.BorderLayout(12, 12));
        lblTratamientos = new javax.swing.JLabel();
        lblTratamientos.setText("Tratamientos realizados");
        panelTratamientos.add(lblTratamientos, "North");
        panelCarga = new javax.swing.JPanel();
        panelCarga.setLayout(new java.awt.GridLayout(2, 2, 12, 12));
        lblTratamiento = new javax.swing.JLabel();
        lblTratamiento.setText("Tratamiento");
        panelCarga.add(lblTratamiento);
        comboTratamiento = new javax.swing.JComboBox<>();
        panelCarga.add(comboTratamiento);
        lblDetalle = new javax.swing.JLabel();
        lblDetalle.setText("Dosis / detalle");
        panelCarga.add(lblDetalle);
        txtDetalle = new javax.swing.JTextField();
        txtDetalle.setColumns(20);
        panelCarga.add(txtDetalle);
        panelTratamientos.add(panelCarga, "Center");
        panelAcciones = new javax.swing.JPanel();
        panelAcciones.setLayout(new java.awt.FlowLayout(0, 8, 6));
        btnAgregar = new javax.swing.JButton();
        btnAgregar.setText("Agregar tratamiento");
        panelAcciones.add(btnAgregar);
        btnQuitar = new javax.swing.JButton();
        btnQuitar.setText("Quitar seleccionado");
        panelAcciones.add(btnQuitar);
        panelTratamientos.add(panelAcciones, "South");
        panelRegistro.add(panelTratamientos, "North");
        scrollTratamientos = new javax.swing.JScrollPane();
        tablaTratamientos = new javax.swing.JTable();
        tablaTratamientos.setRowHeight(26);
        scrollTratamientos.setViewportView(tablaTratamientos);
        panelRegistro.add(scrollTratamientos, "Center");
        panelInforme = new javax.swing.JPanel();
        panelInforme.setLayout(new java.awt.BorderLayout(12, 12));
        lblInforme = new javax.swing.JLabel();
        lblInforme.setText("Informe médico de la atención");
        panelInforme.add(lblInforme, "North");
        scrollInforme = new javax.swing.JScrollPane();
        txtInforme = new javax.swing.JTextArea();
        txtInforme.setRows(5);
        txtInforme.setColumns(24);
        scrollInforme.setViewportView(txtInforme);
        panelInforme.add(scrollInforme, "Center");
        panelRegistro.add(panelInforme, "South");
        panelConsulta.add(panelRegistro);
        panelHistoria = new javax.swing.JPanel();
        panelHistoria.setLayout(new java.awt.BorderLayout(12, 12));
        lblHistoria = new javax.swing.JLabel();
        lblHistoria.setText("Historia clínica · antecedentes");
        panelHistoria.add(lblHistoria, "North");
        scrollHistoria = new javax.swing.JScrollPane();
        txtHistoria = new javax.swing.JTextArea();
        txtHistoria.setRows(5);
        txtHistoria.setColumns(24);
        scrollHistoria.setViewportView(txtHistoria);
        panelHistoria.add(scrollHistoria, "Center");
        panelConsulta.add(panelHistoria);
        panelContenido.add(panelConsulta, "Center");
        this.add(panelContenido, "Center");
        panelPie = new javax.swing.JPanel();
        panelPie.setLayout(new java.awt.BorderLayout(12, 12));
        lblEstado = new javax.swing.JLabel();
        lblEstado.setText("Consultá los turnos para comenzar.");
        panelPie.add(lblEstado, "North");
        lblTotal = new javax.swing.JLabel();
        lblTotal.setText("Importe a cobrar: $ 0,00");
        panelPie.add(lblTotal, "Center");
        panelBotones = new javax.swing.JPanel();
        panelBotones.setLayout(new java.awt.FlowLayout(0, 8, 6));
        btnCancelar = new javax.swing.JButton();
        btnCancelar.setText("Cancelar");
        panelBotones.add(btnCancelar);
        btnGuardar = new javax.swing.JButton();
        btnGuardar.setText("Guardar atención");
        panelBotones.add(btnGuardar);
        panelPie.add(panelBotones, "East");
        this.add(panelPie, "South");
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnConsultar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnQuitar;
    private javax.swing.JComboBox<Object> comboProfesional;
    private javax.swing.JComboBox<Object> comboTratamiento;
    private javax.swing.JLabel lblDetalle;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JLabel lblHistoria;
    private javax.swing.JLabel lblInforme;
    private javax.swing.JLabel lblPaciente;
    private javax.swing.JLabel lblProfesional;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JLabel lblTratamiento;
    private javax.swing.JLabel lblTratamientos;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelCarga;
    private javax.swing.JPanel panelConsulta;
    private javax.swing.JPanel panelContenido;
    private javax.swing.JPanel panelFiltros;
    private javax.swing.JPanel panelHistoria;
    private javax.swing.JPanel panelInforme;
    private javax.swing.JPanel panelPie;
    private javax.swing.JPanel panelRegistro;
    private javax.swing.JPanel panelTratamientos;
    private javax.swing.JPanel panelTurno;
    private javax.swing.JScrollPane scrollHistoria;
    private javax.swing.JScrollPane scrollInforme;
    private javax.swing.JScrollPane scrollTratamientos;
    private javax.swing.JScrollPane scrollTurnos;
    private javax.swing.JTable tablaTratamientos;
    private javax.swing.JTable tablaTurnos;
    private javax.swing.JTextField txtDetalle;
    private ar.com.sislum.vista.SelectorFecha selectorFecha;
    private javax.swing.JTextArea txtHistoria;
    private javax.swing.JTextArea txtInforme;
    // End of variables declaration//GEN-END:variables

}
