package sk.bais.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;

public class MigrationRunner {

    // zmenit z configu pre superuser, neshipovat s aplikaciou, ktora bezi s normalnym userom
    public static void main(String[] args) {
        boolean seedEnabled = false;
        if (args.length > 0 && args[0].equals("seed")) {
            seedEnabled = true;
        }

        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = MigrationRunner.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.out.println("application.properties not found, using defaults/env vars.");
            }
        } catch (java.io.IOException e) {
            System.err.println("Could not load application.properties");
        }

        String dbUrl = getEnvOrDefault("DB_URL", props.getProperty("db.url", "jdbc:postgresql://localhost:5432/bais"));
        String dbUser = getEnvOrDefault("DB_USER", props.getProperty("db.user", "postgres"));
        String dbPassword = getEnvOrDefault("DB_PASSWORD", props.getProperty("db.password", "postgres"));

        try {
            System.out.println("Configuring Flyway...");
            String[] locations = seedEnabled 
                    ? new String[]{"classpath:db/migration", "classpath:db/seed"}
                    : new String[]{"classpath:db/migration"};

            Flyway flyway = Flyway.configure()
                    .dataSource(dbUrl, dbUser, dbPassword)
                    .locations(locations)
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
