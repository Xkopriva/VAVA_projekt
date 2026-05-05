package com.example.bais.services;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WebSocketClientService extends WebSocketClient {

    //Singleton
    private static WebSocketClientService instance;

    public static synchronized WebSocketClientService getInstance() {
        if (instance == null) {
            try {
                instance = new WebSocketClientService(new URI("ws://localhost:8887"));
                instance.setConnectionLostTimeout(60);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return instance;
    }

    // Polia
    private final ObjectMapper mapper = new ObjectMapper();
    private CompletableFuture<Void> connectFuture;

    private final Map<String, List<ListenerEntry>> listeners = new ConcurrentHashMap<>();

    private record ListenerEntry(String id, Consumer<JsonNode> handler) {}

    private WebSocketClientService(URI uri) {
        super(
            uri, 
            new Draft_6455(),
            null,
            5000
        );
    }

    //Pripojenie

    public CompletableFuture<Void> connectAsync() {
        if (isOpen()) return CompletableFuture.completedFuture(null);
        if (connectFuture != null && !connectFuture.isDone()) return connectFuture;
        connectFuture = new CompletableFuture<>();
        connect();
        return connectFuture;
    }

    //Subscribe API

    public String subscribe(String messageType, Consumer<JsonNode> handler) {
        String id = UUID.randomUUID().toString();
        listeners
            .computeIfAbsent(messageType, k -> new CopyOnWriteArrayList<>())
            .add(new ListenerEntry(id, handler));
        return id;
    }

    public void unsubscribe(String listenerId) {
        if (listenerId == null) return;
        for (List<ListenerEntry> list : listeners.values()) {
            list.removeIf(e -> e.id().equals(listenerId));
        }
    }

    @Deprecated
    public void setOnMessageCallback(Consumer<JsonNode> callback) {
        String catchAllKey = "__LEGACY__";
        List<ListenerEntry> list = listeners.computeIfAbsent(catchAllKey, k -> new CopyOnWriteArrayList<>());
        list.clear(); 
        if (callback != null) {
            list.add(new ListenerEntry("legacy", callback));
        }
    }

    //Odosielanie

    public void sendAction(String action, Object payload) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("action", action);
            if (payload != null) root.set("payload", mapper.valueToTree(payload));
            send(root.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    //WebSocket callbacks

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[WS] Pripojené k serveru");
        if (connectFuture != null && !connectFuture.isDone()) connectFuture.complete(null);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WS] Prijaté: " + message);
        try {
            JsonNode node = mapper.readTree(message);
            String   type = node.path("type").asText("");

            //Type-based listenery
            List<ListenerEntry> typed = listeners.get(type);
            if (typed != null) {
                for (ListenerEntry e : typed) {
                    try { e.handler().accept(node); }
                    catch (Exception ex) { System.err.println("[WS] Listener chyba (" + type + "): " + ex.getMessage()); }
                }
            }

            // Legacy catch-all callback
            List<ListenerEntry> legacy = listeners.get("__LEGACY__");
            if (legacy != null) {
                for (ListenerEntry e : legacy) {
                    try { e.handler().accept(node); }
                    catch (Exception ex) { System.err.println("[WS] Legacy callback chyba: " + ex.getMessage()); }
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WS] Spojenie ukončené: " + reason);
        connectFuture = null;
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[WS] Chyba: " + ex.getMessage());
        if (connectFuture != null && !connectFuture.isDone()) connectFuture.completeExceptionally(ex);
    }
}
