package sk.bais.core;

import sk.bais.auth.AuthService;
import sk.bais.dao.EnrollmentDAO;
import sk.bais.dao.MarkDAO;
import sk.bais.dao.StudentDAO;
import sk.bais.dao.SubjectDAO;
import sk.bais.service.StudentService;

/**
 * Hlavná vstupná trieda aplikácie.
 * Zodpovedá za inicializáciu backend služieb a spustenie WebSocket servera.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== BAIS Backend Server - Štartujem ===\n");

        // 1. Inicializácia DAO vrstvy
        AuthService authService = new AuthService();
        StudentDAO studentDAO = new StudentDAO();
        EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
        MarkDAO markDAO = new MarkDAO();
        SubjectDAO subjectDAO = new SubjectDAO();

        // 2. Inicializácia Biznis logiky (Service vrstva)
        StudentService studentService = new StudentService(studentDAO, enrollmentDAO, markDAO);

        // 3. Spustenie WebSocket servera na porte 8887
        int port = 8887;
        BaisWebSocketServer server = new BaisWebSocketServer(port, authService, studentService);
        server.start();

        System.out.println("Server beží na: ws://localhost:" + port);
        System.out.println("Backend je pripravený prijímať spojenia z frontendu.");
        System.out.println("Ukončíte ho pomocou Ctrl+C.");

        // 4. Graceful shutdown – zachytí Ctrl+C a korektne uzavrie všetky spojenia
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nZastavujem server...");
            try {
                server.stop(3000); // 3 sekundy na dokončenie aktívnych spojení
                System.out.println("Server úspešne zastavený.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Prerušenie počas zastavovania servera.");
            }
        }));
    }
}