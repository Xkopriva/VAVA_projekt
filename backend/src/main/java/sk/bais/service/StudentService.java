package sk.bais.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import sk.bais.auth.AuthContext;
import sk.bais.dao.EnrollmentDAO;
import sk.bais.dao.EventDAO;
import sk.bais.dao.EventTranslationDAO;
import sk.bais.dao.IndexRecordDAO;
import sk.bais.dao.MarkDAO;
import sk.bais.dao.StudentDAO;
import sk.bais.dao.SubjectDAO;
import sk.bais.dao.SubjectTranslationDAO;
import sk.bais.dto.EnrollmentWithSubjectDTO;
import sk.bais.dto.EventWithTranslationDTO;
import sk.bais.model.Enrollment;
import sk.bais.model.Event;
import sk.bais.model.EventTranslation;
import sk.bais.model.IndexRecord;
import sk.bais.model.Mark;
import sk.bais.model.Student;
import sk.bais.model.Subject;
import sk.bais.model.SubjectTranslation;

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
    private final SubjectDAO subjectDAO;
    private final SubjectTranslationDAO subjectTranslationDAO;
    private final EventDAO eventDAO;
    private final EventTranslationDAO eventTranslationDAO;
    
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

    /**
     * Vyčistená verzia pôvodného handleGetMyEvents.
     * Získava udalosti študenta transformované na jednoduchý zoznam pre UI.
     */
    public List<Map<String, Object>> getMyEventsSimpleList(AuthContext ctx, String locale) {
        try {
            // Využijeme už existujúcu čistú metódu getMyEnrollments[cite: 15]
            List<EnrollmentWithSubjectDTO> enrollments = getMyEnrollments(ctx);
            List<Map<String, Object>> result = new ArrayList<>();

            for (EnrollmentWithSubjectDTO enr : enrollments) {
                // Používame injektované eventDAO[cite: 13, 15]
                List<Event> events = eventDAO.listBySubject(enr.getSubjectId());
                
                for (Event ev : events) {
                    Map<String, Object> map = new HashMap<>();
                    
                    // Skúsime získať preložený titulok, inak použijeme typ[cite: 14]
                    String displayTitle = ev.getType() != null ? ev.getType().name() : "EVENT";
                    Optional<EventTranslation> trans = eventTranslationDAO.get(ev.getId(), locale);
                    if (trans.isPresent()) {
                        displayTitle = trans.get().getTitle();
                    }

                    map.put("title", displayTitle);
                    map.put("type", ev.getType() != null ? ev.getType().name() : "PREDNASKA");
                    map.put("subjectCode", enr.getSubjectCode());
                    
                    if (ev.getScheduledAt() != null) {
                        map.put("scheduledAt", ev.getScheduledAt().toString());
                    }
                    
                    map.put("durationMinutes", ev.getDurationMinutes() != null ? ev.getDurationMinutes() : 90);
                    result.add(map);
                }
            }
            return result;
        } catch (SQLException e) {
            log.error("Chyba v getMyEventsSimpleList pre studenta {}", ctx.getUserId(), e);
            return Collections.emptyList();
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

    
    /**
     * Moje zápisy — obohacené o polia predmetu (subjectCode, subjectName, credits).
     * Fronted (GradesController, ProgressController, CoursesController) potrebuje tieto
     * polia priamo v enrollment objekte.
     */
    public List<EnrollmentWithSubjectDTO> getMyEnrollments(AuthContext ctx) {
        try {
            List<Enrollment> raw = enrollmentDAO.listByStudent(ctx.getUserId());
            List<EnrollmentWithSubjectDTO> result = new ArrayList<>();

            for (Enrollment e : raw) {
                String code  = "";
                String name  = "";
                int    creds = 0;

                try {
                    Optional<Subject> subjectOpt = subjectDAO.getById(e.getSubjectId());
                    if (subjectOpt.isPresent()) {
                        Subject s = subjectOpt.get();
                        code  = s.getCode() != null ? s.getCode() : "";
                        name  = resolveSubjectName(s);
                        creds = s.getCredits();
                    }
                } catch (SQLException ex) {
                    log.warn("Nepodarilo sa načítať predmet subjectId={} pre enrollment id={}", e.getSubjectId(), e.getId());
                }

                result.add(new EnrollmentWithSubjectDTO(
                        e.getId(),
                        e.getStudentId(),
                        e.getSubjectId(),
                        e.getSemesterId(),
                        e.getAttemptNumber(),
                        e.getStatus() != null ? e.getStatus().name() : "ACTIVE",
                        code,
                        name,
                        creds
                ));
            }

            log.info("Načítaných {} zápisov (s predmetmi) pre userId={}", result.size(), ctx.getUserId());
            return result;
        } catch (SQLException e) {
            log.error("Chyba pri načítaní zápisov pre userId={}", ctx.getUserId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Vráti ľudsky čitateľný názov predmetu z tabuľky subject_translation.
     * Priorita: slovenský preklad → anglický preklad → kód predmetu.
     */
    private String resolveSubjectName(Subject s) {
        try {
            // Skús SK preklad
            java.util.Optional<SubjectTranslation> sk = subjectTranslationDAO.get(s.getId(), "sk");
            if (sk.isPresent() && sk.get().getName() != null && !sk.get().getName().isBlank()) {
                return sk.get().getName();
            }
            // Fallback EN preklad
            java.util.Optional<SubjectTranslation> en = subjectTranslationDAO.get(s.getId(), "en");
            if (en.isPresent() && en.get().getName() != null && !en.get().getName().isBlank()) {
                return en.get().getName();
            }
        } catch (Exception ex) {
            log.warn("Nepodarilo sa načítať preklad pre subjectId={}", s.getId());
        }
        // Posledný fallback — kód predmetu
        return s.getCode() != null ? s.getCode() : "Predmet " + s.getId();
    }

    // ziska BODY v enrollmente pre daneho studenta
    public List<Mark> getMyPoints(int enrollmentId, AuthContext ctx) {
    try {
        return markDAO.listByEnrollment(enrollmentId, ctx.getUserId());
    } catch (SQLException e) {
        log.error("Chyba pri načítaní bodov pre enrollment {}", enrollmentId, e);
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
    
    /**
     * Získa udalosti pre kalendár prihláseného študenta.
     * Vracia udalosti len pre predmety, na ktoré je študent zapísaný.
     */
    public List<EventWithTranslationDTO> getMyCalendarEvents(AuthContext ctx, String locale) {
        try {
            // 1. Získame ID predmetov, na ktoré je študent zapísaný[cite: 18]
            List<Enrollment> enrollments = enrollmentDAO.listByStudent(ctx.getUserId());
            List<Integer> mySubjectIds = enrollments.stream()
                    .map(Enrollment::getSubjectId)
                    .toList();

            List<EventWithTranslationDTO> calendar = new ArrayList<>();

            // 2. Pre každý predmet vytiahneme jeho udalosti[cite: 16]
            for (int subjectId : mySubjectIds) {
                List<Event> subjectEvents = eventDAO.listBySubject(subjectId);
                
                for (Event e : subjectEvents) {
                    // Ignorujeme nepublikované udalosti (ak frontend nemá byť admin)
                    if (!e.isPublished()) continue;

                    // 3. Získame preklad pre daný event a locale
                    String title = "Event " + e.getId();
                    String description = "";

                    Optional<EventTranslation> trans = eventTranslationDAO.get(e.getId(), locale);
                    if (trans.isPresent()) {
                        title = trans.get().getTitle();
                        description = trans.get().getDescription();
                    } else {
                        // Fallback na iný jazyk, ak požadovaný neexistuje
                        Optional<EventTranslation> fallback = eventTranslationDAO.get(e.getId(), "sk");
                        if (fallback.isPresent()) {
                            title = fallback.get().getTitle();
                            description = fallback.get().getDescription();
                        }
                    }

                    calendar.add(new EventWithTranslationDTO(e, title, description));
                }
            }
            return calendar;
        } catch (SQLException e) {
            log.error("Chyba pri načítaní kalendára pre študenta {}", ctx.getUserId(), e);
            return Collections.emptyList();
        }
    }

}