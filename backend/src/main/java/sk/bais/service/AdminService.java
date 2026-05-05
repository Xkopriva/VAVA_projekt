package sk.bais.service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import sk.bais.auth.AuthContext;
import sk.bais.dao.SemesterDAO;
import sk.bais.dao.SubjectDAO;
import sk.bais.dao.SubjectTranslationDAO;
import sk.bais.dao.UserDAO;
import sk.bais.model.Semester;
import sk.bais.model.Subject;
import sk.bais.model.SubjectTranslation;
import sk.bais.model.User;


/**
 * Service vrstva pre Administrátora.
 * Admin zodpovedá za správu používateľov, priraďovanie systémových rolí,
 * deaktiváciu účtov a dohľad nad bezpečnosťou (hashovanie hesiel).
 */
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final UserDAO userDAO;
    private final SubjectDAO subjectDAO;
    private final SemesterDAO semesterDAO;
    private final SubjectTranslationDAO subjectTranslationDAO;

    // Registruje noveho pouzivatela so zahashovanym heslom a priradi mu rolu
    public Optional<User> createUser(User newUser, String plainTextPassword, String roleName, AuthContext ctx) {
        if (!ctx.hasPermission("users:manage")) {
            log.warn("Zamietnutý pokus o vytvorenie používateľa: userId={} nemá oprávnenie", ctx.getUserId());
            return Optional.empty();
        }

        try {
            // Hashovanie hesla pomocou BCrypt pred uložením
            String hashedPassword = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
            newUser.setPasswordHash(hashedPassword);
            newUser.setActive(true);

            User created = userDAO.create(newUser);
            
            // Pridanie role Uzivatelovi
            userDAO.assignRole(created.getId(), roleName); 

            log.info("Admin id={} vytvoril používateľa: {} s rolou={}", 
                    ctx.getUserId(), created.getEmail(), roleName);
            return Optional.of(created);

        } catch (SQLException e) {
            log.error("Chyba pri vytváraní používateľa: email={}", newUser.getEmail(), e);
            return Optional.empty();
        }
    }

    // CREATE SUBJECT
    public boolean createSubject(Subject subject, String name, String locale, String description, AuthContext ctx) {
        if (!ctx.hasPermission("subjects:write")) {
            log.warn("Zamietnutý prístup k vytvoreniu predmetu pre userId={}", ctx.getUserId());
            return false;
        }

        try {
            // 1. Nastavíme autora z kontextu (v JSONe to už byť nemusí)
            subject.setCreatedBy(ctx.getUserId());

            // 2. Vytvoríme základný predmet v tabuľke 'subject'
            Subject createdSubject = subjectDAO.create(subject);

            // 3. Vytvoríme preklad (názov predmetu)
            SubjectTranslation translation = new SubjectTranslation(
                createdSubject.getId(), // ID, ktoré pridelila DB
                locale,                 // napr. 'sk'
                name,                   // Názov z JSONu
                description                    // Popis celeho predmetu
            );

            subjectTranslationDAO.create(translation); 

            log.info("Predmet {} vytvorený s ID {} a názvom '{}' v jazyku {}", 
                    subject.getCode(), createdSubject.getId(), name, locale);
            return true;

        } catch (SQLException e) {
            log.error("Chyba pri komplexnom vytváraní predmetu", e);
            return false;
        }
    }

    // DELETE SUBJECT
    public boolean removeSubject(int subjectId, AuthContext ctx) {
        if (!ctx.hasPermission("subjects:delete")) return false;
        try {
            return subjectDAO.delete(subjectId);
        } catch (SQLException e) {
            log.error("Chyba pri mazaní predmetu id={}", subjectId, e);
            return false;
        }
    }

    // CREATE SEMESTER
    public boolean createSemester(Semester semester, AuthContext ctx) {
        if (!ctx.hasPermission("subjects:manage")) {
            log.warn("Zamietnutý prístup k správe semestrov pre userId={}", ctx.getUserId());
            return false;
        }
        try {
            semesterDAO.create(semester);
            log.info("Admin userId={} vytvoril semester {}", ctx.getUserId(), semester.getCode());
            return true;
        } catch (SQLException e) {
            log.error("Chyba pri vytváraní semestra", e);
            return false;
        }
    }

    /**
     * Opätovne aktivuje deaktivované konto používateľa.
     */
    public boolean activateUser(int targetUserId, AuthContext ctx) {
        if (!ctx.hasPermission("users:manage")) {
            log.warn("Zamietnutý pokus o aktiváciu používateľa: userId={}", ctx.getUserId());
            return false;
        }

        try {
            boolean success = userDAO.setActive(targetUserId, true);
            if (success) {
                log.info("Admin id={} aktivoval používateľa id={}", ctx.getUserId(), targetUserId);
            }
            return success;
        } catch (SQLException e) {
            log.error("Chyba pri aktivácii používateľa id={}", targetUserId, e);
            return false;
        }
    }

    /**
     * Odstráni garanta z predmetu (nastaví guarantor_id na NULL).
     */
    public boolean removeGuarantor(int subjectId, AuthContext ctx) {
        if (!ctx.hasPermission("subjects:manage")) {
            log.warn("Zamietnutý pokus o odstránenie garanta: userId={}", ctx.getUserId());
            return false;
        }

        try {
            Optional<Subject> subjectOpt = subjectDAO.getById(subjectId);
            if (subjectOpt.isEmpty()) {
                log.error("Nepodarilo sa odstrániť garanta: Predmet id={} neexistuje", subjectId);
                return false;
            }

            Subject subject = subjectOpt.get();
            Integer oldGuarantor = subject.getGuarantorId();
            subject.setGuarantorId(null); // Odstránenie garanta

            boolean success = subjectDAO.update(subject);
            if (success) {
                log.info("Admin id={} odstránil garanta (pôvodne id={}) z predmetu id={}", 
                        ctx.getUserId(), oldGuarantor, subjectId);
            }
            return success;

        } catch (SQLException e) {
            log.error("Chyba pri odstraňovaní garanta z predmetu id={}", subjectId, e);
            return false;
        }
    }

    // Deaktivuje uzivatela v DB
    public boolean deactivateUser(int targetUserId, AuthContext ctx) {
        if (!ctx.hasPermission("users:manage")) {
            log.warn("Zamietnutá deaktivácia: userId={}", ctx.getUserId());
            return false;
        }

        // Ochrana: Admin nemôže deaktivovať sám seba
        if (ctx.getUserId() == targetUserId) {
            log.warn("Admin id={} sa pokúsil deaktivovať svoj vlastný účet. Operácia zamietnutá.", ctx.getUserId());
            return false;
        }

        try {
            return userDAO.setActive(targetUserId, false);
        } catch (SQLException e) {
            log.error("Chyba pri deaktivácii používateľa id={}", targetUserId, e);
            return false;
        }
    }

    // ASSIGN ROLE (používa UserDAO.assignRole alebo UserRoleDAO.assign)
    public boolean assignRole(int targetUserId, String roleName, AuthContext ctx) {
        if (!ctx.hasPermission("users:manage")) return false;
        try {
            // Použijeme UserDAO, ktorý priraďuje rolu podľa názvu
            userDAO.assignRole(targetUserId, roleName);
            log.info("Admin userId={} priradil rolu {} používateľovi {}", ctx.getUserId(), roleName, targetUserId);
            return true;
        } catch (SQLException e) {
            log.error("Chyba pri priraďovaní roly", e);
            return false;
        }
    }

    public List<User> getAllUsers(AuthContext ctx) {
        if (!ctx.hasPermission("users:read")) {
            log.warn("Zamietnutý prístup k zoznamu: userId={}", ctx.getUserId());
            return Collections.emptyList();
        }

        try {
            // 1. Získame základný zoznam userov
            List<User> users = userDAO.list();

            // 2. Pre každého usera dohráme jeho role z prepojovacej tabuľky
            for (User user : users) {
                // Tu voláme tvoju existujúcu metódu z UserDAO, ktorá robí JOIN na role
                List<String> roles = userDAO.getUserRoles(user.getId());
                user.setRoles(roles); 
            }

            return users;
        } catch (SQLException e) {
            log.error("Chyba pri načítaní používateľov s rolami", e);
            return Collections.emptyList();
        }
    }

    /**
     * Priradí učiteľa ako garanta pre konkrétny predmet.
     */
    public boolean assignGuarantor(int teacherId, int subjectId, AuthContext ctx) {
        if (!ctx.hasPermission("subjects:manage")) {
            log.warn("Zamietnutý pokus o priradenie garanta: userId={} nemá oprávnenie subjects:manage", ctx.getUserId());
            return false;
        }

        try {
            Optional<Subject> subjectOpt = subjectDAO.getById(subjectId);
            
            if (subjectOpt.isEmpty()) {
                log.error("Nepodarilo sa priradiť garanta: Predmet id={} neexistuje", subjectId);
                return false;
            }

            Subject subject = subjectOpt.get();
            subject.setGuarantorId(teacherId); 

            boolean success = subjectDAO.update(subject);

            if (success) {
                log.info("Admin id={} úspešne nastavil učiteľa id={} ako garanta predmetu id={}", 
                        ctx.getUserId(), teacherId, subjectId);
            }
            return success;

        } catch (SQLException e) {
            log.error("Databázová chyba pri priraďovaní garanta učiteľovi id={} k predmetu id={}", teacherId, subjectId, e);
            return false;
        }
    }

    /**
     * Vymaže predmet z DB. Mame v DB delete on cascade
     */
    public boolean removeSubject(int subjectId) {
        log.info("Admin inicioval vymazanie predmetu ID: {}", subjectId);
        try {
            return subjectDAO.delete(subjectId);
        } catch (Exception e) {
            log.error("Zlyhalo vymazanie predmetu: {}", e.getMessage());
            return false;
        }
    }
}