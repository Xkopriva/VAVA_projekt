package com.example.bais;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class SubmissionsController implements Initializable {

    @FXML private Label submissionsTitle;
    @FXML private VBox submissionsListContainer;

    private boolean isEnglish;
    private String subTasks;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isEnglish = UserSession.get().isEnglish();
        updateTitle();
        loadMyTasks();
    }

    private void updateTitle() {
        if (submissionsTitle != null) {
            submissionsTitle.setText(isEnglish ? "My Assignments" : "Moje zadania");
        }
    }

    // ── A. Načítanie zoznamu úloh ──────────────────────────────────────────────

    private void loadMyTasks() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        System.out.println("[SubmissionsController] Sending GET_MY_TASKS");

        // Backend: action=GET_MY_TASKS → response type=MY_TASKS_LIST
        subTasks = ws.subscribe("MY_TASKS_LIST", this::handleTasksList);
        ws.sendAction("GET_MY_TASKS", null);
    }

    private void handleTasksList(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subTasks);
        System.out.println("[SubmissionsController] Received MY_TASKS_LIST");

        JsonNode data = node.path("data");

        Platform.runLater(() -> {
            submissionsListContainer.getChildren().clear();

            if (!data.isArray() || data.size() == 0) {
                Label empty = new Label(isEnglish
                        ? "✅  No assignments found."
                        : "✅  Žiadne zadania sa nenašli.");
                empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-padding: 16;");
                submissionsListContainer.getChildren().add(empty);
                return;
            }

            for (JsonNode task : data) {
                submissionsListContainer.getChildren().add(buildTaskCard(task));
            }
        });
    }

    // ── B. Karta jednej úlohy ──────────────────────────────────────────────────

    private VBox buildTaskCard(JsonNode task) {
        int    taskId      = task.path("id").asInt(-1);
        String title       = task.path("title").asText(isEnglish ? "Unnamed task" : "Nepomenovaná úloha");
        String description = task.path("description").asText("");
        String dueAtRaw    = task.path("dueAt").asText("");
        double maxPoints   = task.path("maxPoints").asDouble(0);
        boolean published  = task.path("isPublished").asBoolean(false);

        VBox card = new VBox(8);
        card.getStyleClass().add("section-card");
        card.setPadding(new Insets(16));

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("submission-course-assignment");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        // Status badge
        Label badge = new Label(published
                ? (isEnglish ? "Published" : "Publikované")
                : (isEnglish ? "Draft" : "Koncept"));
        badge.setStyle("-fx-font-size: 11px; -fx-padding: 2 8; -fx-background-radius: 12;"
                + (published
                   ? "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;"
                   : "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;"));

        titleRow.getChildren().addAll(titleLbl, badge);

        // Description (skrátená)
        if (!description.isBlank()) {
            Label descLbl = new Label(description.length() > 100
                    ? description.substring(0, 100) + "…"
                    : description);
            descLbl.getStyleClass().add("submission-deadline");
            descLbl.setWrapText(true);
            card.getChildren().add(descLbl);
        }

        // Deadline + body row
        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        if (!dueAtRaw.isBlank()) {
            try {
                OffsetDateTime due = OffsetDateTime.parse(dueAtRaw);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");
                Label deadlineLbl = new Label("⏰ " + due.format(fmt));
                deadlineLbl.getStyleClass().add("submission-deadline");
                deadlineLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
                metaRow.getChildren().add(deadlineLbl);
            } catch (Exception ignored) {
                Label deadlineLbl = new Label("⏰ " + dueAtRaw);
                deadlineLbl.getStyleClass().add("submission-deadline");
                metaRow.getChildren().add(deadlineLbl);
            }
        }

        if (maxPoints > 0) {
            Label pointsLbl = new Label((isEnglish ? "Max: " : "Max: ") + (int) maxPoints + " b.");
            pointsLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563eb;");
            metaRow.getChildren().add(pointsLbl);
        }

        // Buttons
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailBtn = new Button(isEnglish ? "Details" : "Detail");
        detailBtn.getStyleClass().add("submission-button");
        detailBtn.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1;"
                + "-fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;");
        detailBtn.setOnAction(e -> openTaskDetail(taskId, title));

        Button submitBtn = new Button(isEnglish ? "Submit" : "Odovzdať");
        submitBtn.getStyleClass().add("submission-button");
        submitBtn.setOnAction(e -> showSubmitDialog(taskId, title));

        HBox btnRow = new HBox(8, spacer, detailBtn, submitBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(titleRow, metaRow, btnRow);
        return card;
    }

    // ── C. Detail úlohy (GET_TASK_DETAIL → TASK_DETAIL) ───────────────────────

    private void openTaskDetail(int taskId, String taskTitle) {
        if (taskId < 0) return;
        WebSocketClientService ws = WebSocketClientService.getInstance();

        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle(isEnglish ? "Loading..." : "Načítavam...");
        Label lbl = new Label(isEnglish ? "⏳ Loading task detail..." : "⏳ Načítavam detail úlohy...");
        lbl.setStyle("-fx-font-size: 14px; -fx-padding: 30;");
        loadingStage.setScene(new Scene(new VBox(lbl), 300, 100));
        loadingStage.show();

        String[] sub = new String[1];
        sub[0] = ws.subscribe("TASK_DETAIL", node -> {
            ws.unsubscribe(sub[0]);
            JsonNode data = node.path("data");
            Platform.runLater(() -> {
                loadingStage.close();
                showDetailDialog(data, taskTitle);
            });
        });

        ws.sendAction("GET_TASK_DETAIL", java.util.Map.of("taskId", taskId));
    }

    private void showDetailDialog(JsonNode data, String fallbackTitle) {
        boolean en = isEnglish;
        JsonNode task       = data.path("task");
        JsonNode submission = data.path("submission");

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(en ? "Task Detail" : "Detail úlohy");
        stage.setWidth(520);
        stage.setResizable(false);

        VBox root = new VBox(12);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f8fafc;");

        // Title
        Label title = new Label(task.path("title").asText(fallbackTitle));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        title.setWrapText(true);

        // Description
        String desc = task.path("description").asText("");
        if (!desc.isBlank()) {
            Label descLbl = new Label(desc);
            descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
            descLbl.setWrapText(true);
            root.getChildren().addAll(title, new Separator(), descLbl);
        } else {
            root.getChildren().addAll(title, new Separator());
        }

        // Deadline + body
        String dueRaw = task.path("dueAt").asText("");
        double max    = task.path("maxPoints").asDouble(0);

        HBox meta = new HBox(16);
        if (!dueRaw.isBlank()) {
            try {
                OffsetDateTime due = OffsetDateTime.parse(dueRaw);
                Label dl = new Label("⏰ " + due.format(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm")));
                dl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
                meta.getChildren().add(dl);
            } catch (Exception ignored) {}
        }
        if (max > 0) {
            Label pt = new Label("🏆 " + (en ? "Max points: " : "Max bodov: ") + (int) max);
            pt.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 13px;");
            meta.getChildren().add(pt);
        }
        if (!meta.getChildren().isEmpty()) root.getChildren().add(meta);

        // Existing submission status
        if (!submission.isMissingNode() && !submission.isNull()) {
            String status   = submission.path("status").asText("SUBMITTED");
            String fileUrl  = submission.path("fileUrl").asText("");
            String content  = submission.path("content").asText("");
            String subAtRaw = submission.path("submittedAt").asText("");

            VBox subBox = new VBox(6);
            subBox.setStyle("-fx-background-color: #f0fdf4; -fx-padding: 12;"
                    + "-fx-background-radius: 8; -fx-border-color: #86efac;"
                    + "-fx-border-radius: 8; -fx-border-width: 1;");

            Label subTitle = new Label(en ? "✅ Your submission" : "✅ Vaše odovzdanie");
            subTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a; -fx-font-size: 13px;");

            Label statusLbl = new Label((en ? "Status: " : "Stav: ") + status);
            statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            subBox.getChildren().addAll(subTitle, statusLbl);

            if (!fileUrl.isBlank()) {
                Label urlLbl = new Label("🔗 " + fileUrl);
                urlLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2563eb;");
                urlLbl.setWrapText(true);
                subBox.getChildren().add(urlLbl);
            }
            if (!content.isBlank()) {
                Label contentLbl = new Label(content);
                contentLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                contentLbl.setWrapText(true);
                subBox.getChildren().add(contentLbl);
            }
            if (!subAtRaw.isBlank()) {
                try {
                    OffsetDateTime sat = OffsetDateTime.parse(subAtRaw);
                    Label satLbl = new Label((en ? "Submitted: " : "Odovzdané: ")
                            + sat.format(DateTimeFormatter.ofPattern("d.M.yyyy HH:mm")));
                    satLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                    subBox.getChildren().add(satLbl);
                } catch (Exception ignored) {}
            }

            root.getChildren().add(subBox);
        } else {
            Label noSub = new Label(en ? "⚠️  Not submitted yet." : "⚠️  Ešte neodovzdané.");
            noSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #d97706;");
            root.getChildren().add(noSub);
        }

        // Buttons
        int taskId = task.path("id").asInt(-1);
        Button submitBtn = new Button(en ? "Submit / Resubmit" : "Odovzdať / Prepísať");
        submitBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white;"
                + "-fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        submitBtn.setOnAction(e -> { stage.close(); showSubmitDialog(taskId, task.path("title").asText(fallbackTitle)); });

        Button closeBtn = new Button(en ? "Close" : "Zatvoriť");
        closeBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569;"
                + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(10, closeBtn, submitBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(btnRow);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        stage.setScene(new Scene(scroll, 520, 480));
        stage.show();
    }

    // ── D. Odovzdanie úlohy (SUBMIT_TASK s fileUrl) ────────────────────────────

    private void showSubmitDialog(int taskId, String taskTitle) {
        if (taskId < 0) {
            showAlert(Alert.AlertType.WARNING,
                    isEnglish ? "Invalid task" : "Neplatná úloha",
                    isEnglish ? "Cannot submit — task ID is unknown." : "Nedá sa odovzdať — neznáme ID úlohy.");
            return;
        }

        boolean en = isEnglish;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(en ? "Submit Assignment" : "Odovzdať zadanie");
        stage.setWidth(460);
        stage.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label(en ? "Submit: " + taskTitle : "Odovzdať: " + taskTitle);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        title.setWrapText(true);

        // Info o fileUrl
        Label info = new Label(en
                ? "ℹ️  The backend accepts a file URL (cloud link). Upload your file to a cloud service (Google Drive, GitHub, etc.) and paste the link below."
                : "ℹ️  Backend prijíma URL súboru (cloudový odkaz). Nahrajte súbor na cloudovú službu (Google Drive, GitHub, ...) a vložte odkaz nižšie.");
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 8;"
                + "-fx-background-color: #eff6ff; -fx-background-radius: 6;");
        info.setWrapText(true);

        // File URL field
        Label urlLbl = new Label(en ? "File URL *" : "URL súboru *");
        urlLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField urlField = new TextField();
        urlField.setPromptText("https://drive.google.com/...");
        urlField.getStyleClass().add("text-field");

        // Optional comment
        Label commentLbl = new Label(en ? "Comment (optional)" : "Komentár (voliteľné)");
        commentLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextArea commentArea = new TextArea();
        commentArea.setPromptText(en ? "Add a note for the teacher..." : "Poznámka pre učiteľa...");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);
        commentArea.getStyleClass().add("text-field");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        Button cancelBtn = new Button(en ? "Cancel" : "Zrušiť");
        cancelBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569;"
                + "-fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button sendBtn = new Button(en ? "Submit" : "Odovzdať");
        sendBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white;"
                + "-fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");

        sendBtn.setOnAction(e -> {
            String fileUrl = urlField.getText().trim();
            String content = commentArea.getText().trim();

            if (fileUrl.isBlank()) {
                errorLbl.setText(en ? "Please enter a file URL." : "Zadajte URL súboru.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            // SUBMIT_TASK – backend expects: taskId, fileUrl, content
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("taskId", taskId);
            payload.put("fileUrl", fileUrl);
            if (!content.isBlank()) payload.put("content", content);

            WebSocketClientService ws = WebSocketClientService.getInstance();

            String[] sub = new String[1];
            sub[0] = ws.subscribe("SUBMISSION_SAVED", resp -> {
                ws.unsubscribe(sub[0]);
                Platform.runLater(() -> {
                    stage.close();
                    showAlert(Alert.AlertType.INFORMATION,
                            en ? "Submitted!" : "Odovzdané!",
                            en ? "Your assignment was submitted successfully." : "Zadanie bolo úspešne odovzdané.");
                    // Reload task list to reflect new status
                    loadMyTasks();
                });
            });

            // Also listen for ERROR
            String[] errSub = new String[1];
            errSub[0] = ws.subscribe("ERROR", errNode -> {
                ws.unsubscribe(errSub[0]);
                Platform.runLater(() -> {
                    String msg = errNode.path("data").path("message").asText(
                            en ? "Submission failed." : "Odovzdanie zlyhalo.");
                    errorLbl.setText(msg);
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                });
            });

            ws.sendAction("SUBMIT_TASK", payload);

            sendBtn.setDisable(true);
            sendBtn.setText(en ? "Submitting..." : "Odosielam...");

            // Timeout – re-enable button after 8s if no response
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() ->
                Platform.runLater(() -> {
                    ws.unsubscribe(sub[0]);
                    ws.unsubscribe(errSub[0]);
                    if (sendBtn.isDisabled()) {
                        sendBtn.setDisable(false);
                        sendBtn.setText(en ? "Submit" : "Odovzdať");
                        errorLbl.setText(en ? "No response from server. Try again." : "Žiadna odpoveď zo servera. Skúste znova.");
                        errorLbl.setVisible(true);
                        errorLbl.setManaged(true);
                    }
                }), 8, java.util.concurrent.TimeUnit.SECONDS);
        });

        HBox btnRow = new HBox(10, cancelBtn, sendBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, info, new Separator(),
                urlLbl, urlField, commentLbl, commentArea,
                errorLbl, btnRow);

        stage.setScene(new Scene(root));
        stage.show();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
