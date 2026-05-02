package com.example.bais;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class NotificationWindow {

    /** Opens the notification pop-up respecting the current language from UserSession. */
    public static void show() {
        boolean en = UserSession.get().isEnglish();

        Stage stage = new Stage();
        stage.setTitle(en ? "Notifications" : "Upozornenia");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(500);
        stage.setHeight(400);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #f1f5f9;");

        // Title
        Label title = new Label(en ? "Your Notifications" : "Vaše upozornenia");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Notifications list
        VBox notificationsList = new VBox(8);
        notificationsList.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8;");

        if (en) {
            notificationsList.getChildren().addAll(
                createNotification("⚠️  Project submission – PAS",  "Deadline: Friday 23:59. Upload via AIS.",      "#fef2f2", "#dc2626"),
                createNotification("📋  New materials – DBS",       "Lecture 7 has been uploaded to the portal.",  "#eff6ff", "#2563eb"),
                createNotification("📚  Exam – Algebra",            "Date: 28.1.2025 at 9:00 in D-302.",           "#fff7ed", "#d97706")
            );
        } else {
            notificationsList.getChildren().addAll(
                createNotification("⚠️  Odovzdanie projektu – PAS", "Deadline: piatok 23:59. Nahraj cez AIS.",     "#fef2f2", "#dc2626"),
                createNotification("📋  Nové materiály – DBS",      "Prednáška 7 bola nahraná na portál.",         "#eff6ff", "#2563eb"),
                createNotification("📚  Skúška – Algebra",          "Dátum: 28.1.2025 o 9:00 v D-302.",           "#fff7ed", "#d97706")
            );
        }

        ScrollPane scroll = new ScrollPane(notificationsList);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Close button
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button closeButton = new Button(en ? "Close" : "Zatvoriť");
        closeButton.setStyle("-fx-padding: 8 20; -fx-font-size: 12px;");
        closeButton.setOnAction(e -> stage.close());
        buttonBox.getChildren().add(closeButton);

        root.getChildren().addAll(title, scroll, buttonBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createNotification(String title, String message, String bgColor, String titleColor) {
        VBox notification = new VBox(4);
        notification.setPadding(new Insets(10, 12, 10, 12));
        notification.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        messageLabel.setWrapText(true);

        notification.getChildren().addAll(titleLabel, messageLabel);
        return notification;
    }
}
