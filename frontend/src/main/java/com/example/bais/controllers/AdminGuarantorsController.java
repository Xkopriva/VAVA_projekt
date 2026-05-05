package com.example.bais.controllers;

import com.example.bais.models.*;
import com.example.bais.services.*;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Admin — Assign Guarantors screen.
 * SK/EN + dark/light mode aware.
 */
public class AdminGuarantorsController implements Initializable {

    @FXML
    private VBox adminGuarantorsRoot;

    private final List<SubjectEntry> subjects = new ArrayList<>();
    private final List<UserEntry> teachers = new ArrayList<>();
    private String subSubjects, subUsers;
    private volatile int loadedCount = 0;

    record SubjectEntry(int id, String code, Integer guarantorId) {
    }

    record UserEntry(int id, String email, String name, List<String> roles) {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        adminGuarantorsRoot.getChildren().clear();
        adminGuarantorsRoot.setSpacing(24);
        adminGuarantorsRoot.setPadding(new Insets(32, 32, 32, 32));

        boolean en = UserSession.get().isEnglish();

        Label title = new Label(en ? "Guarantors" : "Garanti");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en
                ? "Assign teachers as guarantors for subjects."
                : "Priraďte učiteľov ako garantov predmetov.");
        sub.getStyleClass().add("welcome-sub");
        sub.setWrapText(true);
        adminGuarantorsRoot.getChildren().add(new VBox(6, title, sub));

        Label loading = new Label("⏳ " + (en ? "Loading data..." : "Načítavam dáta..."));
        loading.setStyle("-fx-font-size:14px;-fx-text-fill:#94a3b8;");
        adminGuarantorsRoot.getChildren().add(loading);

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subSubjects = ws.subscribe("TEACHER_SUBJECTS_LIST", node -> {
            ws.unsubscribe(subSubjects);
            JsonNode data = node.path("data");
            subjects.clear();
            if (data.isArray()) {
                for (JsonNode s : data) {
                    Integer gid = s.path("guarantorId").isNull() ? null : s.path("guarantorId").asInt();
                    subjects.add(new SubjectEntry(s.path("id").asInt(), s.path("code").asText("?"), gid));
                }
            }
            checkReady(loading, en);
        });
        subUsers = ws.subscribe("USER_LIST", node -> {
            ws.unsubscribe(subUsers);
            JsonNode data = node.path("data");
            teachers.clear();
            if (data.isArray()) {
                for (JsonNode u : data) {
                    List<String> roles = new ArrayList<>();
                    u.path("roles").forEach(r -> roles.add(r.asText()));
                    teachers.add(new UserEntry(
                            u.path("id").asInt(), u.path("email").asText(""),
                            u.path("firstName").asText("") + " " + u.path("lastName").asText(""), roles));
                }
            }
            checkReady(loading, en);
        });
        ws.sendAction("GET_MY_SUBJECTS", null);
        ws.sendAction("LIST_USERS", null);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subSubjects);
            ws.unsubscribe(subUsers);
            Platform.runLater(() -> {
                if (adminGuarantorsRoot.getChildren().contains(loading))
                    buildUI(loading, en);
            });
        }, 6, TimeUnit.SECONDS);
    }

    private synchronized void checkReady(Label loading, boolean en) {
        loadedCount++;
        if (loadedCount >= 2)
            Platform.runLater(() -> buildUI(loading, en));
    }

    private void buildUI(Label loading, boolean en) {
        adminGuarantorsRoot.getChildren().remove(loading);
        adminGuarantorsRoot.getChildren().add(buildAssignCard(en));
        adminGuarantorsRoot.getChildren().add(buildOverviewCard(en));
    }

    // ── Card: Assign ─────────────────────────────────────────────────────────────

    private VBox buildAssignCard(boolean en) {
        VBox card = new VBox(16);
        card.getStyleClass().add("section-card");

        Label hdr = new Label("🔗  " + (en ? "Assign Guarantor" : "Priradiť garanta"));
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#f59e0b;");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        ComboBox<String> subjCombo = new ComboBox<>();
        subjects.forEach(s -> subjCombo.getItems().add(s.code() + " (id:" + s.id() + ")"));
        subjCombo.setPromptText(en ? "Select subject..." : "Vyber predmet...");
        subjCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> guarCombo = buildTeacherCombo();

        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-size:12px;");
        statusLbl.setVisible(false);

        Button assignBtn = actionButton(en ? "Assign Guarantor" : "Priradiť garanta", "#f59e0b");
        assignBtn.setOnAction(e -> {
            String subjSel = subjCombo.getValue();
            String guarSel = guarCombo.getValue();
            if (subjSel == null || guarSel == null) {
                setStatus(statusLbl, en ? "⚠️ Select subject and teacher." : "⚠️ Vyber predmet a učiteľa.", "#d97706");
                return;
            }
            int subjectId = subjects.stream()
                    .filter(s -> subjSel.startsWith(s.code()))
                    .findFirst().map(SubjectEntry::id).orElse(-1);
            int teacherId = teachers.stream()
                    .filter(t -> guarSel.startsWith(t.email()))
                    .findFirst().map(UserEntry::id).orElse(-1);
            if (subjectId == -1 || teacherId == -1)
                return;

            String[] sub = new String[1];
            sub[0] = WebSocketClientService.getInstance().subscribe("GUARANTOR_ASSIGNED", resp -> {
                WebSocketClientService.getInstance().unsubscribe(sub[0]);
                // Update local data
                for (int i = 0; i < subjects.size(); i++) {
                    if (subjects.get(i).id() == subjectId) {
                        subjects.set(i, new SubjectEntry(subjectId, subjects.get(i).code(), teacherId));
                        break;
                    }
                }
                Platform.runLater(() -> {
                    setStatus(statusLbl, "✅ " + (en ? "Guarantor assigned!" : "Garant priradený!"), "#16a34a");
                    // Refresh overview card
                    adminGuarantorsRoot.getChildren().stream()
                            .filter(n -> n instanceof VBox vb && "overviewCard".equals(vb.getId()))
                            .forEach(n -> {
                                VBox ov = (VBox) n;
                                while (ov.getChildren().size() > 2)
                                    ov.getChildren().remove(2);
                                populateOverview(ov, en);
                            });
                });
            });
            WebSocketClientService.getInstance().sendAction("ASSIGN_GUARANTOR",
                    Map.of("teacherId", teacherId, "subjectId", subjectId));
        });

        HBox row = twoColRow(fieldLabel(en ? "Subject" : "Predmet"), subjCombo,
                fieldLabel(en ? "Teacher" : "Učiteľ"), guarCombo);
        card.getChildren().addAll(row,
                new HBox(12, assignBtn, statusLbl) {
                    {
                        setAlignment(Pos.CENTER_LEFT);
                    }
                });
        return card;
    }

    // ── Card: Overview ──────────────────────────────────────────────────────────

    private VBox buildOverviewCard(boolean en) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");
        card.setId("overviewCard");

        Label hdr = new Label("📋  " + (en ? "All Subjects & Guarantors" : "Všetky predmety a garanti"));
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#10b981;");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        populateOverview(card, en);
        return card;
    }

    private void populateOverview(VBox card, boolean en) {
        if (subjects.isEmpty()) {
            Label empty = new Label(en ? "No subjects found." : "Žiadne predmety nenájdené.");
            empty.getStyleClass().add("schedule-loc");
            card.getChildren().add(empty);
            return;
        }

        // Header row
        HBox colHdr = new HBox();
        colHdr.setAlignment(Pos.CENTER_LEFT);
        colHdr.setStyle(
                "-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;-fx-padding:0 0 6 0;");
        for (String col : new String[] { en ? "Code" : "Kód", en ? "ID" : "ID", en ? "Guarantor" : "Garant" }) {
            Label l = new Label(col);
            l.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
            if (col.equals(en ? "Code" : "Kód"))
                HBox.setHgrow(l, Priority.ALWAYS);
            else
                l.setMinWidth(col.equals(en ? "ID" : "ID") ? 50 : 240);
            colHdr.getChildren().add(l);
        }
        card.getChildren().add(colHdr);

        for (SubjectEntry s : subjects) {
            String guarantorName = "—";
            if (s.guarantorId() != null) {
                guarantorName = teachers.stream()
                        .filter(t -> t.id() == s.guarantorId())
                        .findFirst()
                        .map(t -> t.name().trim() + " (" + t.email() + ")")
                        .orElse("ID " + s.guarantorId());
            }

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 0, 8, 0));
            row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");

            Label codeLbl = new Label(s.code());
            codeLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;" +
                    "-fx-background-color:rgba(6,182,212,0.1);-fx-padding:2 8;-fx-background-radius:6;");
            HBox.setHgrow(codeLbl, Priority.ALWAYS);

            Label idLbl = new Label(String.valueOf(s.id()));
            idLbl.getStyleClass().add("schedule-loc");
            idLbl.setMinWidth(50);

            Label gLbl = new Label(guarantorName);
            gLbl.getStyleClass().add("schedule-name");
            gLbl.setMinWidth(240);
            if ("—".equals(guarantorName))
                gLbl.setStyle("-fx-text-fill:#94a3b8;");

            row.getChildren().addAll(codeLbl, idLbl, gLbl);
            card.getChildren().add(row);
        }
    }

    // ── Helpers
    // ───────────────────────────────────────────────────────────────────

    private ComboBox<String> buildTeacherCombo() {
        ComboBox<String> combo = new ComboBox<>();
        teachers.stream()
                .filter(t -> t.roles().contains("TEACHER") || t.roles().contains("ADMIN"))
                .forEach(t -> combo.getItems().add(t.email() + " – " + t.name().trim()));
        combo.setPromptText("—");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#e2e8f0;");
        return r;
    }

    private Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;");
        l.getStyleClass().add("text-primary");
        return l;
    }

    private Button actionButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:8 20;-fx-cursor:hand;-fx-font-size:13px;");
        return b;
    }

    private HBox twoColRow(javafx.scene.Node lbl1, javafx.scene.Node f1,
            javafx.scene.Node lbl2, javafx.scene.Node f2) {
        VBox c1 = new VBox(4, lbl1, f1);
        c1.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(c1, Priority.ALWAYS);
        VBox c2 = new VBox(4, lbl2, f2);
        c2.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(c2, Priority.ALWAYS);
        HBox row = new HBox(16, c1, c2);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void setStatus(Label lbl, String text, String color) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + color + ";");
        lbl.setVisible(true);
    }
}
