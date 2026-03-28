package sk.bais.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;

public class MigrationRunner {

    // zmenit z configu pre superuser, neshipovat s aplikaciou, ktora bezi s normalnym userom
    public static void main(String[] args) {
        String dbUrl = getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/bais");
        String dbUser = getEnvOrDefault("DB_USER", "postgres");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "postgres");

        try {
            System.out.println("Configuring Flyway...");
            Flyway flyway = Flyway.configure()
                    .dataSource(dbUrl, dbUser, dbPassword)
                    .locations("classpath:db/migration")
                    .load();

            System.out.println("Flyway Info:");
            MigrationInfo current = flyway.info().current();
            if (current != null) {
                System.out.println("  Current version: " + current.getVersion());
            } else {
                System.out.println("  No schema history found. Database is uninitialized.");
            }

            MigrationInfo[] pending = flyway.info().pending();
            System.out.println("  Pending migrations: " + pending.length);
            for (MigrationInfo info : pending) {
                System.out.println("    - " + info.getVersion() + " " + info.getDescription());
            }

            System.out.println("\nRunning migrations...");
            MigrateResult result = flyway.migrate();

            System.out.println("Migration successful!");
            System.out.println("Applied " + result.migrationsExecuted + " migrations.");
            
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
