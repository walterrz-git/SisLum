package ar.com.sislum.vista;

// Campos del alta de paciente.
public final class FormularioPaciente extends javax.swing.JPanel {
    public FormularioPaciente() {
        initComponents();
    }

    public String getDni() { return txtDni.getText(); }
    public String getNombre() { return txtNombre.getText(); }
    public String getApellido() { return txtApellido.getText(); }
    public String getTelefono() { return txtTelefono.getText(); }
    public String getEmail() { return txtEmail.getText(); }

    @SuppressWarnings("unchecked")
    private void initComponents() {//GEN-BEGIN:initComponents
        this.setLayout(new java.awt.GridLayout(0, 2, 12, 12));
        lblDni = new javax.swing.JLabel();
        lblDni.setText("DNI (sin puntos)*");
        this.add(lblDni);
        txtDni = new javax.swing.JTextField();
        txtDni.setColumns(22);
        this.add(txtDni);
        lblNombre = new javax.swing.JLabel();
        lblNombre.setText("Nombre*");
        this.add(lblNombre);
        txtNombre = new javax.swing.JTextField();
        txtNombre.setColumns(22);
        this.add(txtNombre);
        lblApellido = new javax.swing.JLabel();
        lblApellido.setText("Apellido*");
        this.add(lblApellido);
        txtApellido = new javax.swing.JTextField();
        txtApellido.setColumns(22);
        this.add(txtApellido);
        lblTelefono = new javax.swing.JLabel();
        lblTelefono.setText("Teléfono");
        this.add(lblTelefono);
        txtTelefono = new javax.swing.JTextField();
        txtTelefono.setColumns(22);
        this.add(txtTelefono);
        lblEmail = new javax.swing.JLabel();
        lblEmail.setText("Email");
        this.add(lblEmail);
        txtEmail = new javax.swing.JTextField();
        txtEmail.setColumns(22);
        this.add(txtEmail);
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblApellido;
    private javax.swing.JLabel lblDni;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblTelefono;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtDni;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables

}
