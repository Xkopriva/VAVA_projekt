package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationWindow {

    public static void show() {
        boolean en = UserSession.get().isEnglish();

        Stage stage = new Stage();
        stage.setTitle(en ? "Notifications" : "Upozornenia");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(520);
        stage.setHeight(480);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(en ? "Your Notifications" : "Vaše upozornenia");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(title, Priority.ALWAYS);
        Button markAllBtn = new Button(en ? "Mark all read" : "Označiť všetky");
        markAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #06b6d4; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: #06b6d4; " +
            "-fx-border-radius: 6; -fx-padding: 4 10;");
        header.getChildren().addAll(title, markAllBtn);

        // Loading indicator
        Label loadingLbl = new Label(en ? "⏳ Loading..." : "⏳ Načítavam...");
        loadingLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 20;");

        VBox notificationsList = new VBox(8);
        notificationsList.setPadding(new Insets(4));
        notificationsList.getChildren().add(loadingLbl);

        ScrollPane scroll = new ScrollPane(notificationsList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button closeButton = new Button(en ? "Close" : "Zatvoriť");
        closeButton.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-padding: 8 24; -fx-background-radius: 8; -fx-cursor: hand;");
        closeButton.setOnAction(e -> stage.close());
        buttonBox.getChildren().add(closeButton);

        root.getChildren().addAll(header, new Separator(), scroll, buttonBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        // Load real notifications from WebSocket
        loadNotifications(notificationsList, en);

        markAllBtn.setOnAction(e -> {
            // Correct backend action name is MARK_ALL_UNREAD
            WebSocketClientService.getInstance().sendAction("MARK_ALL_UNREAD", null);
            // Visual feedback
            notificationsList.getChildren().forEach(child -> {
                if (child instanceof VBox vb) {
                    vb.setOpacity(0.6);
                }
            });
        });
    }

    private static void loadNotifications(VBox list, boolean en) {
        WebSocketClientService ws = WebSocketClientService.getInstance();

        // Backend sends response type "NOTIFICATIONS_LIST" for action "GET_ALL_NOTIFICATIONS"
        String[] subId = new String[1];
        subId[0] = ws.subscribe("NOTIFICATIONS_LIST", node -> {
            ws.unsubscribe(subId[0]);
            JsonNode data = node.path("data");
            Platform.runLater(() -> {
                list.getChildren().clear();

                if (!data.isArray() || data.size() == 0) {
                    // No notifications – show pleasant empty state
                    Label empty = new Label(en ? "✅  No notifications" : "✅  Žiadne upozornenia");
                    empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 20;");
                    list.getChildren().add(empty);
                    return;
                }

                for (JsonNode notif : data) {
                    String type    = notif.path("type").asText("");
                    String title   = notif.path("title").asText(en ? "Notification" : "Upozornenie");
                    String message = notif.path("message").asText("");
                    boolean isRead = notif.path("isRead").asBoolean(false);
                    int notifId    = notif.path("id").asInt(-1);

                    String[] colors = colorsForType(type);
                    VBox card = createNotification(title, message, colors[0], colors[1]);
                    if (isRead) card.setOpacity(0.6);

                    // Click to mark individual notification as read
                    if (!isRead && notifId >= 0) {
                        final int id = notifId;
                        card.setOnMouseClicked(e -> {
                            ws.sendAction("MARK_READ_NOTIFICATION", java.util.Map.of("id", id));
                            card.setOpacity(0.6);
                        });
                        card.setStyle(card.getStyle() + "; -fx-cursor: hand;");
                    }

                    list.getChildren().add(card);
                }
            });
        });

        // Correct backend action name
        ws.sendAction("GET_ALL_NOTIFICATIONS", null);

        // Timeout 4s – if backend doesn't respond, show fallback
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subId[0]);
            Platform.runLater(() -> {
                if (list.getChildren().stream().anyMatch(n ->
                        n instanceof Label lbl && lbl.getText().contains("⏳"))) {
                    list.getChildren().clear();
                    addStaticFallback(list, en);
                }
            });
        }, 4, TimeUnit.SECONDS);
    }

    private static String[] colorsForType(String type) {
        return switch (type) {
            case "MARK_ADDED", "SUBMISSION_GRADED" -> new String[]{"#eff6ff", "#2563eb"};
            case "TASK_DUE"   -> new String[]{"#fef2f2", "#dc2626"};
            case "EXAM_SCHEDULED" -> new String[]{"#fff7ed", "#d97706"};
            case "ANNOUNCEMENT"   -> new String[]{"#f0fdf4", "#16a34a"};
            default               -> new String[]{"#f8fafc",  "#64748b"};
        };
    }

    private static void addStaticFallback(VBox list, boolean en) {
        if (en) {
            list.getChildren().addAll(
                createNotification("⚠️  Project submission – PAS",
                    "Deadline: Friday 23:59. Upload via AIS.", "#fef2f2", "#dc2626"),
                createNotification("📋  New materials – DBS",
                    "Lecture 7 has been uploaded to the portal.", "#eff6ff", "#2563eb"),
                createNotification("📚  Exam – Algebra",
                    "Date: 28.1.2025 at 9:00 in D-302.", "#fff7ed", "#d97706")
            );
        } else {
            list.getChildren().addAll(
                createNotification("⚠️  Odovzdanie projektu – PAS",
                    "Deadline: piatok 23:59. Nahraj cez AIS.", "#fef2f2", "#dc2626"),
                createNotification("📋  Nové materiály – DBS",
                    "Prednáška 7 bola nahraná na portál.", "#eff6ff", "#2563eb"),
                createNotification("📚  Skúška – Algebra",
                    "Dátum: 28.1.2025 o 9:00 v D-302.", "#fff7ed", "#d97706")
            );
        }
    }

    private static VBox createNotification(String title, String message,
                                            String bgColor, String titleColor) {
        VBox notification = new VBox(6);
        notification.setPadding(new Insets(12, 14, 12, 14));
        notification.setStyle("-fx-background-color: " + bgColor +
            "; -fx-background-radius: 10; -fx-border-color: " + titleColor + "33;" +
            " -fx-border-radius: 10; -fx-border-width: 1;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        messageLabel.setWrapText(true);

        notification.getChildren().addAll(titleLabel, messageLabel);
        return notification;
    }
}
