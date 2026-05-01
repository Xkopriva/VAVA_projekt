package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WebSocketClientService extends WebSocketClient {

    private static WebSocketClientService instance;
    private final ObjectMapper mapper = new ObjectMapper();
    private Consumer<JsonNode> onMessageCallback;
    private CompletableFuture<Void> connectFuture;

    private WebSocketClientService(URI serverUri) {
        super(serverUri);
    }

    public static synchronized WebSocketClientService getInstance() {
        if (instance == null) {
            try {
                instance = new WebSocketClientService(new URI("ws://localhost:8887"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public CompletableFuture<Void> connectAsync() {
        if (isOpen()) return CompletableFuture.completedFuture(null);
        connectFuture = new CompletableFuture<>();
        connect();
        return connectFuture;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WS] Connected to server");
        if (connectFuture != null) connectFuture.complete(null);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WS] Received: " + message);
        try {
            JsonNode node = mapper.readTree(message);
            if (onMessageCallback != null) {
                onMessageCallback.accept(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WS] Closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[WS] Error: " + ex.getMessage());
        if (connectFuture != null && !connectFuture.isDone()) {
            connectFuture.completeExceptionally(ex);
        }
    }

    public void setOnMessageCallback(Consumer<JsonNode> callback) {
        this.onMessageCallback = callback;
    }

    public void sendAction(String action, Object payload) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("action", action);
            if (payload != null) {
                root.set("payload", mapper.valueToTree(payload));
            }
            send(root.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
