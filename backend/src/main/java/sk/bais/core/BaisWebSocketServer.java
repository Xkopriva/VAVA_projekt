package sk.bais.core;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import sk.bais.auth.AuthContext;
import sk.bais.auth.AuthService;
import sk.bais.service.AdminService;
import sk.bais.service.StudentService;
import sk.bais.service.TeacherService;

public class BaisWebSocketServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(BaisWebSocketServer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final AuthService authService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AdminService adminService;    

    // Mapa aktívnych sessions: každé WebSocket spojenie má svoj AuthContext
    private final Map<WebSocket, AuthContext> sessions = new ConcurrentHashMap<>();

    public BaisWebSocketServer(int port, AuthService authService, StudentService studentService, 
                            TeacherService teacherService, AdminService adminService) {
    super(new InetSocketAddress(port));
    this.authService = authService;
    this.studentService = studentService;
    this.teacherService = teacherService; 
    this.adminService = adminService;   
    mapper.registerModule(new JavaTimeModule());  
}

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("Nové spojenie z: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        sessions.remove(conn);
        log.info("Spojenie uzavreté: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonNode json = mapper.readTree(message);

            // NULL CHECK – správa musí obsahovať pole "action"
            if (!json.hasNonNull("action")) {
                sendError(conn, "Chýba povinné pole 'action'");
                return;
            }

            String action = json.get("action").asText();
            // .path() vráti MissingNode namiesto null – bezpečnejšie ako .get()
            JsonNode payload = json.path("payload");

            switch (action) {
                case "LOGIN"        -> handleLogin(conn, payload);
                case "GET_STUDENTS" -> handleGetStudents(conn);

                // --- ADMIN AKCIE ---
                case "LIST_USERS" -> handleListUsers(conn);
                case "CREATE_USER" -> handleCreateUser(conn, payload);
                case "CREATE_SUBJECT" -> handleCreateSubject(conn, payload);
                case "CREATE_SEMESTER" -> handleCreateSemester(conn, payload);
                case "ASSIGN_ROLE" -> handleAssignRole(conn, payload);
                case "DEACTIVATE_USER" -> handleDeactivateUser(conn, payload);
                case "ASSIGN_GUARANTOR" -> handleAssignGuarantor(conn, payload);
                case "DELETE_SUBJECT" -> handleDeleteSubject(conn, payload);
                

                // --- TEACHER AKCIE ---
                case "GET_MY_SUBJECTS" -> handleGetTeacherSubjects(conn);
                case "ADD_MARK" -> handleAddMark(conn, payload);
                
                // --- STUDENT AKCIE ---
                case "ENROLL_SUBJECT" -> handleEnrollSubject(conn, payload);
                case "GET_MY_ENROLLMENTS" -> handleGetMyEnrollments(conn);




                default             -> sendError(conn, "Neznáma akcia: " + action);
            }
        } catch (Exception e) {
            log.error("Chyba pri spracovaní správy", e);
            sendError(conn, "Neplatný formát správy");
        }
    }

    // -------------------------------------------------------------------------
    // Pomocná metóda – centralizovaná autorizácia pre všetky chránené akcie
    // -------------------------------------------------------------------------
    private Optional<AuthContext> requireAuth(WebSocket conn) {
        AuthContext ctx = sessions.get(conn);
        if (ctx == null) {
            sendError(conn, "Vyžaduje sa prihlásenie");
            return Optional.empty();
        }
        return Optional.of(ctx);
    }

    // -------------------------------------------------------------------------
    // Handlery akcií
    // -------------------------------------------------------------------------

    private void handleLogin(WebSocket conn, JsonNode payload) {
        // Validácia – payload musí obsahovať email aj heslo
        if (!payload.hasNonNull("email") || !payload.hasNonNull("password")) {
            sendError(conn, "Chýba email alebo heslo");
            return;
        }

        String email    = payload.get("email").asText().trim();
        String password = payload.get("password").asText();

        if (email.isEmpty() || password.isEmpty()) {
            sendError(conn, "Email a heslo nemôžu byť prázdne");
            return;
        }

        Optional<AuthContext> auth = authService.login(email, password);
        if (auth.isPresent()) {
            sessions.put(conn, auth.get());
            sendResponse(conn, "LOGIN_SUCCESS", auth.get());
            log.info("Úspešné prihlásenie: {}", email);
        } else {
            log.warn("Neúspešné prihlásenie pre: {}", email);
            sendError(conn, "Nesprávne prihlasovacie údaje");
        }
    }

    private void handleGetStudents(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            var students = studentService.getAllStudents(ctx);
            sendResponse(conn, "STUDENTS_LIST", students);
        });
    }

    // -------------------------------------------------------------------------
    // Student Handlery
    // -------------------------------------------------------------------------

    private void handleEnrollSubject(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            int subjectId = payload.get("subjectId").asInt();
            int semesterId = payload.get("semesterId").asInt();
            var result = studentService.enrollInSubject(subjectId, semesterId, ctx);
            if (result.isPresent()) {
                sendResponse(conn, "ENROLLMENT_SUCCESS", result.get());
            } else {
                sendError(conn, "Zápis na predmet zlyhal");
            }
        });
    }

    private void handleGetMyEnrollments(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            var enrollments = studentService.getMyEnrollments(ctx);
            sendResponse(conn, "MY_ENROLLMENTS", enrollments);
        });
    }

    


    // -------------------------------------------------------------------------
    // Teacher Handlery
    // -------------------------------------------------------------------------

    private void handleGetTeacherSubjects(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            var subjects = teacherService.getMySubjects(ctx);
            sendResponse(conn, "TEACHER_SUBJECTS_LIST", subjects);
        });
    }

    private void handleAddMark(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                sk.bais.model.Mark mark = mapper.treeToValue(payload, sk.bais.model.Mark.class);
                var result = teacherService.addMark(mark, ctx);
                if (result.isPresent()) {
                    sendResponse(conn, "MARK_ADDED", result.get());
                } else {
                    sendError(conn, "Nepodarilo sa pridať známku");
                }
            } catch (Exception e) {
                sendError(conn, "Neplatné dáta pre známku");
            }
        });
    }


    // -------------------------------------------------------------------------
    // Admin Handlery
    // -------------------------------------------------------------------------

    // LIST_USERS
    private void handleListUsers(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            var users = adminService.getAllUsers(ctx);
            sendResponse(conn, "USERS_LIST", users);
        });
    }

    // CREATE_USER
    private void handleCreateUser(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                // Mapovanie JSONu na objekt User a vytiahnutie plain hesla
                sk.bais.model.User newUser = mapper.treeToValue(payload.get("user"), sk.bais.model.User.class);
                String password = payload.get("password").asText();
                String role = payload.get("role").asText();
                
                var result = adminService.createUser(newUser, password, role, ctx);
                if (result.isPresent()) {
                    sendResponse(conn, "USER_CREATED", result.get());
                } else {
                    sendError(conn, "Nepodarilo sa vytvoriť používateľa (chýbajúce práva alebo chyba v DB)");
                }
            } catch (Exception e) {
                sendError(conn, "Neplatné dáta pre vytvorenie používateľa");
            }
        });
    }

    // CREATE_SUBJECT 
    private void handleCreateSubject(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                sk.bais.model.Subject subject = mapper.treeToValue(payload.get("subject"), sk.bais.model.Subject.class);
                
                if (adminService.createSubject(subject, ctx)) {
                    sendResponse(conn, "SUBJECT_CREATED", subject);
                } else {
                    sendError(conn, "Nepodarilo sa vytvoriť predmet (práva/DB)");
                }
            } catch (Exception e) {
                sendError(conn, "Neplatné dáta pre predmet");
            }
        });
    }
    // CREATE_SEMESTER
    private void handleCreateSemester(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                sk.bais.model.Semester semester = mapper.treeToValue(payload.get("semester"), sk.bais.model.Semester.class);
                
                if (adminService.createSemester(semester, ctx)) {
                    sendResponse(conn, "SEMESTER_CREATED", semester);
                } else {
                    sendError(conn, "Nepodarilo sa vytvoriť semester");
                }
            } catch (Exception e) {
                sendError(conn, "Neplatné dáta pre semester");
            }
        });
    }


    // ASSIGN_ROLE
    private void handleAssignRole(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                int targetUserId = payload.get("userId").asInt();
                String role = payload.get("role").asText();

                if (adminService.assignRole(targetUserId, role, ctx)) {
                    sendResponse(conn, "ROLE_ASSIGNED", "Rola " + role + " priradená userovi " + targetUserId);
                } else {
                    sendError(conn, "Nepodarilo sa priradiť rolu");
                }
            } catch (Exception e) {
                sendError(conn, "Neplatné parametre pre ASSIGN_ROLE");
            }
        });
    }
    // ASSIGN_GUARANTOR
    private void handleAssignGuarantor(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                int teacherId = payload.get("teacherId").asInt();
                int subjectId = payload.get("subjectId").asInt();
                
                boolean success = adminService.assignGuarantor(teacherId, subjectId, ctx);

                if (success) {
                    sendResponse(conn, "GUARANTOR_ASSIGNED", Map.of(
                        "success", true,
                        "teacherId", teacherId,
                        "subjectId", subjectId
                    ));
                    log.info("Garant priradený: teacherId={}, subjectId={} (vykonal adminId={})", 
                            teacherId, subjectId, ctx.getUserId());
                } else {
                    sendError(conn, "Nepodarilo sa priradiť garanta (nedostatočné práva alebo neexistujúci predmet/učiteľ)");
                }
            } catch (Exception e) {
                log.error("Chyba pri spracovaní priradenia garanta", e);
                sendError(conn, "Neplatné dáta pre priradenie garanta (očakávané teacherId a subjectId)");
            }
        });
    }
    
    // DEACTIVATE_USER
    private void handleDeactivateUser(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            int targetId = payload.get("userId").asInt();
            boolean success = adminService.deactivateUser(targetId, ctx);
            sendResponse(conn, "USER_DEACTIVATED", Map.of("success", success, "userId", targetId));
        });
    }


    // DELETE_SUBJECT 
    private void handleDeleteSubject(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                int subjectId = payload.get("subjectId").asInt();
                
                if (adminService.removeSubject(subjectId, ctx)) {
                    sendResponse(conn, "SUBJECT_REMOVED", subjectId);
                } else {
                    sendError(conn, "Nepodarilo sa odstrániť predmet");
                }
            } catch (Exception e) {
                sendError(conn, "Neplatné ID predmetu");
            }
        });
    }



    // -------------------------------------------------------------------------
    // Pomocné metódy pre odosielanie odpovedí
    // -------------------------------------------------------------------------

    private void sendResponse(WebSocket conn, String type, Object data) {
        try {
            Map<String, Object> response = Map.of(
                "type", type,
                "data", data
            );
            conn.send(mapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Chyba pri odosielaní odpovede", e);
        }
    }

    private void sendError(WebSocket conn, String message) {
        sendResponse(conn, "ERROR", Map.of("message", message));
    }

    // -------------------------------------------------------------------------
    // WebSocket lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("WebSocket chyba pre spojenie {}: {}", conn.getRemoteSocketAddress(), ex.getMessage());
    }

    @Override
    public void onStart() {
        log.info("WebSocket server úspešne spustený na porte: {}", getPort());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }
}