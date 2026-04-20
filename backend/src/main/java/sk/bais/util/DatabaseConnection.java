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
        // Načítanie z resources cez ClassLoader
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                // Ak súbor chýba, zastavíme aplikáciu skoro, aby sme neskôr nehľadali chybu v SQL
                throw new RuntimeException("Kritická chyba: Súbor 'application.properties' nebol nájdený v src/main/resources!");
            }
            props.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Nepodarilo sa prečítať konfiguračný súbor: " + e.getMessage());
        }

        URL      = props.getProperty("db.url");
        USER     = props.getProperty("db.user");
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
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // Nastavenie autoCommitu na true je default, ale explicitnosť nezaškodí
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