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
import sk.bais.dao.NotificationDAO;
import sk.bais.dao.SubjectDAO;
import sk.bais.dao.TaskDAO;
import sk.bais.dao.TaskSubmissionDAO;
import sk.bais.model.Enrollment;
import sk.bais.model.IndexRecord;
import sk.bais.model.Mark;
import sk.bais.model.Notification;
import sk.bais.model.Subject;
import sk.bais.model.Task;
import sk.bais.model.TaskSubmission;

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
    private final NotificationDAO notificationDAO;
    private final TaskDAO taskDAO;
    private final TaskSubmissionDAO taskSubmissionDAO;

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
     * Upraví existujúce hodnotenie a informuje študenta.
     * Vyžaduje oprávnenie marks:write a učiteľ musí byť garantom predmetu.
     */
    public boolean updateMark(Mark mark, AuthContext ctx) {
        if (!ctx.hasPermission("marks:write")) {
            log.warn("Zamietnutý prístup k úprave známky pre userId={}", ctx.getUserId());
            return false;
        }
        try {
            // 1. Bezpečnostné overenie: Má učiteľ právo meniť známky pre tento enrollment?
            if (!isGuarantorOfEnrollment(mark.getEnrollmentId(), ctx)) {
                log.warn("Teacher {} nie je autorizovaný upraviť známku pre enrollment {}", ctx.getUserId(), mark.getEnrollmentId());
                return false;
            }

            // 2. Samotná aktualizácia v DB
            boolean updated = markDAO.update(mark);
            
            if (updated) {
                log.info("Teacher userId={} upravil známku id={}", ctx.getUserId(), mark.getId());
                
                // 3. Notifikácia pre študenta o zmene hodnotenia
                Optional<Enrollment> enrOpt = enrollmentDAO.getById(mark.getEnrollmentId());
                enrOpt.ifPresent(enr -> {
                    createNotification(
                        enr.getStudentId(), 
                        ctx.getUserId(), 
                        Notification.Type.MARK_ADDED,
                        "Zmena hodnotenia", 
                        "Vaše body za '" + mark.getTitle() + "' boli aktualizované.", 
                        mark.getId(), 
                        enr.getSubjectId(), 
                        mark.getTaskSubmissionId()
                    );
                });
            }
            return updated;
        } catch (SQLException e) {
            log.error("Chyba pri úprave známky id={}", mark.getId(), e);
            return false;
        }
    }

    /**
     * Upraví existujúci zápis (napr. zmena pokusu alebo manuálna zmena statusu).
     */
    public boolean updateEnrollment(Enrollment e, AuthContext ctx) {
        if (!ctx.hasPermission("enrollments:write")) {
            log.warn("Teacher {} nemá právo upravovať zápisy", ctx.getUserId());
            return false;
        }
        try {
            // Overenie, či učiteľ garantuje predmet daného zápisu
            if (!isGuarantorOfEnrollment(e.getId(), ctx)) return false;

            boolean updated = enrollmentDAO.update(e);
            if (updated) {
                log.info("Teacher {} upravil enrollment id={}", ctx.getUserId(), e.getId());
            }
            return updated;
        } catch (SQLException ex) {
            log.error("Chyba pri úprave enrollmentu {}", e.getId(), ex);
            return false;
        }
    }

    /**
     * Vymaže čiastkové hodnotenie (Mark).
     */
    public boolean deleteMark(int markId, AuthContext ctx) {
        if (!ctx.hasPermission("marks:write")) return false;
        try {
            Optional<Mark> markOpt = markDAO.getById(markId);
            if (markOpt.isEmpty()) return false;

            // Kontrola, či učiteľ môže manipulovať so známkami tohto enrollmentu
            if (!isGuarantorOfEnrollment(markOpt.get().getEnrollmentId(), ctx)) return false;

            boolean deleted = markDAO.delete(markId);
            if (deleted) {
                log.info("Teacher {} vymazal známku id={}", ctx.getUserId(), markId);
                // Tu by sa dala poslať notifikácia študentovi, že známka bola odstránená
            }
            return deleted;
        } catch (SQLException e) {
            log.error("Chyba pri mazaní známky id={}", markId, e);
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

    public Optional<Enrollment> createEnrollment(Enrollment e, AuthContext ctx) {
        if (!ctx.hasPermission("enrollments:write")) return Optional.empty();
        try {
            Enrollment created = enrollmentDAO.create(e);
            log.info("Teacher {} vytvoril zápis pre študenta {}", ctx.getUserId(), e.getStudentId());
            
            // Notifikácia pre študenta
            createNotification(e.getStudentId(), ctx.getUserId(), Notification.Type.ENROLLMENT_OPEN, 
                "Nový zápis", "Boli ste zapísaný na predmet.", null, e.getSubjectId(), null);
                
            return Optional.of(created);
        } catch (SQLException ex) {
            log.error("Chyba pri vytváraní zápisu", ex);
            return Optional.empty();
        }
    }

    public boolean deleteEnrollment(int id, AuthContext ctx) {
        if (!ctx.hasPermission("enrollments:write")) return false;
        try {
            return enrollmentDAO.delete(id);
        } catch (SQLException e) {
            log.error("Chyba pri mazaní zápisu id={}", id, e);
            return false;
        }
    }

    // --- TASKS ---

    public Optional<Task> createTask(Task t, AuthContext ctx) {
        if (!ctx.hasPermission("tasks:write")) return Optional.empty();
        try {
            t.setCreatedBy(ctx.getUserId());
            Task created = taskDAO.create(t);
            
            // Notifikácia všetkým študentom na predmete (zjednodušene)
            List<Enrollment> students = enrollmentDAO.list().stream()
                    .filter(e -> e.getSubjectId() == t.getSubjectId()).toList();
            for (Enrollment s : students) {
                createNotification(s.getStudentId(), ctx.getUserId(), Notification.Type.TASK_DUE,
                    "Nové zadanie: " + t.getTitle(), "Bolo pridané nové zadanie.", null, t.getSubjectId(), t.getId());
            }
            return Optional.of(created);
        } catch (SQLException e) {
            log.error("Chyba pri vytváraní úlohy", e);
            return Optional.empty();
        }
    }

    // --- GRADING SUBMISSIONS ---

    public boolean gradeSubmission(int submissionId, TaskSubmission.Status status, AuthContext ctx) {
        if (!ctx.hasPermission("marks:write")) return false;
        try {
            Optional<TaskSubmission> tsOpt = taskSubmissionDAO.getById(submissionId);
            if (tsOpt.isEmpty()) return false;
            
            TaskSubmission ts = tsOpt.get();
            ts.setStatus(status);
            ts.setGradedBy(ctx.getUserId());
            ts.setGradedAt(java.time.OffsetDateTime.now());
            
            boolean ok = taskSubmissionDAO.update(ts);
            if (ok) {
                createNotification(ts.getStudentId(), ctx.getUserId(), Notification.Type.MARK_ADDED,
                    "Zadanie ohodnotené", "Vaše odovzdané zadanie bolo skontrolované.", null, null, ts.getTaskId());
            }
            return ok;
        } catch (SQLException e) {
            log.error("Chyba pri hodnotení odovzdania id={}", submissionId, e);
            return false;
        }
    }

    // Helper pre notifikácie
    private void createNotification(int recipientId, Integer senderId, Notification.Type type, 
                                String title, String msg, Integer markId, Integer subId, Integer taskId) {
        try {
            Notification n = new Notification();
            n.setRecipientId(recipientId);
            n.setSenderId(senderId);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(msg);
            n.setRelatedMarkId(markId);
            n.setRelatedSubjectId(subId);
            n.setRelatedTaskId(taskId);
            notificationDAO.create(n);
        } catch (SQLException e) {
            log.error("Nepodarilo sa vytvoriť notifikáciu pre {}", recipientId, e);
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