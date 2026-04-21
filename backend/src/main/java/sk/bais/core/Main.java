package sk.bais.core;

import java.util.List;
import java.util.Optional;

import sk.bais.auth.AuthContext;
import sk.bais.auth.AuthService;
import sk.bais.dao.EnrollmentDAO;
import sk.bais.dao.MarkDAO;
import sk.bais.dao.StudentDAO;
import sk.bais.dao.SubjectDAO;
import sk.bais.model.Enrollment;
import sk.bais.model.Student;
import sk.bais.model.Subject;
import sk.bais.service.StudentService;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== BAIS Backend Test ===\n");

        AuthService authService = new AuthService();
        StudentDAO studentDAO = new StudentDAO();
        EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
        MarkDAO markDAO = new MarkDAO();
        SubjectDAO subjectDAO = new SubjectDAO();
        StudentService studentService = new StudentService(studentDAO, enrollmentDAO, markDAO);

        // TEST 1: Prihlásenie
        System.out.println("--- TEST 1: Prihlásenie ---");
        String testEmail = "jozko.mrkvicka@stuba.sk";   
        String testPassword = "$2a$12$K8TaifKpYFrWLlOPD6XGFuQ3VfF1G4aJBZH0LB7X3nEpY3F2mWnTe";     

        Optional<AuthContext> loginResult = authService.login(testEmail, testPassword);

        if (loginResult.isEmpty()) {
            System.out.println("FAIL: Prihlásenie zlyhalo pre " + testEmail);
            System.out.println("      Skontroluj email a password_hash v seed dátach");
            System.out.println("      SQL: SELECT email, password_hash FROM \"user\" LIMIT 5;");
            // Pokračujeme bez auth - priamy DAO test
            testDirectDAO(studentDAO, subjectDAO);
            return;
        }

        AuthContext ctx = loginResult.get();
        System.out.println("OK: Prihlásený userId=" + ctx.getUserId()
                + " role=" + ctx.getRole()
                + " permissions=" + ctx.getPermissions().size());

        // TEST 2: Zoznam študentov cez Service
        System.out.println("\n--- TEST 2: Zoznam študentov ---");
        List<Student> students = studentService.getAllStudents(ctx);
        if (students.isEmpty()) {
            System.out.println("INFO: Žiadni študenti (možno nemáš permissions users:read)");
        } else {
            students.forEach(s -> System.out.println("  " + s));
        }

        // TEST 3: Zoznam predmetov (priamo cez DAO)
        System.out.println("\n--- TEST 3: Zoznam predmetov ---");
        try {
            List<Subject> subjects = subjectDAO.list();
            System.out.println("Počet predmetov v DB: " + subjects.size());
            subjects.stream().limit(3).forEach(s ->
                    System.out.println("  id=" + s.getId() + " code=" + s.getCode()
                            + " credits=" + s.getCredits()));
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
        }

        // TEST 4: Moje zápisy
        System.out.println("\n--- TEST 4: Moje zápisy ---");
        List<Enrollment> myEnrollments = studentService.getMyEnrollments(ctx);
        System.out.println("Počet zápisov: " + myEnrollments.size());
        myEnrollments.forEach(e -> System.out.println("  " + e));

        // TEST 5: Zápis na predmet (len ak existujú predmety v DB)
        System.out.println("\n--- TEST 5: Zápis na predmet ---");
        try {
            List<Subject> subjects = subjectDAO.list();
            if (subjects.isEmpty()) {
                System.out.println("SKIP: Žiadne predmety v DB na zápis");
            } else {
                // Vezmeme prvý predmet a semester 1
                int subjectId = subjects.get(0).getId();
                int semesterId = 1; // UPRAV ak vieš ID semestra zo seed dát

                Optional<Enrollment> enrollment =
                        studentService.enrollInSubject(subjectId, semesterId, ctx);

                if (enrollment.isPresent()) {
                    System.out.println("OK: Zapísaný na predmet " + subjects.get(0).getCode()
                            + " enrollmentId=" + enrollment.get().getId());
                } else {
                    System.out.println("INFO: Zápis sa nepodaril "
                            + "(možno už zapísaný, alebo chýba permissions)");
                }
            }
        } catch (Exception e) {
            System.out.println("FAIL: " + e.getMessage());
        }

        System.out.println("\n=== Test dokončený ===");
    }

    // Fallback - priamy DAO test bez auth (ak login zlyhá)
    private static void testDirectDAO(StudentDAO studentDAO, SubjectDAO subjectDAO) {
        System.out.println("\n--- FALLBACK: Priamy DAO test ---");
        try {
            List<Student> students = studentDAO.list();
            System.out.println("Študenti v DB: " + students.size());
            students.stream().limit(3).forEach(s -> System.out.println("  " + s));

            List<Subject> subjects = subjectDAO.list();
            System.out.println("Predmety v DB: " + subjects.size());
            subjects.stream().limit(3).forEach(s ->
                    System.out.println("  " + s.getCode() + " - " + s.getCredits() + " kreditov"));
        } catch (Exception e) {
            System.out.println("FAIL DAO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}