package sk.bais.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.bais.auth.AuthContext;
import sk.bais.auth.AuthService;
import sk.bais.service.StudentService;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BaisWebSocketServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(BaisWebSocketServer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final AuthService authService;
    private final StudentService studentService;

    // Mapa aktívnych sessions: každé WebSocket spojenie má svoj AuthContext
    private final Map<WebSocket, AuthContext> sessions = new ConcurrentHashMap<>();

    public BaisWebSocketServer(int port, AuthService authService, StudentService studentService) {
        super(new InetSocketAddress(port));
        this.authService = authService;
        this.studentService = studentService;
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