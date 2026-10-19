package ar.com.sislum;

import ar.com.sislum.vista.VentanaPrincipal;
import javax.swing.SwingUtilities;

public final class SisLum {
    private SisLum() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new VentanaPrincipal().setVisible(true));
    }
}
