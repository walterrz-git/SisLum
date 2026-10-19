package ar.com.sislum.vista;

import ar.com.sislum.controlador.TurnoController;
import ar.com.sislum.modelo.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class VistaAgenda extends JPanel {
    private final TurnoController controlador = new TurnoController();
    private final DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private boolean ocupado;
    private boolean cargado;

    public VistaAgenda() {
        initComponents();
        lblTitulo.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,23));
        comboHora.addItem("Todos los horarios");
        for (LocalTime hora=LocalTime.of(9,0);hora.isBefore(LocalTime.of(18,0));hora=hora.plusMinutes(30)) {
            comboHora.addItem(hora);
        }
        tablaTurnos.setModel(new DefaultTableModel(
                new String[]{"Hora","Profesional","Paciente","DNI","Tratamiento previsto","Estado"},0) {
            @Override public boolean isCellEditable(int fila,int columna) { return false; }
        });
        tablaTurnos.getTableHeader().setReorderingAllowed(false);
        btnConsultar.addActionListener(evento -> consultarAgenda());
        comboProfesional.addActionListener(evento -> filtrosModificados());
        comboHora.addActionListener(evento -> filtrosModificados());
        selectorFecha.addDateChangeListener(evento -> filtrosModificados());
    }

    private void filtrosModificados() {
        ((DefaultTableModel)tablaTurnos.getModel()).setRowCount(0);
        lblEstado.setText("Filtros modificados. Pulsá Consultar.");
    }

    public void cargarDatos() {
        if (ocupado) { return; }
        if (cargado) { consultarAgenda(); return; }
        ejecutar(() -> controlador.consultarProfesionales(), profesionales -> {
            comboProfesional.removeAllItems();
            comboProfesional.addItem("Todos los profesionales");
            for (Profesional profesional:profesionales) { comboProfesional.addItem(profesional); }
            cargado=true;
            consultarAgenda();
        });
    }

    public void consultarAgenda() {
        if (ocupado) { return; }
        try {
            LocalDate fecha=selectorFecha.leerFecha();
            Object seleccionado=comboProfesional.getSelectedItem();
            Profesional profesional=seleccionado instanceof Profesional ? (Profesional)seleccionado : null;
            Object horaElegida=comboHora.getSelectedItem();
            LocalTime hora=horaElegida instanceof LocalTime ? (LocalTime)horaElegida : null;
            ejecutar(() -> controlador.consultarAgenda(profesional,fecha,hora), turnos -> {
                DefaultTableModel modelo=(DefaultTableModel)tablaTurnos.getModel();
                modelo.setRowCount(0);
                for(Turno turno:turnos) {
                    Paciente paciente=turno.getPaciente();
                    Profesional medico=turno.getAgenda().getProfesional();
                    modelo.addRow(new Object[]{turno.getFechaHora().toLocalTime(),medico.getNombre()+" "+medico.getApellido(),
                        paciente.getNombre()+" "+paciente.getApellido(),paciente.getDni(),
                        turno.getTratamientoPrevisto()==null ? "Sin especificar":turno.getTratamientoPrevisto(),turno.getEstado()});
                }
                lblEstado.setText("Turnos encontrados: "+turnos.size()+" · "+fecha.format(formato));
            });
        } catch(IllegalArgumentException error) {
            filtrosModificados();
            JOptionPane.showMessageDialog(this,"Seleccioná una fecha en el calendario.");
        }
    }

    private <T> void ejecutar(Callable<T> operacion,Consumer<T> alTerminar) {
        cambiarOcupado(true);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return operacion.call(); }
            @Override protected void done() {
                cambiarOcupado(false);
                try { alTerminar.accept(get()); }
                catch(InterruptedException error) { Thread.currentThread().interrupt(); }
                catch(ExecutionException error) {
                    ((DefaultTableModel)tablaTurnos.getModel()).setRowCount(0);
                    lblEstado.setText("No se pudo consultar la agenda.");
                    JOptionPane.showMessageDialog(VistaAgenda.this,"No se pudo consultar la agenda. Revisá la conexión con MySQL.");
                }
            }
        }.execute();
    }

    private void cambiarOcupado(boolean valor) {
        ocupado=valor;
        for(JComponent componente:new JComponent[]{btnConsultar,selectorFecha,comboProfesional,comboHora}) {
            componente.setEnabled(!valor);
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        this.setLayout(new java.awt.BorderLayout(12, 12));
        lblTitulo = new javax.swing.JLabel();
        lblTitulo.setText("Agenda");
        this.add(lblTitulo, "North");
        panelConsulta = new javax.swing.JPanel();
        panelConsulta.setLayout(new java.awt.BorderLayout(12, 12));
        panelFiltros = new javax.swing.JPanel();
        panelFiltros.setLayout(new java.awt.FlowLayout(0, 8, 6));
        lblDia = new javax.swing.JLabel();
        lblDia.setText("Día (dd/MM/aaaa)");
        panelFiltros.add(lblDia);
        selectorFecha = new ar.com.sislum.vista.SelectorFecha();
        panelFiltros.add(selectorFecha);
        lblProfesional = new javax.swing.JLabel();
        lblProfesional.setText("Profesional");
        panelFiltros.add(lblProfesional);
        comboProfesional = new javax.swing.JComboBox<>();
        panelFiltros.add(comboProfesional);
        lblHora = new javax.swing.JLabel();
        lblHora.setText("Hora");
        panelFiltros.add(lblHora);
        comboHora = new javax.swing.JComboBox<>();
        panelFiltros.add(comboHora);
        btnConsultar = new javax.swing.JButton();
        btnConsultar.setText("Consultar");
        panelFiltros.add(btnConsultar);
        panelConsulta.add(panelFiltros, "North");
        scrollTurnos = new javax.swing.JScrollPane();
        tablaTurnos = new javax.swing.JTable();
        tablaTurnos.setRowHeight(26);
        scrollTurnos.setViewportView(tablaTurnos);
        panelConsulta.add(scrollTurnos, "Center");
        this.add(panelConsulta, "Center");
        lblEstado = new javax.swing.JLabel();
        lblEstado.setText("Seleccioná los filtros y consultá la agenda.");
        this.add(lblEstado, "South");
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConsultar;
    private javax.swing.JComboBox<Object> comboHora;
    private javax.swing.JComboBox<Object> comboProfesional;
    private javax.swing.JLabel lblDia;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblHora;
    private javax.swing.JLabel lblProfesional;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel panelConsulta;
    private javax.swing.JPanel panelFiltros;
    private javax.swing.JScrollPane scrollTurnos;
    private javax.swing.JTable tablaTurnos;
    private ar.com.sislum.vista.SelectorFecha selectorFecha;
    // End of variables declaration//GEN-END:variables

}
