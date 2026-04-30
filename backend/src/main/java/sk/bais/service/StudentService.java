package sk.bais.service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import sk.bais.auth.AuthContext;
import sk.bais.dao.EnrollmentDAO;
import sk.bais.dao.IndexRecordDAO;
import sk.bais.dao.MarkDAO;
import sk.bais.dao.StudentDAO;
import sk.bais.model.Enrollment;
import sk.bais.model.IndexRecord;
import sk.bais.model.Mark;
import sk.bais.model.Student;

/**
 * Service vrstva pre studenta.
 *
 * Pravidlo: GUI (presentation vrstva) NIKDY nevolá DAO priamo.
 * Vždy ide cez Service, ktorý:
 *   1. skontroluje oprávnenia (cez AuthContext)
 *   2. vykoná biznis logiku
 *   3. zaloguje akciu
 *   4. zavolá DAO
 */
@RequiredArgsConstructor  // Lombok: generuje konstruktor pre final polia
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    // Injektujeme DAO cez konstruktor — nie new() priamo v metóde
    private final StudentDAO studentDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final MarkDAO markDAO;
    private final IndexRecordDAO indexRecordDAO;
    
    // Zoznam všetkých študentov — len ADMIN a POWER_USER
    public List<Student> getAllStudents(AuthContext ctx) {
        if (!ctx.hasPermission("users:read")) {
            log.warn("Zamietnutý prístup k zoznamu študentov pre užívateľa id={}", ctx.getUserId());
            return Collections.emptyList();
        }
        try {
            List<Student> students = studentDAO.list();
            log.info("Načítaných {} študentov, požiadavka od userId={}", students.size(), ctx.getUserId());
            return students;
        } catch (SQLException e) {
            log.error("Chyba pri načítaní zoznamu študentov", e);
            return Collections.emptyList();
        }
    }

    
    // Detail študenta — student vidí len seba, admin/teacher vidí kohokoľvek
    public Optional<Student> getStudent(int studentId, AuthContext ctx) {
        boolean isSelf = ctx.getUserId() == studentId;
        boolean canReadOthers = ctx.hasPermission("users:read");

        if (!isSelf && !canReadOthers) {
            log.warn("Zamietnutý prístup: userId={} sa pokúša čítať profil studentId={}", ctx.getUserId(), studentId);
            return Optional.empty();
        }
        try {
            return studentDAO.getById(studentId);
        } catch (SQLException e) {
            log.error("Chyba pri načítaní študenta id={}", studentId, e);
            return Optional.empty();
        }
    }

    
    // Zápis na predmet — len STUDENT, len sám seba
    public Optional<Enrollment> enrollInSubject(int subjectId, int semesterId, AuthContext ctx) {
        if (!ctx.hasPermission("enrollments:write")) {
            log.warn("Zamietnutý zápis na predmet: userId={} nemá enrollments:write", ctx.getUserId());
            return Optional.empty();
        }

        try {
            // Biznis pravidlo: student sa nemôže zapísať dvakrát na rovnaký predmet v rovnakom semestri
            List<Enrollment> existing = enrollmentDAO.listByStudent(ctx.getUserId());
            boolean alreadyEnrolled = existing.stream()
                    .anyMatch(e -> e.getSubjectId() == subjectId
                                && e.getSemesterId() == semesterId
                                && e.getStatus() == Enrollment.Status.ACTIVE);
            if (alreadyEnrolled) {
                log.warn("Študent userId={} je už zapísaný na subjectId={}", ctx.getUserId(), subjectId);
                return Optional.empty();
            }

            Enrollment enrollment = new Enrollment(ctx.getUserId(), subjectId, semesterId, 1, Enrollment.Status.ACTIVE);
            Enrollment created = enrollmentDAO.create(enrollment);
            log.info("Študent userId={} sa zapísal na subjectId={} v semestri={}", ctx.getUserId(), subjectId, semesterId);
            return Optional.of(created);

        } catch (SQLException e) {
            log.error("Chyba pri zápise na predmet subjectId={}", subjectId, e);
            return Optional.empty();
        }
    }

    
    // Moje zápisy — student vidí len svoje
    public List<Enrollment> getMyEnrollments(AuthContext ctx) {
        try {
            return enrollmentDAO.listByStudent(ctx.getUserId());
        } catch (SQLException e) {
            log.error("Chyba pri načítaní zápisov pre userId={}", ctx.getUserId(), e);
            return Collections.emptyList();
        }
    }

    
    // Moje známky — student vidí len svoje  potom vymazat alebo zmenit 
    public List<Mark> getMyMarks(int enrollmentId, AuthContext ctx) {
        try {
            // Overíme že enrollment patrí tomuto studentovi
            Optional<Enrollment> enrollment = enrollmentDAO.getById(enrollmentId);
            if (enrollment.isEmpty() || enrollment.get().getStudentId() != ctx.getUserId()) {
                log.warn("Zamietnutý prístup k známkam: userId={} enrollmentId={}", ctx.getUserId(), enrollmentId);
                return Collections.emptyList();
            }
            return markDAO.listByEnrollment(enrollmentId);
        } catch (SQLException e) {
            log.error("Chyba pri načítaní známok pre enrollmentId={}", enrollmentId, e);
            return Collections.emptyList();
        }
    }
    // Ziska vsetky znamky pre daneho studenta
    public List<IndexRecord> getMyFinalMarks(AuthContext ctx) {
        if (!ctx.hasPermission("marks:read")) {
            return Collections.emptyList();
        }
        try {
            // studentId berieme z kontextu prihláseného užívateľa[cite: 9]
            return indexRecordDAO.listByStudentId(ctx.getUserId());
        } catch (SQLException e) {
            log.error("Chyba pri načítaní známok z indexu", e);
            return Collections.emptyList();
        }
    }
}