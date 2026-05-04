package sk.bais.core;

import java.net.InetSocketAddress;
import java.util.List;
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import sk.bais.auth.AuthContext;
import sk.bais.auth.AuthService;
import sk.bais.dao.UserDAO;
import sk.bais.dto.CalendarItemDTO;
import sk.bais.dto.UserProfileDTO;
import sk.bais.model.Enrollment;
import sk.bais.model.IndexRecord;
import sk.bais.model.Mark;
import sk.bais.model.Notification;
import sk.bais.model.Task;
import sk.bais.model.TaskSubmission;
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
    private final UserDAO userDAO;

    // Mapa aktívnych sessions: každé WebSocket spojenie má svoj AuthContext
    private final Map<WebSocket, AuthContext> sessions = new ConcurrentHashMap<>();

    public BaisWebSocketServer(int port, AuthService authService, StudentService studentService, 
                            TeacherService teacherService, AdminService adminService, UserDAO userDAO) {
    super(new InetSocketAddress(port));
    this.authService = authService;
    this.studentService = studentService;
    this.teacherService = teacherService; 
    this.adminService = adminService;
    this.userDAO = userDAO;
    mapper.registerModule(new JavaTimeModule());  
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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
                case "ACTIVATE_USER" -> handleActivateUser(conn, payload);
                case "REMOVE_GUARANTOR" -> handleRemoveGuarantor(conn, payload);
                

                // --- TEACHER AKCIE ---
                case "GET_MY_SUBJECTS" -> handleGetTeacherSubjects(conn);

                case "ADD_MARK" -> handleAddMark(conn, payload);
                case "UPDATE_MARK" -> handleUpdateMark(conn, payload);
                case "DELETE_MARK" -> handleDeleteMark(conn, payload);

                case "GET_MARKS_FOR_ENROLLMENT" -> handleGetIndexRecordForEnrollment(conn, payload);
                case "GET_POINTS_ENROLLMENT" -> handleGetPointsForEnrollment(conn, payload);
                case "GET_ENROLLMENTS_FOR_SUBJECT" -> handleGetEnrollmentsForSubject(conn, payload);

                case "CREATE_ENROLLMENT" -> handleCreateEnrollment(conn, payload);
                case "UPDATE_ENROLLMENT" -> handleUpdateEnrollment(conn, payload);
                case "DELETE_ENROLLMENT" -> handleDeleteEnrollment(conn, payload);

                case "CREATE_TASK" -> handleCreateTask(conn, payload);
                case "GRADE_SUBMISSION" -> handleGradeSubmission(conn, payload);
                case "RECORD_FINAL_MARK" -> handleRecordFinalMark(conn, payload);
                
                // --- STUDENT AKCIE ---
                case "ENROLL_SUBJECT" -> handleEnrollSubject(conn, payload);
                case "GET_MY_ENROLLMENTS" -> handleGetMyEnrollments(conn);
                case "GET_MY_MARKS" -> handleGetMyMarks(conn);
                case "GET_MY_POINTS" -> handleGetMyPoints(conn, payload);
                case "GET_MY_EVENTS" -> handleGetMyEvents(conn, payload);
                case "GET_MY_CALENDAR" -> handleGetMyCalendar(conn, payload);

                case "GET_ALL_NOTIFICATIONS" -> handleGetNotifications(conn);
                case "GET_UNDREAD_NOTIFICATIONS" -> handleGetUnreadNotifications(conn);
                case "MARK_READ_NOTIFICATION" -> handleMarkRead(conn, payload);
                case "MARK_ALL_UNREAD" -> handleMarkAllRead(conn);

                case "GET_MY_TASKS" -> handleGetMyTasks(conn);
                case "GET_TASK_DETAIL" -> handleGetTaskDetail(conn, payload);
                case "SUBMIT_TASK" -> handleSaveSubmission(conn, payload);
                
                // --- SPOLOCNE AKCIE ---
                case "GET_USER_PROFILE" -> handleGetUserProfile(conn);
                case "CREATE_NOTIFICATION" -> handleCreateNotification(conn, payload);

                


                default             -> sendError(conn, "Neznáma akcia: " + action);
            }
        } catch (Exception e) {
            log.error("Chyba pri spracovaní správy", e);
            sendError(conn, "Neplatný formát správy");
        }
    }

    private void handleCreateNotification(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (!payload.hasNonNull("title") || !payload.hasNonNull("message")) {
                sendError(conn, "Chýba title alebo message");
                return;
            }
            String title = payload.get("title").asText();
            String message = payload.get("message").asText();
            teacherService.createBroadcastNotification(title, message, ctx);
            sendResponse(conn, "NOTIFICATION_CREATED", null);
            
            // Push to connected clients using the correct active connections
            for (Map.Entry<WebSocket, AuthContext> entry : sessions.entrySet()) {
                if (entry.getValue().hasRole("STUDENT")) {
                    sendResponse(entry.getKey(), "NEW_NOTIFICATION", null);
                }
            }
        });
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

    private void handleGetMyEvents(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            String locale = payload.has("locale") ? payload.get("locale").asText() : "sk";
            
            // Všetka špina je teraz schovaná v servise
            List<Map<String, Object>> events = studentService.getMyEventsSimpleList(ctx, locale);
            
            sendResponse(conn, "MY_EVENTS_LIST", events);
        });
    }

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

    private void handleGetMyCalendar(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            // Jazyk skúsime vytiahnuť z payloadu alebo použijeme default
            String locale = payload.has("locale") ? payload.get("locale").asText() : "sk";
            
            List<CalendarItemDTO> calendar = studentService.getMyCalendar(ctx, locale);
            
            log.info("Posielam kalendár pre študenta {} (položiek: {})", ctx.getUserId(), calendar.size());
            sendResponse(conn, "MY_CALENDAR_EVENTS", calendar);
        });
    }

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
    // GET_MY_ENROLLMENTS
    private void handleGetMyEnrollments(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            var enrollments = studentService.getMyEnrollments(ctx);
            sendResponse(conn, "MY_ENROLLMENTS", enrollments);
        });
    }

    // GET_MY_MARKS (vráti List<IndexRecord>) teda studentove znamky
    private void handleGetMyMarks(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            var finalMarks = studentService.getMyFinalMarks(ctx);
            
            if (finalMarks != null) {
                sendResponse(conn, "MY_INDEX_RECORDS", finalMarks);
            } else {
                sendError(conn, "Nepodarilo sa načítať záznamy z indexu");
            }
        });
    }
    
    // GET_MY_POINTS (vráti List<Mark> pre daný enrollment) cize body v danom enrollmente
    private void handleGetMyPoints(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            // Validácia vstupu z payloadu
            if (payload == null || !payload.has("enrollmentId")) {
                sendError(conn, "Chýba enrollmentId");
                return;
            }

            int enrollmentId = payload.get("enrollmentId").asInt();
            var points = studentService.getMyPoints(enrollmentId, ctx);
            
            if (points != null) {
                sendResponse(conn, "MY_POINTS_LIST", points);
            } else {
                sendError(conn, "Nepodarilo sa načítať body pre daný zápis");
            }
        });
    }

    // GET_ALL_NOTIFICATIONS Zoznam všetkých notifikácií
    private void handleGetNotifications(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            List<Notification> list = studentService.getMyNotifications(ctx);
            sendResponse(conn, "NOTIFICATIONS_LIST", list);
        });
    }

    // GET_UNDREAD_NOTIFICATIONS Zoznam len notifikacii
    private void handleGetUnreadNotifications(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            List<Notification> list = studentService.getMyUnreadNotifications(ctx);
            sendResponse(conn, "UNREAD_NOTIFICATIONS_LIST", list);
        });
    }

    // MARK_READ_NOTIFICATION Označenie jednej notifikácie za precitanu
    private void handleMarkRead(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (!payload.has("id")) {
                sendError(conn, "Chýba ID notifikácie");
                return;
            }
            int id = payload.get("id").asInt();
            boolean success = studentService.markNotificationAsRead(id, ctx);
            
            if (success) {
                sendResponse(conn, "NOTIFICATION_MARKED_READ", Map.of("id", id));
            } else {
                sendError(conn, "Nepodarilo sa označiť notifikáciu za prečítanú");
            }
        });
    }

    // MARK_ALL_UNREAD oznacit vsetky notifikacie za precitane
    private void handleMarkAllRead(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            int count = studentService.markAllMyNotificationsAsRead(ctx);
            sendResponse(conn, "ALL_NOTIFICATIONS_MARKED_READ", Map.of("count", count));
        });
    }

    // GET_MY_TASKS Získanie zoznamu úloh
    private void handleGetMyTasks(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                List<Task> tasks = studentService.getMyTasks(ctx);
                sendResponse(conn, "MY_TASKS_LIST", tasks);
            } catch (Exception e) {
                log.error("Fatal error v handleGetMyTasks", e);
                sendError(conn, "Nepodarilo sa načítať zoznam úloh");
            }
        });
    }

    // GET_TASK_DETAIL Detail konkrétnej úlohy a odovzdania
    private void handleGetTaskDetail(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                int taskId = payload.get("taskId").asInt();
                Map<String, Object> detail = studentService.getTaskDetail(taskId, ctx);
                sendResponse(conn, "TASK_DETAIL", detail);
            } catch (Exception e) {
                log.error("Chyba v handleGetTaskDetail pre task {}", payload.get("taskId"), e);
                sendError(conn, "Nepodarilo sa načítať detail úlohy");
            }
        });
    }

    // SUBMIT_TASK Odoslanie (Submit) úlohy
    private void handleSaveSubmission(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                int taskId = payload.get("taskId").asInt();
                String content = payload.has("content") ? payload.get("content").asText() : null;
                String fileUrl = payload.has("fileUrl") ? payload.get("fileUrl").asText() : null;

                Optional<TaskSubmission> res = studentService.submitTask(taskId, content, fileUrl, ctx);
                if (res.isPresent()) {
                    sendResponse(conn, "SUBMISSION_SAVED", res.get());
                } else {
                    sendError(conn, "Odovzdanie sa nepodarilo uložiť");
                }
            } catch (Exception e) {
                log.error("Kritická chyba pri ukladaní submission", e);
                sendError(conn, "Chyba na strane servera pri ukladaní");
            }
        });
    }


    // -------------------------------------------------------------------------
    // Spolocne handlery
    // -------------------------------------------------------------------------

    private void handleGetUserProfile(WebSocket conn) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                var userOpt = userDAO.getById(ctx.getUserId());
                if (userOpt.isPresent()) {
                    var u = userOpt.get();
                    UserProfileDTO dto = new UserProfileDTO(
                            u.getId(),
                            u.getEmail(),
                            u.getFirstName() != null ? u.getFirstName() : "",
                            u.getLastName()  != null ? u.getLastName()  : ""
                    );
                    sendResponse(conn, "USER_PROFILE", dto);
                } else {
                    sendError(conn, "Používateľ nebol nájdený");
                }
            } catch (Exception e) {
                log.error("Chyba pri načítaní profilu pre userId={}", ctx.getUserId(), e);
                sendError(conn, "Nepodarilo sa načítať profil");
            }
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

    private void handleGetEnrollmentsForSubject(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (payload == null || !payload.has("subjectId")) {
                sendError(conn, "Chýba subjectId");
                return;
            }
            int subjectId = payload.get("subjectId").asInt();
            var enrollments = teacherService.getEnrollmentsForSubject(subjectId, ctx);
            
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("subjectId", subjectId);
            resp.put("enrollments", enrollments);
            sendResponse(conn, "SUBJECT_ENROLLMENTS", resp);
        });
    }

    // GET_POINTS_ENROLLMENT
    private void handleGetPointsForEnrollment(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (payload == null || !payload.has("enrollmentId")) {
                sendError(conn, "Chýba enrollmentId");
                return;
            }

            int eid = payload.get("enrollmentId").asInt();
            var points = teacherService.getPointsForEnrollment(eid, ctx);
            
            // Vrátime zoznam bodov (Mark objektov)
            sendResponse(conn, "ENROLLMENT_POINTS_LIST", points);
        });
    }

    // GET_MARKS_FOR_ENROLLMENT
    private void handleGetIndexRecordForEnrollment(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (payload == null || !payload.has("enrollmentId")) {
                sendError(conn, "Chýba enrollmentId");
                return;
            }

            int eid = payload.get("enrollmentId").asInt();
            var record = teacherService.getIndexRecordForEnrollment(eid, ctx);
            
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("enrollmentId", eid);
            resp.put("record", record.orElse(null));
            sendResponse(conn, "INDEX_RECORD_DETAIL", resp);
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

    // CREATE_ENROLLMENT
    private void handleCreateEnrollment(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            Enrollment e = mapper.convertValue(payload, Enrollment.class);
            teacherService.createEnrollment(e, ctx).ifPresentOrElse(
                created -> sendResponse(conn, "ENROLLMENT_CREATED", created),
                () -> sendError(conn, "Could not create enrollment")
            );
        });
    }

    // CREATE_TASK
    private void handleCreateTask(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            Task t = mapper.convertValue(payload, Task.class);
            teacherService.createTask(t, ctx).ifPresentOrElse(
                created -> sendResponse(conn, "TASK_CREATED", created),
                () -> sendError(conn, "Could not create task")
            );
        });
    }

    // GRADE_SUBMISSION
    private void handleGradeSubmission(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            int sid = payload.get("submissionId").asInt();
            TaskSubmission.Status status = TaskSubmission.Status.valueOf(payload.get("status").asText());
            boolean success = teacherService.gradeSubmission(sid, status, ctx);
            sendResponse(conn, "SUBMISSION_GRADED", success);
        });
    }

    // RECORD_FINAL_MARK 
    private void handleRecordFinalMark(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            IndexRecord record = mapper.convertValue(payload, IndexRecord.class);
            teacherService.recordFinalMark(record, ctx).ifPresentOrElse(
                created -> sendResponse(conn, "FINAL_MARK_RECORDED", created),
                () -> sendError(conn, "Could not record final mark")
            );
        });
    }

    /**
     * Handler pre UPDATE_ENROLLMENT
     */
    private void handleUpdateEnrollment(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                Enrollment e = mapper.convertValue(payload, Enrollment.class);
                boolean success = teacherService.updateEnrollment(e, ctx);
                if (success) {
                    sendResponse(conn, "ENROLLMENT_UPDATED", e);
                } else {
                    sendError(conn, "Nepodarilo sa upraviť zápis.");
                }
            } catch (Exception ex) {
                log.error("Chyba pri spracovaní UPDATE_ENROLLMENT", ex);
                sendError(conn, "Neplatné dáta pre zápis.");
            }
        });
    }

    /**
     * Handler pre DELETE_ENROLLMENT
     */
    private void handleDeleteEnrollment(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (!payload.has("id")) {
                sendError(conn, "Chýba ID zápisu pre vymazanie.");
                return;
            }
            
            int enrollmentId = payload.get("id").asInt();
            
            // Predpokladáme, že service metóda deleteEnrollment už obsahuje isGuarantor kontrolu
            boolean success = teacherService.deleteEnrollment(enrollmentId, ctx);
            
            if (success) {
                log.info("Enrollment {} úspešne vymazaný cez WS (userId={})", enrollmentId, ctx.getUserId());
                sendResponse(conn, "ENROLLMENT_DELETED", enrollmentId);
            } else {
                sendError(conn, "Nepodarilo sa vymazať zápis. Overte svoje oprávnenia.");
            }
        });
    }

    /**
     * Handler pre DELETE_MARK
     */
    private void handleDeleteMark(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (payload.has("markId")) {
                int mid = payload.get("markId").asInt();
                boolean success = teacherService.deleteMark(mid, ctx);
                if (success) {
                    sendResponse(conn, "MARK_DELETED", mid);
                } else {
                    sendError(conn, "Znamku sa nepodarilo vymazať.");
                }
            }
        });
    }

    /**
     * Handler pre UPDATE_MARK
     */
    private void handleUpdateMark(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            try {
                Mark m = mapper.convertValue(payload, Mark.class);
                boolean success = teacherService.updateMark(m, ctx);
                if (success) {
                    sendResponse(conn, "MARK_UPDATED", m);
                } else {
                    sendError(conn, "Nepodarilo sa upraviť známku.");
                }
            } catch (Exception ex) {
                log.error("Chyba pri spracovaní UPDATE_MARK", ex);
                sendError(conn, "Neplatné dáta pre známku.");
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
                String name = payload.get("name").asText();
                String locale = payload.get("locale").asText();
                String description = payload.get("description").asText();

                if (adminService.createSubject(subject, name, locale, description, ctx)) {
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

    /**
     * Handler pre ACTIVATE_USER
     */
    private void handleActivateUser(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (payload.has("userId")) {
                int targetId = payload.get("userId").asInt();
                boolean success = adminService.activateUser(targetId, ctx);
                
                if (success) {
                    sendResponse(conn, "USER_ACTIVATED", targetId);
                } else {
                    sendError(conn, "Nepodarilo sa aktivovať používateľa.");
                }
            }
        });
    }

    /**
     * Handler pre REMOVE_GUARANTOR
     */
    private void handleRemoveGuarantor(WebSocket conn, JsonNode payload) {
        requireAuth(conn).ifPresent(ctx -> {
            if (payload.has("subjectId")) {
                int subjectId = payload.get("subjectId").asInt();
                boolean success = adminService.removeGuarantor(subjectId, ctx);
                
                if (success) {
                    sendResponse(conn, "GUARANTOR_REMOVED", subjectId);
                } else {
                    sendError(conn, "Nepodarilo sa odstrániť garanta z predmetu.");
                }
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