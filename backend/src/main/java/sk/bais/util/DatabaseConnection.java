package sk.bais.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Trieda pre spravu pripojenia k PostgreSQL databaze.
 * Pouziva sa ako jednoduchy Connection helper (nie connection pool).
 */
public class DatabaseConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    // Konfiguracia – nacitanie konfiguracie z application.properties suboru
    static {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("WARN: application.properties nenajdene, pouzivam defaulty.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Nepodarilo sa nacitat application.properties", e);
        }
        URL      = props.getProperty("db.url",      "jdbc:postgresql://localhost:5432/bais");
        USER     = props.getProperty("db.user",     "postgres");
        PASSWORD = props.getProperty("db.password", "postgres");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private DatabaseConnection() {}
}