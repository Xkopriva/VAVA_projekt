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
import sk.bais.dao.SubjectDAO;
import sk.bais.model.Enrollment;
import sk.bais.model.IndexRecord;
import sk.bais.model.Mark;
import sk.bais.model.Subject;

/**
 * Service vrstva pre učiteľa.
 * Teacher môže čítať predmety ktoré garantuje, vidieť zápisov študentov,
 * pridávať a upravovať známky a zapisovať finálne hodnotenia.
 */
@RequiredArgsConstructor
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    private final SubjectDAO subjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final MarkDAO markDAO;
    private final IndexRecordDAO indexRecordDAO;

    /**
     * Vráti predmety ktoré daný učiteľ garantuje (guarantor_id == ctx.userId).
     * Každý prihlásený užívateľ s rolou TEACHER môže vidieť svoje predmety.
     */
    public List<Subject> getMySubjects(AuthContext ctx) {
        if (!ctx.hasPermission("subjects:read")) {
            log.warn("Zamietnutý prístup k predmetom pre userId={}", ctx.getUserId());
            return Collections.emptyList();
        }
        try {
            List<Subject> all = subjectDAO.list();
            List<Subject> mine = all.stream()
                    .filter(s -> s.getGuarantorId() != null && s.getGuarantorId() == ctx.getUserId())
                    .toList();
            log.info("Teacher userId={} má {} predmetov", ctx.getUserId(), mine.size());
            return mine;
        } catch (SQLException e) {
            log.error("Chyba pri načítaní predmetov pre userId={}", ctx.getUserId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Vráti všetkých zapísaných študentov na daný predmet.
     * Teacher musí byť garantor predmetu — inak je prístup zamietnutý.
     */
    public List<Enrollment> getEnrollmentsForSubject(int subjectId, AuthContext ctx) {
        if (!ctx.hasPermission("enrollments:read")) {
            log.warn("Zamietnutý prístup k zápisom pre userId={}", ctx.getUserId());
            return Collections.emptyList();
        }
        try {
            // Biznis pravidlo: teacher vidí zápisov len pre predmety ktoré garantuje
            Optional<Subject> subject = subjectDAO.getById(subjectId);
            if (subject.isEmpty()) {
                log.warn("Predmet subjectId={} neexistuje", subjectId);
                return Collections.emptyList();
            }
            boolean isGuarantor = subject.get().getGuarantorId() != null
                    && subject.get().getGuarantorId() == ctx.getUserId();
            if (!isGuarantor && !ctx.hasRole("ADMIN")) {
                log.warn("Teacher userId={} nie je garantor subjectId={}", ctx.getUserId(), subjectId);
                return Collections.emptyList();
            }
            List<Enrollment> enrollments = enrollmentDAO.list().stream()
                    .filter(e -> e.getSubjectId() == subjectId)
                    .toList();
            log.info("Nacitanych {} zapisov pre subjectId={}", enrollments.size(), subjectId);
            return enrollments;
        } catch (SQLException e) {
            log.error("Chyba pri načítaní zápisov pre subjectId={}", subjectId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Pridá hodnotenie (Mark) k danému enrollment.
     * Vyžaduje oprávnenie marks:write.
     */
    public Optional<Mark> addMark(Mark mark, AuthContext ctx) {
        if (!ctx.hasPermission("marks:write")) {
            log.warn("Zamietnutý prístup k pridaniu známky pre userId={}", ctx.getUserId());
            return Optional.empty();
        }
        try {
            mark.setGivenBy(ctx.getUserId());
            Mark created = markDAO.create(mark);
            log.info("Teacher userId={} pridal známku id={} pre enrollmentId={}",
                    ctx.getUserId(), created.getId(), created.getEnrollmentId());
            return Optional.of(created);
        } catch (SQLException e) {
            log.error("Chyba pri pridávaní známky pre enrollmentId={}", mark.getEnrollmentId(), e);
            return Optional.empty();
        }
    }

    /**
     * Upraví existujúce hodnotenie.
     * Vyžaduje oprávnenie marks:write.
     */
    public boolean updateMark(Mark mark, AuthContext ctx) {
        if (!ctx.hasPermission("marks:write")) {
            log.warn("Zamietnutý prístup k úprave známky pre userId={}", ctx.getUserId());
            return false;
        }
        try {
            boolean updated = markDAO.update(mark);
            if (updated) log.info("Teacher userId={} upravil známku id={}", ctx.getUserId(), mark.getId());
            return updated;
        } catch (SQLException e) {
            log.error("Chyba pri úprave známky id={}", mark.getId(), e);
            return false;
        }
    }

    /**
     * Zapíše finálnu známku do indexu (IndexRecord).
     * Jeden enrollment môže mať maximálne jeden IndexRecord (UNIQUE constraint v DB).
     * Vyžaduje oprávnenie marks:write.
     */
    public Optional<IndexRecord> recordFinalMark(IndexRecord record, AuthContext ctx) {
        if (!ctx.hasPermission("marks:write")) {
            log.warn("Zamietnutý zápis finálnej známky pre userId={}", ctx.getUserId());
            return Optional.empty();
        }
        try {
            // Biznis pravidlo: enrollment nesmie mať už existujúci IndexRecord
            Optional<IndexRecord> existing = indexRecordDAO.getByEnrollment(record.getEnrollmentId());
            if (existing.isPresent()) {
                log.warn("Enrollment id={} už má finálnu známku", record.getEnrollmentId());
                return Optional.empty();
            }
            record.setRecordedBy(ctx.getUserId());
            IndexRecord created = indexRecordDAO.create(record);
            // Po zapísaní finálnej známky aktualizujeme status enrollmentu
            String mark = created.getFinalMark();
            Enrollment.Status newStatus = (mark.equals("FX") || mark.equals("FAIL"))
                    ? Enrollment.Status.FAILED
                    : Enrollment.Status.PASSED;
            enrollmentDAO.updateStatus(record.getEnrollmentId(), newStatus);
            log.info("Teacher userId={} zapísal finálnu známku {} pre enrollmentId={}",
                    ctx.getUserId(), mark, record.getEnrollmentId());
            return Optional.of(created);
        } catch (SQLException e) {
            log.error("Chyba pri zápise finálnej známky pre enrollmentId={}", record.getEnrollmentId(), e);
            return Optional.empty();
        }
    }

    /**
     * Vráti finálne hodnotenie z indexu pre daný enrollment.
     * Vyžaduje overenie, či je učiteľ garantom predmetu.
     */
    public Optional<IndexRecord> getIndexRecordForEnrollment(int enrollmentId, AuthContext ctx) {
        if (!ctx.hasPermission("marks:read")) return Optional.empty();
        
        try {
            // Bezpečnostná kontrola garantora
            if (!isGuarantorOfEnrollment(enrollmentId, ctx)) return Optional.empty();

            return indexRecordDAO.getByEnrollment(enrollmentId);
        } catch (SQLException e) {
            log.error("Chyba pri načítaní index recordu pre enrollmentId={}", enrollmentId, e);
            return Optional.empty();
        }
    }

    /**
     * Vráti všetky čiastkové body (Mark) pre daný enrollment.
     * Vyžaduje overenie, či je učiteľ garantom predmetu.
     */
    public List<Mark> getPointsForEnrollment(int enrollmentId, AuthContext ctx) {
        if (!ctx.hasPermission("marks:read")) return Collections.emptyList();
        
        try {
            // Kontrola, či je učiteľ garantom
            if (!isGuarantorOfEnrollment(enrollmentId, ctx)) {
                log.warn("Teacher {} nie je autorizovaný pre enrollment {}", ctx.getUserId(), enrollmentId);
                return Collections.emptyList();
            }

            // Voláme metódu bez potreby studentId
            return markDAO.listByEnrollment(enrollmentId);
        } catch (SQLException e) {
            log.error("Chyba pri načítaní bodov pre enrollmentId={}", enrollmentId, e);
            return Collections.emptyList();
        }
    }



    // Pomocná metóda na overenie práv k enrollmentu
    private boolean isGuarantorOfEnrollment(int enrollmentId, AuthContext ctx) throws SQLException {
        if (ctx.hasRole("ADMIN")) return true;

        Optional<Enrollment> enrollmentOpt = enrollmentDAO.getById(enrollmentId);
        if (enrollmentOpt.isEmpty()) return false;

        Optional<Subject> subjectOpt = subjectDAO.getById(enrollmentOpt.get().getSubjectId());
        if (subjectOpt.isEmpty()) return false;

        Subject s = subjectOpt.get();
        return s.getGuarantorId() != null && s.getGuarantorId() == ctx.getUserId();
    }
}