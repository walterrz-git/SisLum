package ar.com.sislum.vista;

import ar.com.sislum.controlador.CobroController;
import ar.com.sislum.modelo.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class VistaCobros extends JPanel {
    private final CobroController controlador = new CobroController();
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
    private final DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private List<Atencion> pendientes = List.of();
    private Atencion atencionSeleccionada;
    private java.util.Map<Integer,Cobro> cobros = java.util.Map.of();
    private Cobro cobroSeleccionado;
    private record Consulta(List<Atencion> atenciones, java.util.Map<Integer,Cobro> cobros) {}
    private boolean ocupado;
    private boolean cambiandoSeleccion;

    public VistaCobros() {
        initComponents();
        prepararVista();
        conectarAcciones();
    }

    private void prepararVista() {
        lblTitulo.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,23));
        tablaAtenciones.setModel(modelo("Atención","Fecha y hora del turno","DNI","Paciente","Profesional","Importe","Estado","Fecha de cobro","Medio de pago"));
        tablaTratamientos.setModel(modelo("Tratamiento realizado","Importe"));
        for(JTable tabla:new JTable[]{tablaAtenciones,tablaTratamientos}) {
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabla.getTableHeader().setReorderingAllowed(false);
        }
        scrollAtenciones.setPreferredSize(new java.awt.Dimension(950,115));
        scrollObservaciones.setPreferredSize(new java.awt.Dimension(500,100));
        txtObservaciones.setLineWrap(true);txtObservaciones.setWrapStyleWord(true);
        comboMedioPago.addItem("Seleccionar medio de pago");
        for(String medio:new String[]{"Efectivo","Transferencia","Tarjeta de débito","Tarjeta de crédito"}) {
            comboMedioPago.addItem(medio);
        }
        actualizarAcciones();
    }

    private DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas,0) {
            @Override public boolean isCellEditable(int fila,int columna) { return false; }
        };
    }

    private void conectarAcciones() {
        btnBuscar.addActionListener(evento -> buscarPaciente());
        txtBusqueda.addActionListener(evento -> buscarPaciente());
        btnActualizar.addActionListener(evento -> consultarImporte());
        btnRegistrar.addActionListener(evento -> registrarCobro());
        btnCancelar.addActionListener(evento -> cancelar());
        comboMedioPago.addActionListener(evento -> actualizarAcciones());
        comboEstado.addActionListener(evento -> consultarImporte());
        fechaTurno.addDateChangeListener(evento -> consultarImporte());
        tablaAtenciones.getSelectionModel().addListSelectionListener(evento -> {
            if(evento.getValueIsAdjusting() || cambiandoSeleccion) { return; }
            seleccionarAtencion();
        });
    }

    public void cargarDatos() { consultarImporte(); }

    public void buscarPaciente() { consultarImporte(); }

    public void consultarImporte() {
        if(ocupado) { return; }
        limpiarPendientes();
        java.time.LocalDate fecha=fechaTurno.leerFecha();
        String consulta=txtBusqueda.getText();
        String estado=switch(comboEstado.getSelectedIndex()) {
            case 1 -> "PAGADO";
            case 2 -> "TODOS";
            default -> "PENDIENTE";
        };
        ejecutar(() -> {
            List<Atencion> atenciones=controlador.consultarPorEstado(fecha,consulta,estado);
            java.util.Map<Integer,Cobro> encontrados=new java.util.HashMap<>();
            for(Atencion atencion:atenciones) {
                Cobro cobro=controlador.consultarCobro(atencion.getIdAtencion());
                if(cobro!=null && "PAGADO".equals(cobro.getEstado())) { encontrados.put(atencion.getIdAtencion(),cobro); }
            }
            return new Consulta(atenciones,encontrados);
        }, resultado -> {
            pendientes=resultado.atenciones();
            cobros=resultado.cobros();
            DefaultTableModel modelo=(DefaultTableModel)tablaAtenciones.getModel();
            BigDecimal total=new BigDecimal("0.00");
            for(Atencion atencion:pendientes) {
                Profesional profesional=atencion.getTurno().getAgenda().getProfesional();
                Paciente paciente=atencion.getTurno().getPaciente();
                Cobro cobro=cobros.get(atencion.getIdAtencion());
                modelo.addRow(new Object[]{atencion.getIdAtencion(),
                    atencion.getTurno().getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")),
                    paciente.getDni(),paciente.getNombre()+" "+paciente.getApellido(),
                    profesional.getNombre()+" "+profesional.getApellido(),moneda.format(atencion.getImporteTotal()),
                    cobro==null ? "PENDIENTE":"PAGADO",cobro==null ? "—":cobro.getFecha().format(formato),
                    cobro==null ? "—":nombreMedio(cobro.getMedioPago())});
                total=total.add(atencion.getImporteTotal());
            }
            lblEstado.setText(pendientes.isEmpty() ? "No hay atenciones para los filtros elegidos."
                    : "Atenciones: "+pendientes.size()+" · Total: "+moneda.format(total));
            if(pendientes.size()==1) { tablaAtenciones.setRowSelectionInterval(0,0); }
            actualizarAcciones();
        });
    }

    private void seleccionarAtencion() {
        int fila=tablaAtenciones.getSelectedRow();
        limpiarDetalle();
        atencionSeleccionada=fila>=0 && fila<pendientes.size() ? pendientes.get(fila):null;
        if(atencionSeleccionada==null) { actualizarAcciones();return; }
        Atencion atencion=atencionSeleccionada;
        Paciente paciente=atencion.getTurno().getPaciente();
        lblPaciente.setText("Paciente: "+paciente.getNombre()+" "+paciente.getApellido()+" · DNI "+paciente.getDni());
        Profesional profesional=atencion.getTurno().getAgenda().getProfesional();
        lblAtencion.setText("Atención N.º "+atencion.getIdAtencion()+" · "+atencion.getFecha().format(formato));
        lblProfesional.setText("Profesional: "+profesional.getNombre()+" "+profesional.getApellido());
        lblImporte.setText("Total a cobrar: "+moneda.format(atencion.getImporteTotal()));
        cobroSeleccionado=cobros.get(atencion.getIdAtencion());
        lblSituacion.setText(cobroSeleccionado==null ? "Estado: PENDIENTE"
                : "PAGADO · Cobro N.º "+cobroSeleccionado.getIdCobro()+" · "+cobroSeleccionado.getFecha().format(formato));
        if(cobroSeleccionado!=null) {
            lblImporte.setText("Total cobrado: "+moneda.format(cobroSeleccionado.getImporteTotal()));
            txtObservaciones.setText(cobroSeleccionado.getObservaciones());
            comboMedioPago.setSelectedItem(nombreMedio(cobroSeleccionado.getMedioPago()));
        }
        DefaultTableModel modelo=(DefaultTableModel)tablaTratamientos.getModel();
        for(TratamientoRealizado detalle:atencion.getTratamientos()) {
            modelo.addRow(new Object[]{detalle.getTratamiento().getNombre(),moneda.format(detalle.getImporte())});
        }
        actualizarAcciones();
    }

    private String nombreMedio(String medio) {
        return switch(medio) {
            case "EFECTIVO" -> "Efectivo";
            case "TRANSFERENCIA" -> "Transferencia";
            case "DEBITO" -> "Tarjeta de débito";
            case "CREDITO" -> "Tarjeta de crédito";
            default -> medio;
        };
    }

    private String medioSeleccionado() {
        return switch(comboMedioPago.getSelectedIndex()) {
            case 1 -> "EFECTIVO";
            case 2 -> "TRANSFERENCIA";
            case 3 -> "DEBITO";
            case 4 -> "CREDITO";
            default -> throw new IllegalArgumentException("Seleccioná un medio de pago.");
        };
    }

    public void registrarCobro() {
        if(ocupado) { return; }
        try {
            Atencion atencion=atencionSeleccionada;
            if(cobroSeleccionado!=null) { throw new IllegalArgumentException("La atención ya está pagada."); }
            if(atencion==null) { throw new IllegalArgumentException("Seleccioná una atención."); }
            String medio=medioSeleccionado();
            String observaciones=txtObservaciones.getText();
            Paciente paciente=atencion.getTurno().getPaciente();
            String resumen="Paciente: "+paciente.getNombre()+" "+paciente.getApellido()
                    +"\nAtención N.º "+atencion.getIdAtencion()+"\nTotal: "+moneda.format(atencion.getImporteTotal())
                    +"\nMedio de pago: "+comboMedioPago.getSelectedItem()+"\n¿Confirmás el cobro?";
            if(JOptionPane.showConfirmDialog(this,resumen,"Registrar cobro",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) { return; }
            ejecutar(() -> controlador.registrarCobro(atencion,medio,observaciones), cobro -> {
                // El cobro ya se guardó; ahora se actualiza la lista de pendientes.
                JOptionPane.showMessageDialog(this,"Cobro N.º "+cobro.getIdCobro()+" registrado.\nImporte: "
                        +moneda.format(cobro.getImporteTotal())+"\nEstado: PAGADO");
                consultarImporte();
            });
        } catch(IllegalArgumentException error) { mostrarError(error); }
    }

    private void limpiarDetalle() {
        atencionSeleccionada=null;
        cobroSeleccionado=null;
        ((DefaultTableModel)tablaTratamientos.getModel()).setRowCount(0);
        txtObservaciones.setText("");comboMedioPago.setSelectedIndex(0);
        lblAtencion.setText("Seleccioná una atención.");
        lblProfesional.setText("Profesional: —");lblImporte.setText("Total a cobrar: $ 0,00");lblSituacion.setText("Estado: —");
        actualizarAcciones();
    }

    private void limpiarPendientes() {
        boolean anterior=cambiandoSeleccion;cambiandoSeleccion=true;
        tablaAtenciones.clearSelection();
        pendientes=List.of();
        cobros=java.util.Map.of();
        ((DefaultTableModel)tablaAtenciones.getModel()).setRowCount(0);
        limpiarDetalle();
        cambiandoSeleccion=anterior;
        lblPaciente.setText("Seleccioná una atención de la fecha elegida.");
    }

    private void cancelar() {
        txtBusqueda.setText("");
        consultarImporte();
    }

    private void actualizarAcciones() {
        boolean seleccionado=!ocupado && atencionSeleccionada!=null && cobroSeleccionado==null;
        comboMedioPago.setEnabled(seleccionado);txtObservaciones.setEnabled(seleccionado);
        btnRegistrar.setEnabled(seleccionado && comboMedioPago.getSelectedIndex()>0);
        btnActualizar.setEnabled(!ocupado);
    }

    private <T> void ejecutar(Callable<T> operacion,Consumer<T> alTerminar) {
        cambiarOcupado(true);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return operacion.call(); }
            @Override protected void done() {
                cambiarOcupado(false);
                try { alTerminar.accept(get()); }
                catch(InterruptedException error) { Thread.currentThread().interrupt(); }
                catch(ExecutionException error) { limpiarDetalle();mostrarError(error.getCause()); }
            }
        }.execute();
    }

    private void cambiarOcupado(boolean valor) {
        ocupado=valor;
        for(JComponent componente:new JComponent[]{txtBusqueda,fechaTurno,comboEstado,btnBuscar,btnCancelar,tablaAtenciones}) {
            componente.setEnabled(!valor);
        }
        actualizarAcciones();
    }

    private void mostrarError(Throwable error) {
        String mensaje=error instanceof IllegalArgumentException ? error.getMessage()
                : "No se pudo completar la operación con MySQL. Revisá la conexión y la preparación de la base.";
        lblEstado.setText(mensaje);JOptionPane.showMessageDialog(this,mensaje,"Revisá el cobro",JOptionPane.WARNING_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        this.setLayout(new java.awt.BorderLayout(12, 12));
        lblTitulo = new javax.swing.JLabel();
        lblTitulo.setText("03 - Registrar cobro (CU006)");
        this.add(lblTitulo, "North");
        panelContenido = new javax.swing.JPanel();
        panelContenido.setLayout(new java.awt.BorderLayout(12, 12));
        panelPaciente = new javax.swing.JPanel();
        panelPaciente.setLayout(new java.awt.BorderLayout(12, 12));
        panelBusqueda = new javax.swing.JPanel();
        panelBusqueda.setLayout(new java.awt.FlowLayout(0, 8, 6));
        lblFecha = new javax.swing.JLabel("Fecha del turno");
        panelBusqueda.add(lblFecha);
        fechaTurno = new SelectorFecha();
        panelBusqueda.add(fechaTurno);
        lblFiltroEstado = new javax.swing.JLabel("Estado");
        panelBusqueda.add(lblFiltroEstado);
        comboEstado = new javax.swing.JComboBox<>(new String[]{"Pendiente","Pagado","Todos"});
        panelBusqueda.add(comboEstado);
        lblBuscar = new javax.swing.JLabel();
        lblBuscar.setText("Paciente o DNI (opcional)");
        panelBusqueda.add(lblBuscar);
        txtBusqueda = new javax.swing.JTextField();
        txtBusqueda.setColumns(12);
        panelBusqueda.add(txtBusqueda);
        btnBuscar = new javax.swing.JButton();
        btnBuscar.setText("Buscar");
        panelBusqueda.add(btnBuscar);
        panelPaciente.add(panelBusqueda, "North");
        lblPaciente = new javax.swing.JLabel();
        lblPaciente.setText("Seleccioná una atención de la fecha elegida.");
        panelPaciente.add(lblPaciente, "South");
        panelContenido.add(panelPaciente, "North");
        panelCobro = new javax.swing.JPanel();
        panelCobro.setLayout(new java.awt.BorderLayout(12, 12));
        panelPendientes = new javax.swing.JPanel();
        panelPendientes.setLayout(new java.awt.BorderLayout(12, 12));
        panelAcciones = new javax.swing.JPanel();
        panelAcciones.setLayout(new java.awt.FlowLayout(0, 8, 6));
        lblPendientes = new javax.swing.JLabel();
        lblPendientes.setText("Atenciones y cobros");
        panelAcciones.add(lblPendientes);
        btnActualizar = new javax.swing.JButton();
        btnActualizar.setText("Actualizar");
        panelAcciones.add(btnActualizar);
        panelPendientes.add(panelAcciones, "North");
        scrollAtenciones = new javax.swing.JScrollPane();
        tablaAtenciones = new javax.swing.JTable();
        tablaAtenciones.setRowHeight(28);
        scrollAtenciones.setViewportView(tablaAtenciones);
        panelPendientes.add(scrollAtenciones, "Center");
        panelCobro.add(panelPendientes, "North");
        panelDetalle = new javax.swing.JPanel();
        panelDetalle.setLayout(new java.awt.GridLayout(1, 2, 20, 12));
        panelTratamientos = new javax.swing.JPanel();
        panelTratamientos.setLayout(new java.awt.BorderLayout(12, 12));
        lblTratamientos = new javax.swing.JLabel();
        lblTratamientos.setText("Tratamientos realizados e importes");
        panelTratamientos.add(lblTratamientos, "North");
        scrollTratamientos = new javax.swing.JScrollPane();
        tablaTratamientos = new javax.swing.JTable();
        tablaTratamientos.setRowHeight(28);
        scrollTratamientos.setViewportView(tablaTratamientos);
        panelTratamientos.add(scrollTratamientos, "Center");
        panelObservaciones = new javax.swing.JPanel();
        panelObservaciones.setLayout(new java.awt.BorderLayout(12, 12));
        lblObservaciones = new javax.swing.JLabel();
        lblObservaciones.setText("Observaciones del cobro (opcional)");
        panelObservaciones.add(lblObservaciones, "North");
        scrollObservaciones = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        txtObservaciones.setRows(3);
        txtObservaciones.setColumns(24);
        scrollObservaciones.setViewportView(txtObservaciones);
        panelObservaciones.add(scrollObservaciones, "Center");
        panelTratamientos.add(panelObservaciones, "South");
        panelDetalle.add(panelTratamientos);
        panelResumen = new javax.swing.JPanel();
        panelResumen.setLayout(new java.awt.BorderLayout(12, 12));
        panelDatosResumen = new javax.swing.JPanel();
        panelDatosResumen.setLayout(new java.awt.GridLayout(0, 1, 8, 16));
        lblResumen = new javax.swing.JLabel();
        lblResumen.setText("Resumen del cobro");
        panelDatosResumen.add(lblResumen);
        lblAtencion = new javax.swing.JLabel();
        lblAtencion.setText("Seleccioná una atención.");
        panelDatosResumen.add(lblAtencion);
        lblProfesional = new javax.swing.JLabel();
        lblProfesional.setText("Profesional: —");
        panelDatosResumen.add(lblProfesional);
        lblImporte = new javax.swing.JLabel();
        lblImporte.setText("Total a cobrar: $ 0,00");
        panelDatosResumen.add(lblImporte);
        lblSituacion = new javax.swing.JLabel();
        lblSituacion.setText("Estado: —");
        panelDatosResumen.add(lblSituacion);
        lblMedioPago = new javax.swing.JLabel();
        lblMedioPago.setText("Medio de pago");
        panelDatosResumen.add(lblMedioPago);
        comboMedioPago = new javax.swing.JComboBox<>();
        panelDatosResumen.add(comboMedioPago);
        panelResumen.add(panelDatosResumen, "North");
        panelDetalle.add(panelResumen);
        panelCobro.add(panelDetalle, "Center");
        panelContenido.add(panelCobro, "Center");
        this.add(panelContenido, "Center");
        panelPie = new javax.swing.JPanel();
        panelPie.setLayout(new java.awt.BorderLayout(12, 12));
        lblEstado = new javax.swing.JLabel();
        lblEstado.setText("Elegí la fecha del turno y el estado del cobro.");
        panelPie.add(lblEstado, "Center");
        panelBotones = new javax.swing.JPanel();
        panelBotones.setLayout(new java.awt.FlowLayout(0, 8, 6));
        btnCancelar = new javax.swing.JButton();
        btnCancelar.setText("Cancelar");
        panelBotones.add(btnCancelar);
        btnRegistrar = new javax.swing.JButton();
        btnRegistrar.setText("Registrar cobro");
        panelBotones.add(btnRegistrar);
        panelPie.add(panelBotones, "East");
        this.add(panelPie, "South");
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblFiltroEstado;
    private javax.swing.JComboBox<String> comboEstado;
    private SelectorFecha fechaTurno;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JComboBox<Object> comboMedioPago;
    private javax.swing.JLabel lblAtencion;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblImporte;
    private javax.swing.JLabel lblMedioPago;
    private javax.swing.JLabel lblObservaciones;
    private javax.swing.JLabel lblPaciente;
    private javax.swing.JLabel lblPendientes;
    private javax.swing.JLabel lblProfesional;
    private javax.swing.JLabel lblResumen;
    private javax.swing.JLabel lblSituacion;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblTratamientos;
    private javax.swing.JPanel panelAcciones;
    private javax.swing.JPanel panelBotones;
    private javax.swing.JPanel panelBusqueda;
    private javax.swing.JPanel panelCobro;
    private javax.swing.JPanel panelContenido;
    private javax.swing.JPanel panelDatosResumen;
    private javax.swing.JPanel panelDetalle;
    private javax.swing.JPanel panelObservaciones;
    private javax.swing.JPanel panelPaciente;
    private javax.swing.JPanel panelPendientes;
    private javax.swing.JPanel panelPie;
    private javax.swing.JPanel panelResumen;
    private javax.swing.JPanel panelTratamientos;
    private javax.swing.JScrollPane scrollAtenciones;
    private javax.swing.JScrollPane scrollObservaciones;
    private javax.swing.JScrollPane scrollTratamientos;
    private javax.swing.JTable tablaAtenciones;
    private javax.swing.JTable tablaTratamientos;
    private javax.swing.JTextField txtBusqueda;
    private javax.swing.JTextArea txtObservaciones;
    // End of variables declaration//GEN-END:variables

}
