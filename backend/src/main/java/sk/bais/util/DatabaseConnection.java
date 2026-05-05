package sk.bais.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton Helper pre správu JDBC spojenia.
 * Navrhnutý pre Java 25 s dôrazom na čisté načítanie zdrojov.
 */
public final class DatabaseConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();

        // 1. Skús načítať externe (vedľa JAR súboru) — pre produkciu
        java.nio.file.Path externalConfig = java.nio.file.Path.of("application.properties");
        if (java.nio.file.Files.exists(externalConfig)) {
            try (InputStream input = java.nio.file.Files.newInputStream(externalConfig)) {
                props.load(input);
            } catch (IOException e) {
                throw new ExceptionInInitializerError(
                        "Chyba pri čítaní externého application.properties: " + e.getMessage());
            }
        } else {
            // 2. Fallback — načítaj z JAR (pre development)
            try (InputStream input = DatabaseConnection.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (input == null) {
                    throw new RuntimeException("application.properties nenájdený ani externe ani v JAR!");
                }
                props.load(input);
            } catch (IOException e) {
                throw new ExceptionInInitializerError(
                        "Chyba pri čítaní application.properties z JAR: " + e.getMessage());
            }
        }

        URL = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASSWORD = props.getProperty("db.password");

        // Overenie, či máme kľúčové údaje (fail-fast prístup)
        if (URL == null || USER == null || PASSWORD == null) {
            throw new RuntimeException("Chýbajúce databázové údaje v application.properties!");
        }

        // Manuálna registrácia drivera (dobrá prax pri starších JDBC implementáciách)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver nebol nájdený v Classpathe!", e);
        }
    }

    /**
     * Vráti nové spojenie do databázy.
     * POZOR: Volajúci kód musí toto spojenie uzavrieť (try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Explicitné nastavenie UTF-8 pre správne zobrazenie diakritiky
            Properties connProps = new Properties();
            connProps.setProperty("user", USER);
            connProps.setProperty("password", PASSWORD);
            connProps.setProperty("charSet", "UTF-8");
            connProps.setProperty("unicode", "true");

            Connection conn = DriverManager.getConnection(URL, connProps);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            System.err.println("Chyba pri vytváraní spojenia k DB: " + e.getMessage());
            throw e;
        }
    }

    // Privátny konštruktor zabráni inštanciovaniu utility triedy
    private DatabaseConnection() {
        throw new UnsupportedOperationException("Toto je utility trieda a nemôže byť inštancovaná.");
    }
}