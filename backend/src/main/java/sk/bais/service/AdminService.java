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
import sk.bais.dao.SubjectDAO;
import sk.bais.dao.UserDAO;
import sk.bais.model.Subject;
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

    // Registruje noveho pouzivatela so zahashovanym heslom a priradi mu rolu
    public Optional<User> createUser(User newUser, String plainTextPassword, String roleName, AuthContext ctx) {
        if (!ctx.hasPermission("users:write")) {
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

    // Deaktivuje uzivatela v DB
    public boolean deactivateUser(int targetUserId, AuthContext ctx) {
        if (!ctx.hasPermission("users:write")) {
            log.warn("Zamietnutá deaktivácia: userId={}", ctx.getUserId());
            return false;
        }

        try {
            return userDAO.setActive(targetUserId, false);
        } catch (SQLException e) {
            log.error("Chyba pri deaktivácii používateľa id={}", targetUserId, e);
            return false;
        }
    }

    // Pre Admin prehlad, pridava moznost zobrazit vsetkych zaregistrovanych uzivatelov
    public List<User> getAllUsers(AuthContext ctx) {
        if (!ctx.hasPermission("users:read")) {
            log.warn("Zamietnutý prístup k zoznamu: userId={}", ctx.getUserId());
            return Collections.emptyList();
        }
        try {
            return userDAO.list();
        } catch (SQLException e) {
            log.error("Chyba pri načítaní používateľov", e);
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
}