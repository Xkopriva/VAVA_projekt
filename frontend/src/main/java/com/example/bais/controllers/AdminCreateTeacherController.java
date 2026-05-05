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
 * Admin — Create Teacher Account screen (Fotka 3).
 * SK/EN + dark/light mode aware.
 */
public class AdminCreateTeacherController implements Initializable {

    @FXML private VBox adminCreateTeacherRoot;

    private final List<UserEntry> existingUsers = new ArrayList<>();
    private String subUsers;

    record UserEntry(int id, String email, String name, List<String> roles) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        adminCreateTeacherRoot.getChildren().clear();
        adminCreateTeacherRoot.setSpacing(24);
        adminCreateTeacherRoot.setPadding(new Insets(32, 32, 32, 32));

        boolean en = UserSession.get().isEnglish();

        // Title
        Label title = new Label(en ? "Create Teacher Account" : "Vytvoriť konto učiteľa");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label(en
            ? "Add a new teacher or admin user to the system."
            : "Pridajte nového učiteľa alebo administrátora do systému.");
        sub.getStyleClass().add("welcome-sub");
        sub.setWrapText(true);
        adminCreateTeacherRoot.getChildren().add(new VBox(6, title, sub));

        // Loading
        Label loading = new Label("⏳ " + (en ? "Loading users..." : "Načítavam používateľov..."));
        loading.setStyle("-fx-font-size:14px;-fx-text-fill:#94a3b8;");
        adminCreateTeacherRoot.getChildren().add(loading);

        WebSocketClientService ws = WebSocketClientService.getInstance();
        subUsers = ws.subscribe("USER_LIST", node -> {
            ws.unsubscribe(subUsers);
            JsonNode data = node.path("data");
            existingUsers.clear();
            if (data.isArray()) {
                for (JsonNode u : data) {
                    List<String> roles = new ArrayList<>();
                    u.path("roles").forEach(r -> roles.add(r.asText()));
                    existingUsers.add(new UserEntry(
                        u.path("id").asInt(),
                        u.path("email").asText(""),
                        u.path("firstName").asText("") + " " + u.path("lastName").asText(""),
                        roles
                    ));
                }
            }
            Platform.runLater(() -> {
                adminCreateTeacherRoot.getChildren().remove(loading);
                buildForm(en);
                buildExistingTeachersTable(en);
            });
        });
        ws.sendAction("LIST_USERS", null);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            ws.unsubscribe(subUsers);
            Platform.runLater(() -> {
                if (adminCreateTeacherRoot.getChildren().contains(loading)) {
                    adminCreateTeacherRoot.getChildren().remove(loading);
                    buildForm(en);
                    buildExistingTeachersTable(en);
                }
            });
        }, 6, TimeUnit.SECONDS);
    }

    private void buildForm(boolean en) {
        VBox card = new VBox(16);
        card.getStyleClass().add("section-card");

        Label hdr = new Label("👤  " + (en ? "New User Details" : "Údaje nového používateľa"));
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        TextField emailFld  = textField("jan.novak@stuba.sk");
        TextField firstFld  = textField("Ján");
        TextField lastFld   = textField("Novák");
        PasswordField passFld = new PasswordField();
        passFld.setPromptText(en ? "password" : "heslo");
        passFld.getStyleClass().add("text-field-custom");
        passFld.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("TEACHER", "STUDENT", "ADMIN");
        roleCombo.setValue("TEACHER");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-font-size:12px;");
        statusLbl.setVisible(false);

        Button createBtn = actionButton(en ? "Create User" : "Vytvoriť používateľa", "#06b6d4");
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
                    // Refresh table
                    refreshTable(en);
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

        HBox row1 = twoColRow(fieldLabel("Email *"), emailFld, fieldLabel(en ? "Role" : "Rola"), roleCombo);
        HBox row2 = twoColRow(fieldLabel(en ? "First Name *" : "Meno *"), firstFld,
                              fieldLabel(en ? "Last Name *" : "Priezvisko *"), lastFld);
        HBox row3 = twoColRow(fieldLabel(en ? "Password *" : "Heslo *"), passFld, new Label(""), new Label(""));

        card.getChildren().addAll(row1, row2, row3,
            new HBox(12, createBtn, statusLbl) {{ setAlignment(Pos.CENTER_LEFT); }});

        adminCreateTeacherRoot.getChildren().add(card);
    }

    private void buildExistingTeachersTable(boolean en) {
        VBox card = new VBox(12);
        card.getStyleClass().add("section-card");
        card.setId("teachersTableCard");

        Label hdr = new Label("📋  " + (en ? "Existing Users" : "Existujúci používatelia"));
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#10b981;");
        card.getChildren().add(hdr);
        card.getChildren().add(divider());

        populateTeachersTable(card, en);
        adminCreateTeacherRoot.getChildren().add(card);
    }

    private void populateTeachersTable(VBox card, boolean en) {
        // Remove old rows (keep header + divider = 2 children)
        while (card.getChildren().size() > 2) {
            card.getChildren().remove(2);
        }

        if (existingUsers.isEmpty()) {
            Label empty = new Label(en ? "No users found." : "Žiadni používatelia nenájdení.");
            empty.getStyleClass().add("schedule-loc");
            card.getChildren().add(empty);
            return;
        }

        // Column header
        HBox colHdr = new HBox();
        colHdr.setAlignment(Pos.CENTER_LEFT);
        colHdr.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;-fx-padding:0 0 6 0;");
        for (String col : new String[]{ en?"Email":"Email", en?"Name":"Meno", en?"Role":"Rola" }) {
            Label l = new Label(col);
            l.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#94a3b8;");
            if (col.equals(en ? "Email" : "Email")) HBox.setHgrow(l, Priority.ALWAYS);
            else l.setMinWidth(160);
            colHdr.getChildren().add(l);
        }
        card.getChildren().add(colHdr);

        for (UserEntry u : existingUsers) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 0, 8, 0));
            row.setStyle("-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");

            Label emailL = new Label(u.email());
            emailL.setStyle("-fx-font-size:13px;-fx-text-fill:#06b6d4;");
            HBox.setHgrow(emailL, Priority.ALWAYS);

            Label nameL = new Label(u.name().trim());
            nameL.getStyleClass().add("schedule-name");
            nameL.setMinWidth(160);

            String rolesStr = String.join(", ", u.roles());
            Label roleL = new Label(rolesStr);
            roleL.getStyleClass().add("schedule-loc");
            roleL.setMinWidth(160);

            row.getChildren().addAll(emailL, nameL, roleL);
            card.getChildren().add(row);
        }
    }

    private void refreshTable(boolean en) {
        String[] sub = new String[1];
        sub[0] = WebSocketClientService.getInstance().subscribe("USER_LIST", node -> {
            WebSocketClientService.getInstance().unsubscribe(sub[0]);
            JsonNode data = node.path("data");
            existingUsers.clear();
            if (data.isArray()) {
                for (JsonNode u : data) {
                    List<String> roles = new ArrayList<>();
                    u.path("roles").forEach(r -> roles.add(r.asText()));
                    existingUsers.add(new UserEntry(
                        u.path("id").asInt(), u.path("email").asText(""),
                        u.path("firstName").asText("") + " " + u.path("lastName").asText(""), roles));
                }
            }
            Platform.runLater(() -> {
                adminCreateTeacherRoot.getChildren().stream()
                    .filter(n -> n instanceof VBox vb && "teachersTableCard".equals(vb.getId()))
                    .forEach(n -> populateTeachersTable((VBox) n, en));
            });
        });
        WebSocketClientService.getInstance().sendAction("LIST_USERS", null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

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
