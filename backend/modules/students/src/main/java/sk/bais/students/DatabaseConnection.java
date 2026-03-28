package sk.bais.students;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Trieda pre spravu pripojenia k PostgreSQL databaze.
 * Pouziva sa ako jednoduchy Connection helper (nie connection pool).
 */
public class DatabaseConnection {

    // --- Konfiguracia – idealne nacitat z .properties suboru alebo env premennych ---
    private static final String URL      = "jdbc:postgresql://localhost:5432/bais_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "idkMan";

    static {
        try {
            // Nacitanie JDBC drivera (pre novsie verzie nie je nutne, ale neskodi)
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver nebol najdeny!", e);
        }
    }

    /**
     * Vrati nove pripojenie k databaze.
     * Volajuci kod je zodpovedny za zatvorenie (try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // Privatny konstruktor – trieda je len utilita, nema sa instanciovat
    private DatabaseConnection() {}
}
