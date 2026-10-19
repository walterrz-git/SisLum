package ar.com.sislum.vista;

import java.awt.BorderLayout;
import java.awt.CardLayout;

// Cada opción abre una pantalla independiente.
public final class VentanaPrincipal extends javax.swing.JFrame {
    private final VistaTurnos vistaTurnos=new VistaTurnos();
    private final VistaAgenda vistaAgenda=new VistaAgenda();
    private final VistaPacientes vistaPacientes=new VistaPacientes();
    private final VistaAtencion vistaAtencion=new VistaAtencion();
    private final VistaCobros vistaCobros=new VistaCobros();

    public VentanaPrincipal() {
        initComponents();
        setMinimumSize(new java.awt.Dimension(1180,780));
        setSize(1280,850);
        setLocationRelativeTo(null);
        panelMenu.setBorder(javax.swing.BorderFactory.createEmptyBorder(24,18,24,18));
        panelContenido.setBorder(javax.swing.BorderFactory.createEmptyBorder(20,4,12,20));
        lblEstado.setBorder(javax.swing.BorderFactory.createEmptyBorder(8,16,8,16));
        lblMarca.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,25));
        panelNuevoTurno.add(vistaTurnos,BorderLayout.CENTER);
        panelAgenda.add(vistaAgenda,BorderLayout.CENTER);
        panelPacientes.add(vistaPacientes,BorderLayout.CENTER);
        panelAtenciones.add(vistaAtencion,BorderLayout.CENTER);
        panelCobros.add(vistaCobros,BorderLayout.CENTER);
        vistaTurnos.setAlCancelar(() -> { mostrarModulo("Agenda");vistaAgenda.cargarDatos(); });
        btnNuevoTurno.addActionListener(evento -> { mostrarModulo("NuevoTurno");vistaTurnos.cargarDatos(); });
        btnAgenda.addActionListener(evento -> { mostrarModulo("Agenda");vistaAgenda.cargarDatos(); });
        btnPacientes.addActionListener(evento -> { mostrarModulo("Pacientes");vistaPacientes.consultarPaciente(); });
        btnAtenciones.addActionListener(evento -> { mostrarModulo("Atenciones");vistaAtencion.cargarDatos(); });
        btnCobros.addActionListener(evento -> { mostrarModulo("Cobros");vistaCobros.cargarDatos(); });
        mostrarModulo("NuevoTurno");
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent evento) {
                vistaTurnos.cargarDatos();
            }
        });
    }

    private void mostrarModulo(String nombre) {
        ((CardLayout)panelContenido.getLayout()).show(panelContenido,nombre);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        this.setDefaultCloseOperation(3);
        this.setTitle("SisLum | Centro de medicina estética Lumina");
        getContentPane().setLayout(new java.awt.BorderLayout(12, 12));
        panelMenu = new javax.swing.JPanel();
        panelMenu.setBackground(new java.awt.Color(237, 230, 222));
        panelMenu.setLayout(new java.awt.BorderLayout(0, 20));
        panelNavegacion = new javax.swing.JPanel();
        panelNavegacion.setLayout(new java.awt.GridLayout(0, 1, 12, 12));
        lblMarca = new javax.swing.JLabel();
        lblMarca.setText("SisLum");
        panelNavegacion.add(lblMarca);
        btnNuevoTurno = new javax.swing.JButton();
        btnNuevoTurno.setText("Nuevo turno");
        panelNavegacion.add(btnNuevoTurno);
        btnAgenda = new javax.swing.JButton();
        btnAgenda.setText("Agenda");
        panelNavegacion.add(btnAgenda);
        btnPacientes = new javax.swing.JButton();
        btnPacientes.setText("Pacientes");
        panelNavegacion.add(btnPacientes);
        btnAtenciones = new javax.swing.JButton();
        btnAtenciones.setText("Atenciones");
        panelNavegacion.add(btnAtenciones);
        btnCobros = new javax.swing.JButton();
        btnCobros.setText("Cobros");
        panelNavegacion.add(btnCobros);
        panelMenu.add(panelNavegacion, "North");
        getContentPane().add(panelMenu, "West");
        panelContenido = new javax.swing.JPanel();
        panelContenido.setLayout(new java.awt.CardLayout());
        panelNuevoTurno = new javax.swing.JPanel();
        panelNuevoTurno.setLayout(new java.awt.BorderLayout(12, 12));
        panelContenido.add(panelNuevoTurno, "NuevoTurno");
        panelAgenda = new javax.swing.JPanel();
        panelAgenda.setLayout(new java.awt.BorderLayout(12, 12));
        panelContenido.add(panelAgenda, "Agenda");
        panelPacientes = new javax.swing.JPanel();
        panelPacientes.setLayout(new java.awt.BorderLayout(12, 12));
        panelContenido.add(panelPacientes, "Pacientes");
        panelAtenciones = new javax.swing.JPanel();
        panelAtenciones.setLayout(new java.awt.BorderLayout(12, 12));
        panelContenido.add(panelAtenciones, "Atenciones");
        panelCobros = new javax.swing.JPanel();
        panelCobros.setLayout(new java.awt.BorderLayout(16, 16));
        panelContenido.add(panelCobros, "Cobros");
        getContentPane().add(panelContenido, "Center");
        lblEstado = new javax.swing.JLabel();
        lblEstado.setText("SisLum · Turnos de lunes a viernes, de 9 a 18");
        getContentPane().add(lblEstado, "South");
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgenda;
    private javax.swing.JButton btnAtenciones;
    private javax.swing.JButton btnCobros;
    private javax.swing.JButton btnNuevoTurno;
    private javax.swing.JButton btnPacientes;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblMarca;
    private javax.swing.JPanel panelAgenda;
    private javax.swing.JPanel panelAtenciones;
    private javax.swing.JPanel panelCobros;
    private javax.swing.JPanel panelContenido;
    private javax.swing.JPanel panelMenu;
    private javax.swing.JPanel panelNavegacion;
    private javax.swing.JPanel panelNuevoTurno;
    private javax.swing.JPanel panelPacientes;
    // End of variables declaration//GEN-END:variables

}
