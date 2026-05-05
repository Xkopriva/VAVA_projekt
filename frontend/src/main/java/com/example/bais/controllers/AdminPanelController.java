package com.example.bais.controllers;
import com.example.bais.*;
import com.example.bais.models.*;
import com.example.bais.services.*;
import com.example.bais.components.*;

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
 * Admin Panel — create subjects, create users/teachers, assign guarantors.
 * Supports SK/EN + dark/light mode.
 */
public class AdminPanelController implements Initializable {

    @FXML private VBox adminRoot;

    // Loaded data
    private final List<SubjectEntry> subjects = new ArrayList<>();
    private final List<UserEntry>    teachers  = new ArrayList<>();
    private String subSubjects;
    private String subUsers;

    record SubjectEntry(int id, String code, Integer guarantorId) {}
    record UserEntry(int id, String email, String name, List<String> roles) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        adminRoot.getChildren().clear();
        adminRoot.setSpacing(20);
        adminRoot.setPadding(new Insets(28, 28, 28, 28));

        boolean en = UserSession.get().isEnglish();

        // Page title
        VBox titleBlock = new VBox(4);
        Label title = new Label(en ? "Admin Panel" : "Administrátorský panel");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en
            ? "Create subjects, manage teachers and assign guarantors"
            : "Vytvárajte predmety, spravujte učiteľov a priraďujte garantov");
        sub.getStyleClass().add("welcome-sub");
        sub.setWrapText(true);
        titleBlock.getChildren().addAll(title, sub);
        adminRoot.getChildren().add(titleBlock);

        // Loading placeholder
        Label loading = new Label("⏳ " + (en ? "Loading data..." : "Načítavam dáta..."));
        loading.setStyle("-fx-font-size:14px;-fx-text-fill:#94a3b8;");
        adminRoot.getChildren().add(loading);

        // Load subjects + users in parallel
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
                        u.path("id").asInt(),
                        u.path("email").asText(""),
                        u.path("firstName").asText("") + " " + u.path("lastName").asText(""),
                        roles
                    ));
                }
            }
            checkReady(loading, en);
        });
        // Admin fetches all subjects (guarantorId filter removed on backend for ADMIN)
        ws.sendAction("GET_MY_SUBJECTS", null);
        ws.sendAction("LIST_USERS", null);

        // Timeout
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subSubjects);
            ws.unsubscribe(subUsers);
            Platform.runLater(() -> {
                if (adminRoot.getChildren().contains(loading)) buildUI(en);
            });
        }, 6, TimeUnit.SECONDS);
    }

    private volatile int loadedCount = 0;
    private synchronized void checkReady(Label loading, boolean en) {
        loadedCount++;
        if (loadedCount >= 2) {
            Platform.runLater(() -> buildUI(en));
        }
    }

    private void buildUI(boolean en) {
        // Remove loading label
        adminRoot.getChildren().removeIf(n -> n instanceof Label lbl && lbl.getText().contains("⏳"));

        // ── Section 1: Create Subject ──────────────────────────────────────────
        adminRoot.getChildren().add(buildCreateSubjectCard(en));

        // ── Section 2: Create Teacher User ────────────────────────────────────
        adminRoot.getChildren().add(buildCreateUserCard(en));

        // ── Section 3: Assign Guarantor ───────────────────────────────────────
        adminRoot.getChildren().add(buildAssignGuarantorCard(en));

        // ── Section 4: Subject overview with guarantors ────────────────────────
        adminRoot.getChildren().add(buildSubjectsOverviewCard(en));
    }

    // ── Card: Create Subject ─────────────────────────────────────────────────────

    private VBox buildCreateSubjectCard(boolean en) {
        VBox card = new VBox(14);
        card.getStyleClass().add("section-card");

        Label hdr = sectionHeader("📚  " + (en ? "Create New Subject" : "Vytvoriť nový predmet"), "#6366f1");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        // Code
        Label codeLbl = fieldLabel(en ? "Subject Code *" : "Kód predmetu *");
        TextField codeField = textField("DBS_B");

        // Name (SK)
        Label nameLbl = fieldLabel(en ? "Name (Slovak) *" : "Názov (Slovensky) *");
        TextField nameField = textField(en ? "e.g. Databázové systémy" : "napr. Databázové systémy");

        // Credits
        Label creditsLbl = fieldLabel(en ? "Credits *" : "Kredity *");
        Spinner<Integer> creditsSpinner = new Spinner<>(1, 12, 6);
        creditsSpinner.setEditable(true);
        creditsSpinner.setPrefWidth(100);

        // Completion type
        Label typeLbl = fieldLabel(en ? "Completion Type *" : "Typ ukončenia *");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("EXAM", "CREDITS", "CLASSIFIED_CREDITS");
        typeCombo.setValue("EXAM");

        // Mandatory
        CheckBox mandatoryChk = new CheckBox(en ? "Mandatory subject" : "Povinný predmet");
        mandatoryChk.setSelected(true);

        // Faculty
        Label facLbl = fieldLabel(en ? "Faculty" : "Fakulta");
        TextField facField = textField("FIIT");

        // Guarantor (optional)
        Label guarLbl = fieldLabel(en ? "Guarantor (optional)" : "Garant (voliteľné)");
        ComboBox<String> guarCombo = buildTeacherCombo();

        // Status label + button
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

            // Optional guarantor
            String gSel = guarCombo.getValue();
            if (gSel != null) {
                teachers.stream()
                    .filter(t -> gSel.startsWith(t.email()))
                    .findFirst()
                    .ifPresent(t -> payload.put("guarantorId", t.id()));
            }

            String[] sub = new String[1];
            sub[0] = WebSocketClientService.getInstance().subscribe("SUBJECT_CREATED", resp -> {
                WebSocketClientService.getInstance().unsubscribe(sub[0]);
                Platform.runLater(() -> {
                    setStatus(statusLbl, "✅ " + (en ? "Subject created!" : "Predmet vytvorený!"), "#16a34a");
                    codeField.clear(); nameField.clear();
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

        HBox row1 = twoColRow(codeLbl, codeField, nameLbl, nameField);
        HBox row2 = twoColRow(creditsLbl, creditsSpinner, typeLbl, typeCombo);
        HBox row3 = twoColRow(facLbl, facField, guarLbl, guarCombo);

        card.getChildren().addAll(row1, row2, row3, mandatoryChk,
            new HBox(12, createBtn, statusLbl) {{ setAlignment(Pos.CENTER_LEFT); }});
        return card;
    }

    // ── Card: Create Teacher/User ────────────────────────────────────────────────

    private VBox buildCreateUserCard(boolean en) {
        VBox card = new VBox(14);
        card.getStyleClass().add("section-card");

        Label hdr = sectionHeader("👤  " + (en ? "Create Teacher Account" : "Vytvoriť konto učiteľa"), "#06b6d4");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        Label emailLbl    = fieldLabel("Email *");
        TextField emailFld = textField("jan.novak@stuba.sk");

        Label firstLbl    = fieldLabel(en ? "First Name *" : "Meno *");
        TextField firstFld = textField(en ? "Ján" : "Ján");

        Label lastLbl     = fieldLabel(en ? "Last Name *" : "Priezvisko *");
        TextField lastFld = textField("Novák");

        Label passLbl     = fieldLabel(en ? "Password *" : "Heslo *");
        PasswordField passFld = new PasswordField();
        passFld.setPromptText("heslo");
        passFld.getStyleClass().add("text-field-custom");

        Label roleLbl     = fieldLabel(en ? "Role" : "Rola");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("TEACHER", "STUDENT", "ADMIN");
        roleCombo.setValue("TEACHER");

        Label statusLbl   = new Label();
        statusLbl.setStyle("-fx-font-size:12px;");
        statusLbl.setVisible(false);

        Button createBtn  = actionButton(en ? "Create User" : "Vytvoriť používateľa", "#06b6d4");
        createBtn.setOnAction(e -> {
            String email = emailFld.getText().trim();
            String first = firstFld.getText().trim();
            String last  = lastFld.getText().trim();
            String pass  = passFld.getText();
            String role  = roleCombo.getValue();

            if (email.isBlank() || first.isBlank() || last.isBlank() || pass.isBlank()) {
                setStatus(statusLbl, en ? "⚠️ Fill in all fields." : "⚠️ Vyplňte všetky polia.", "#d97706");
                return;
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("email",     email);
            payload.put("firstName", first);
            payload.put("lastName",  last);
            payload.put("password",  pass);
            payload.put("roleName",  role);

            String[] sub = new String[1];
            sub[0] = WebSocketClientService.getInstance().subscribe("USER_CREATED", resp -> {
                WebSocketClientService.getInstance().unsubscribe(sub[0]);
                Platform.runLater(() -> {
                    setStatus(statusLbl, "✅ " + (en ? "User created!" : "Používateľ vytvorený!"), "#16a34a");
                    emailFld.clear(); firstFld.clear(); lastFld.clear(); passFld.clear();
                });
            });
            String[] errSub = new String[1];
            errSub[0] = WebSocketClientService.getInstance().subscribe("ERROR", err -> {
                WebSocketClientService.getInstance().unsubscribe(errSub[0]);
                Platform.runLater(() -> setStatus(statusLbl,
                    "❌ " + err.path("data").path("message").asText(en ? "Error" : "Chyba"), "#dc2626"));
            });
            WebSocketClientService.getInstance().sendAction("CREATE_USER", payload);
        });

        HBox row1 = twoColRow(emailLbl, emailFld, roleLbl, roleCombo);
        HBox row2 = twoColRow(firstLbl, firstFld, lastLbl, lastFld);
        HBox row3 = twoColRow(passLbl, passFld, new Label(""), new Label(""));

        card.getChildren().addAll(row1, row2, row3,
            new HBox(12, createBtn, statusLbl) {{ setAlignment(Pos.CENTER_LEFT); }});
        return card;
    }

    // ── Card: Assign Guarantor ───────────────────────────────────────────────────

    private VBox buildAssignGuarantorCard(boolean en) {
        VBox card = new VBox(14);
        card.getStyleClass().add("section-card");

        Label hdr = sectionHeader("🔗  " + (en ? "Assign Guarantor to Subject" : "Priradiť garanta predmetu"), "#f59e0b");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        Label subjLbl   = fieldLabel(en ? "Subject" : "Predmet");
        ComboBox<String> subjCombo = new ComboBox<>();
        subjects.forEach(s -> subjCombo.getItems().add(s.code() + " (id:" + s.id() + ")"));
        subjCombo.setPromptText(en ? "Select subject..." : "Vyber predmet...");
        subjCombo.setMaxWidth(Double.MAX_VALUE);

        Label guarLbl   = fieldLabel(en ? "Teacher" : "Učiteľ");
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
            if (subjectId == -1 || teacherId == -1) return;

            String[] sub = new String[1];
            sub[0] = WebSocketClientService.getInstance().subscribe("GUARANTOR_ASSIGNED", resp -> {
                WebSocketClientService.getInstance().unsubscribe(sub[0]);
                Platform.runLater(() -> setStatus(statusLbl,
                    "✅ " + (en ? "Guarantor assigned!" : "Garant priradený!"), "#16a34a"));
            });
            WebSocketClientService.getInstance().sendAction("ASSIGN_GUARANTOR",
                Map.of("teacherId", teacherId, "subjectId", subjectId));
        });

        HBox row = twoColRow(subjLbl, subjCombo, guarLbl, guarCombo);
        card.getChildren().addAll(row,
            new HBox(12, assignBtn, statusLbl) {{ setAlignment(Pos.CENTER_LEFT); }});
        return card;
    }

    // ── Card: Subjects Overview ──────────────────────────────────────────────────

    private VBox buildSubjectsOverviewCard(boolean en) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");

        Label hdr = sectionHeader("📋  " + (en ? "All Subjects & Guarantors" : "Všetky predmety a garanti"), "#10b981");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        if (subjects.isEmpty()) {
            Label empty = new Label(en ? "No subjects found." : "Žiadne predmety nenájdené.");
            empty.getStyleClass().add("schedule-loc");
            card.getChildren().add(empty);
            return card;
        }

        // Column header
        HBox colHdr = tableRow(true,
            en ? "Code"      : "Kód",
            en ? "ID"        : "ID",
            en ? "Guarantor" : "Garant");
        card.getChildren().add(colHdr);

        for (int i = 0; i < subjects.size(); i++) {
            SubjectEntry s = subjects.get(i);
            boolean last = i == subjects.size() - 1;

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
            if (!last) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");

            Label codeLbl = new Label(s.code());
            codeLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;" +
                "-fx-background-color:rgba(6,182,212,0.1);-fx-padding:2 8;-fx-background-radius:6;");
            HBox.setHgrow(codeLbl, Priority.ALWAYS);

            Label idLbl = new Label(String.valueOf(s.id()));
            idLbl.getStyleClass().add("schedule-loc");
            idLbl.setMinWidth(50);

            Label gLbl = new Label(guarantorName);
            gLbl.getStyleClass().add("schedule-name");
            gLbl.setMinWidth(220);
            if ("—".equals(guarantorName)) {
                gLbl.setStyle("-fx-text-fill:#94a3b8;");
            }

            row.getChildren().addAll(codeLbl, idLbl, gLbl);
            card.getChildren().add(row);
        }
        return card;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private ComboBox<String> buildTeacherCombo() {
        ComboBox<String> combo = new ComboBox<>();
        teachers.stream()
            .filter(t -> t.roles().contains("TEACHER") || t.roles().contains("ADMIN"))
            .forEach(t -> combo.getItems().add(t.email() + " – " + t.name().trim()));
        combo.setPromptText("—");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private Label sectionHeader(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        return lbl;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1); r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:#e2e8f0;");
        return r;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;");
        l.getStyleClass().add("text-primary");
        return l;
    }

    private TextField textField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("text-field-custom");
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private Button actionButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-background-radius:8;-fx-padding:8 20;-fx-cursor:hand;-fx-font-size:13px;");
        return b;
    }

    private HBox twoColRow(javafx.scene.Node lbl1, javafx.scene.Node field1,
                            javafx.scene.Node lbl2, javafx.scene.Node field2) {
        VBox col1 = new VBox(4, lbl1, field1);
        col1.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(col1, Priority.ALWAYS);

        VBox col2 = new VBox(4, lbl2, field2);
        col2.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(col2, Priority.ALWAYS);

        HBox row = new HBox(16, col1, col2);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private HBox tableRow(boolean header, String... cols) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        if (header) row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;-fx-padding:0 0 6 0;");
        int[] minWidths = {-1, 50, 220};
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i]);
            if (header) l.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
            if (minWidths[i] > 0) l.setMinWidth(minWidths[i]);
            else HBox.setHgrow(l, Priority.ALWAYS);
            row.getChildren().add(l);
        }
        return row;
    }

    private void setStatus(Label lbl, String text, String color) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + color + ";");
        lbl.setVisible(true);
    }
}
