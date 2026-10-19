package ar.com.sislum.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionBD {
    private ConexionBD() { }

    public static Connection obtenerConexion() throws SQLException {
        Properties datos = leerConfiguracion();
        String url = System.getenv().getOrDefault("SISLUM_DB_URL",
                "jdbc:mysql://127.0.0.1:3306/SisLum?connectTimeout=5000&socketTimeout=10000");
        datos.setProperty("connectTimeout", "5000");
        datos.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(url, datos);
    }

    // Reutiliza la conexión local sin copiar la contraseña al proyecto.
    private static Properties leerConfiguracion() throws SQLException {
        Properties datos = new Properties();
        String usuario = System.getenv("SISLUM_DB_USER");
        String clave = System.getenv("SISLUM_DB_PASSWORD");
        if (usuario == null || clave == null) {
            Path archivo = Path.of(System.getProperty("user.home"), "Library", "Application Support",
                    "SisLum", "client.cnf");
            if (Files.isRegularFile(archivo)) {
                try {
                    boolean cliente = false;
                    for (String linea : Files.readAllLines(archivo)) {
                        String texto = linea.trim();
                        if (texto.startsWith("[")) { cliente = texto.equals("[client]"); continue; }
                        if (!cliente || texto.startsWith("#") || texto.startsWith(";")) { continue; }
                        int separador = texto.indexOf('=');
                        if (separador < 0) { continue; }
                        String nombre = texto.substring(0, separador).trim();
                        String valor = texto.substring(separador + 1).trim();
                        if (valor.length() >= 2 && ((valor.startsWith("\"") && valor.endsWith("\""))
                                || (valor.startsWith("'") && valor.endsWith("'")))) {
                            valor = valor.substring(1, valor.length() - 1);
                        }
                        if (nombre.equals("user") && usuario == null) { datos.setProperty("user", valor); }
                        if (nombre.equals("password") && clave == null) { datos.setProperty("password", valor); }
                    }
                } catch (IOException error) {
                    throw new SQLException("No se pudo leer la configuración local de MySQL.", error);
                }
            }
        }
        if (usuario != null) { datos.setProperty("user", usuario); }
        if (clave != null) { datos.setProperty("password", clave); }
        if (!datos.containsKey("user") || !datos.containsKey("password")) {
            throw new SQLException("Configurá SISLUM_DB_USER y SISLUM_DB_PASSWORD para conectar con MySQL.");
        }
        return datos;
    }
}
