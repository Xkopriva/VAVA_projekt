package com.example.bais.controllers;
import com.example.bais.models.*;
import com.example.bais.services.*;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Admin — Subjects list + Create Subject form + Calendar below (Fotka 5).
 * SK/EN + dark/light mode aware.
 */
public class AdminSubjectsController implements Initializable {

    @FXML private VBox adminSubjectsRoot;

    private final List<SubjectEntry> subjects = new ArrayList<>();
    private final List<UserEntry>    teachers  = new ArrayList<>();
    private String subSubjects, subUsers;
    private volatile int loadedCount = 0;

    record SubjectEntry(int id, String code, String name, int credits, String type, boolean mandatory,
                        String faculty, Integer guarantorId) {}
    record UserEntry(int id, String email, String name, List<String> roles) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        adminSubjectsRoot.getChildren().clear();
        adminSubjectsRoot.setSpacing(24);
        adminSubjectsRoot.setPadding(new Insets(32, 32, 32, 32));

        boolean en = UserSession.get().isEnglish();

        Label title = new Label(en ? "Subjects" : "Predmety");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en
            ? "Create new subjects and manage the existing catalogue."
            : "Vytvárajte nové predmety a spravujte existujúci katalóg.");
        sub.getStyleClass().add("welcome-sub");
        sub.setWrapText(true);
        adminSubjectsRoot.getChildren().add(new VBox(6, title, sub));

        Label loading = new Label("⏳ " + (en ? "Loading data..." : "Načítavam dáta..."));
        loading.setStyle("-fx-font-size:14px;-fx-text-fill:#94a3b8;");
        adminSubjectsRoot.getChildren().add(loading);

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subSubjects = ws.subscribe("TEACHER_SUBJECTS_LIST", node -> {
            ws.unsubscribe(subSubjects);
            JsonNode data = node.path("data");
            subjects.clear();
            if (data.isArray()) {
                for (JsonNode s : data) {
                    Integer gid = s.path("guarantorId").isNull() ? null : s.path("guarantorId").asInt();
                    subjects.add(new SubjectEntry(
                        s.path("id").asInt(),
                        s.path("code").asText("?"),
                        s.path("name").asText(""),
                        s.path("credits").asInt(0),
                        s.path("completionType").asText(""),
                        s.path("isMandatory").asBoolean(false),
                        s.path("faculty").asText(""),
                        gid
                    ));
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
                if (adminSubjectsRoot.getChildren().contains(loading)) buildUI(loading, en);
            });
        }, 6, TimeUnit.SECONDS);
    }

    private synchronized void checkReady(Label loading, boolean en) {
        loadedCount++;
        if (loadedCount >= 2) Platform.runLater(() -> buildUI(loading, en));
    }

    private void buildUI(Label loading, boolean en) {
        adminSubjectsRoot.getChildren().remove(loading);
        adminSubjectsRoot.getChildren().add(buildCreateCard(en));
        adminSubjectsRoot.getChildren().add(buildSubjectsTableCard(en));
        loadCalendar(en);
    }

    // ── Card: Create Subject ──────────────────────────────────────────────────────

    private VBox buildCreateCard(boolean en) {
        VBox card = new VBox(16);
        card.getStyleClass().add("section-card");

        Label hdr = new Label("📚  " + (en ? "Create New Subject" : "Vytvoriť nový predmet"));
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#6366f1;");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        TextField codeField  = textField("DBS_B");
        TextField nameField  = textField(en ? "e.g. Database Systems" : "napr. Databázové systémy");
        TextField facField   = textField("FIIT");

        Spinner<Integer> creditsSpinner = new Spinner<>(1, 12, 6);
        creditsSpinner.setEditable(true);
        creditsSpinner.setPrefWidth(100);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("EXAM", "CREDITS", "CLASSIFIED_CREDITS");
        typeCombo.setValue("EXAM");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        CheckBox mandatoryChk = new CheckBox(en ? "Mandatory subject" : "Povinný predmet");
        mandatoryChk.setSelected(true);

        ComboBox<String> guarCombo = buildTeacherCombo();

        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-size:12px;");
        statusLbl.setVisible(false);

        Button createBtn = actionButton(en ? "Create Subject" : "Vytvoriť predmet", "#6366f1");
        createBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            int    cred = creditsSpinner.getValue();
            String type = typeCombo.getValue();
            if (code.isBlank() || name.isBlank() || type == null) {
                setStatus(statusLbl, en ? "⚠️ Fill in required fields." : "⚠️ Vyplňte povinné polia.", "#d97706");
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("code",           code);
            payload.put("name",           name);
            payload.put("locale",         "sk");
            payload.put("description",    "");
            payload.put("credits",        cred);
            payload.put("completionType", type);
            payload.put("isMandatory",    mandatoryChk.isSelected());
            payload.put("faculty",        facField.getText().isBlank() ? "FIIT" : facField.getText().trim());

            String gSel = guarCombo.getValue();
            if (gSel != null) {
                teachers.stream().filter(t -> gSel.startsWith(t.email())).findFirst()
                    .ifPresent(t -> payload.put("guarantorId", t.id()));
            }

            String[] sub = new String[1];
            sub[0] = WebSocketClientService.getInstance().subscribe("SUBJECT_CREATED", resp -> {
                WebSocketClientService.getInstance().unsubscribe(sub[0]);
                Platform.runLater(() -> {
                    setStatus(statusLbl, "✅ " + (en ? "Subject created!" : "Predmet vytvorený!"), "#16a34a");
                    codeField.clear(); nameField.clear();
                    refreshSubjectsTable(en);
                });
            });
            String[] errSub = new String[1];
            errSub[0] = WebSocketClientService.getInstance().subscribe("ERROR", err -> {
                WebSocketClientService.getInstance().unsubscribe(errSub[0]);
                Platform.runLater(() -> setStatus(statusLbl,
                    "❌ " + err.path("data").path("message").asText(en ? "Error" : "Chyba"), "#dc2626"));
            });
            WebSocketClientService.getInstance().sendAction("CREATE_SUBJECT", payload);
        });

        HBox row1 = twoColRow(fieldLabel(en ? "Subject Code *" : "Kód predmetu *"), codeField,
                              fieldLabel(en ? "Name (Slovak) *" : "Názov (Slovensky) *"), nameField);
        HBox row2 = twoColRow(fieldLabel(en ? "Credits *" : "Kredity *"), creditsSpinner,
                              fieldLabel(en ? "Completion Type *" : "Typ ukončenia *"), typeCombo);
        HBox row3 = twoColRow(fieldLabel(en ? "Faculty" : "Fakulta"), facField,
                              fieldLabel(en ? "Guarantor (optional)" : "Garant (voliteľné)"), guarCombo);

        card.getChildren().addAll(row1, row2, row3, mandatoryChk,
            new HBox(12, createBtn, statusLbl) {{ setAlignment(Pos.CENTER_LEFT); }});
        return card;
    }

    // ── Card: Subjects Table ──────────────────────────────────────────────────────

    private VBox buildSubjectsTableCard(boolean en) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");
        card.setId("subjectsTableCard");

        Label hdr = new Label("📋  " + (en ? "Subject Catalogue" : "Katalóg predmetov"));
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#10b981;");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        populateSubjectsTable(card, en);
        return card;
    }

    private void populateSubjectsTable(VBox card, boolean en) {
        while (card.getChildren().size() > 2) card.getChildren().remove(2);

        if (subjects.isEmpty()) {
            Label empty = new Label(en ? "No subjects found." : "Žiadne predmety nenájdené.");
            empty.getStyleClass().add("schedule-loc");
            card.getChildren().add(empty);
            return;
        }

        // Header row
        HBox colHdr = new HBox();
        colHdr.setAlignment(Pos.CENTER_LEFT);
        colHdr.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;" +
            "-fx-border-width:0 0 1 0;-fx-padding:0 0 6 0;");
        String[] cols   = { en?"Code":"Kód",    en?"Name":"Názov", en?"Cr.":"Kr.",
                            en?"Type":"Typ",     en?"Faculty":"Fakulta", en?"Guarantor":"Garant" };
        double[] widths  = { 100, -1, 40, 130, 80, 200 };
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i]);
            l.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
            if (widths[i] > 0) l.setMinWidth(widths[i]);
            else HBox.setHgrow(l, Priority.ALWAYS);
            colHdr.getChildren().add(l);
        }
        card.getChildren().add(colHdr);

        for (SubjectEntry s : subjects) {
            String guarantorName = "—";
            if (s.guarantorId() != null) {
                guarantorName = teachers.stream()
                    .filter(t -> t.id() == s.guarantorId())
                    .findFirst()
                    .map(t -> t.name().trim())
                    .orElse("ID " + s.guarantorId());
            }

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7, 0, 7, 0));
            row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");

            Label codeLbl = new Label(s.code());
            codeLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6366f1;" +
                "-fx-background-color:rgba(99,102,241,0.1);-fx-padding:2 7;-fx-background-radius:6;");
            codeLbl.setMinWidth(100);

            Label nameLbl = new Label(s.name().isEmpty() ? "—" : s.name());
            nameLbl.getStyleClass().add("schedule-name");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            Label credLbl = new Label(String.valueOf(s.credits()));
            credLbl.getStyleClass().add("schedule-loc");
            credLbl.setMinWidth(40);

            String typeShort = switch (s.type()) {
                case "EXAM" -> en ? "Exam" : "Skúška";
                case "CREDITS" -> en ? "Credits" : "Zápočet";
                case "CLASSIFIED_CREDITS" -> en ? "Class.Cr." : "Kl.zápočet";
                default -> s.type();
            };
            Label typeLbl = new Label(typeShort);
            typeLbl.getStyleClass().add("schedule-loc");
            typeLbl.setMinWidth(130);

            Label facLbl = new Label(s.faculty().isEmpty() ? "—" : s.faculty());
            facLbl.getStyleClass().add("schedule-loc");
            facLbl.setMinWidth(80);

            Label gLbl = new Label(guarantorName);
            gLbl.getStyleClass().add("schedule-name");
            gLbl.setMinWidth(200);
            if ("—".equals(guarantorName)) gLbl.setStyle("-fx-text-fill:#94a3b8;");

            row.getChildren().addAll(codeLbl, nameLbl, credLbl, typeLbl, facLbl, gLbl);
            card.getChildren().add(row);
        }
    }

    private void refreshSubjectsTable(boolean en) {
        String[] sub = new String[1];
        sub[0] = WebSocketClientService.getInstance().subscribe("TEACHER_SUBJECTS_LIST", node -> {
            WebSocketClientService.getInstance().unsubscribe(sub[0]);
            JsonNode data = node.path("data");
            subjects.clear();
            if (data.isArray()) {
                for (JsonNode s : data) {
                    Integer gid = s.path("guarantorId").isNull() ? null : s.path("guarantorId").asInt();
                    subjects.add(new SubjectEntry(
                        s.path("id").asInt(), s.path("code").asText("?"),
                        s.path("name").asText(""), s.path("credits").asInt(0),
                        s.path("completionType").asText(""), s.path("isMandatory").asBoolean(false),
                        s.path("faculty").asText(""), gid));
                }
            }
            Platform.runLater(() -> adminSubjectsRoot.getChildren().stream()
                .filter(n -> n instanceof VBox vb && "subjectsTableCard".equals(vb.getId()))
                .forEach(n -> populateSubjectsTable((VBox) n, en)));
        });
        WebSocketClientService.getInstance().sendAction("GET_MY_SUBJECTS", null);
    }

    // ── Embed Calendar below ──────────────────────────────────────────────────────

    private void loadCalendar(boolean en) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/bais/school-calendar-view.fxml"));
            Node calendarView = loader.load();

            Label calTitle = new Label("📅  " + (en ? "School Calendar" : "Školský kalendár"));
            calTitle.setStyle("-fx-font-size:18px;-fx-font-weight:bold;");
            calTitle.getStyleClass().add("section-title");
            calTitle.setPadding(new Insets(8, 0, 0, 0));

            adminSubjectsRoot.getChildren().addAll(calTitle, calendarView);
        } catch (Exception ex) {
            Label err = new Label("⚠️ " + (en ? "Calendar unavailable" : "Kalendár nedostupný"));
            err.setStyle("-fx-text-fill:#94a3b8;");
            adminSubjectsRoot.getChildren().add(err);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private ComboBox<String> buildTeacherCombo() {
        ComboBox<String> combo = new ComboBox<>();
        teachers.stream()
            .filter(t -> t.roles().contains("TEACHER") || t.roles().contains("ADMIN"))
            .forEach(t -> combo.getItems().add(t.email() + " – " + t.name().trim()));
        combo.setPromptText("—"); combo.setMaxWidth(Double.MAX_VALUE); return combo;
    }
    private Region divider() {
        Region r = new Region(); r.setPrefHeight(1); r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#e2e8f0;"); return r;
    }
    private Label fieldLabel(String t) {
        Label l = new Label(t); l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;");
        l.getStyleClass().add("text-primary"); return l;
    }
    private TextField textField(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt);
        f.getStyleClass().add("text-field-custom"); f.setMaxWidth(Double.MAX_VALUE); return f;
    }
    private Button actionButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-background-radius:8;-fx-padding:8 20;-fx-cursor:hand;-fx-font-size:13px;"); return b;
    }
    private HBox twoColRow(javafx.scene.Node lbl1, javafx.scene.Node f1,
                            javafx.scene.Node lbl2, javafx.scene.Node f2) {
        VBox c1 = new VBox(4, lbl1, f1); c1.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(c1, Priority.ALWAYS);
        VBox c2 = new VBox(4, lbl2, f2); c2.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(c2, Priority.ALWAYS);
        HBox row = new HBox(16, c1, c2); row.setAlignment(Pos.TOP_LEFT); return row;
    }
    private void setStatus(Label lbl, String text, String color) {
        lbl.setText(text); lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + color + ";"); lbl.setVisible(true);
    }
}
