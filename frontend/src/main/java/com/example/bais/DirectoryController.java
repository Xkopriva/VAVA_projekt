package com.example.bais;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DirectoryController implements Initializable {

    @FXML private VBox dirRoot;

    record Person(String name, String role, String dept, String email, String initials, String color) {}

    private final List<Person> people = List.of(
        new Person("prof. RNDr. Jana Kováčová",   "Algebra a matematika",         "KAI",  "kovacova@fiit.stuba.sk",  "JK", "#06b6d4"),
        new Person("Ing. Peter Horváth",           "Databázové systémy",           "KKUI", "horvath@fiit.stuba.sk",   "PH", "#f87171"),
        new Person("doc. Mgr. Eva Blahová",        "PAS, Algoritmy a dátové štruktúry", "KAI",  "blahova@fiit.stuba.sk",   "EB", "#4ade80"),
        new Person("RNDr. Tomáš Mináč",            "Počítačové siete I",           "KKUI", "minac@fiit.stuba.sk",     "TM", "#fb923c"),
        new Person("Mgr. Lucia Šimková",           "Vývoj aplikácií (VAVA)",       "KAI",  "simkova@fiit.stuba.sk",   "LS", "#a78bfa"),
        new Person("doc. Ing. Marek Dlugoš",       "Fyzika pre informatikov",      "KF",   "dlugos@fiit.stuba.sk",    "MD", "#60a5fa"),
        new Person("PhDr. Andrea Horníková",       "Anglický jazyk B2",            "KJ",   "hornikova@fiit.stuba.sk", "AH", "#f472b6")
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
    }

    private void buildUI() {
        dirRoot.getChildren().clear();
        dirRoot.setSpacing(16);
        dirRoot.setPadding(new Insets(24, 28, 24, 28));

        // Title
        VBox titleBlock = new VBox(4);
        Label title = new Label("Adresár pedagógov");
        title.setStyle("-fx-font-size:26px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label("Kontakty pedagógov a zamestnancov FIIT STU Bratislava");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);

        // Search field
        TextField search = new TextField();
        search.setPromptText("🔍  Hľadať podľa mena, predmetu alebo katedry...");
        search.getStyleClass().add("search-bar");
        search.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:10;-fx-padding:10 14;" +
            "-fx-border-color:#e2e8f0;-fx-border-radius:10;-fx-border-width:1;-fx-font-size:14px;");
        search.setMaxWidth(Double.MAX_VALUE);

        // Filter chips
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        for (String chip : new String[]{"Všetci", "KAI", "KKUI", "KF", "KJ"}) {
            Label c = new Label(chip);
            c.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 12 5 12;" +
                "-fx-background-radius:20;-fx-cursor:hand;" +
                (chip.equals("Všetci")
                    ? "-fx-background-color:#06b6d4;-fx-text-fill:white;"
                    : "-fx-background-color:#f1f5f9;-fx-text-fill:#64748b;"));
            chips.getChildren().add(c);
        }

        // People cards
        VBox list = new VBox(10);
        for (Person p : people) {
            list.getChildren().add(buildPersonCard(p));
        }

        dirRoot.getChildren().addAll(titleBlock, search, chips, list);
    }

    private HBox buildPersonCard(Person p) {
        HBox card = new HBox(16);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Avatar circle
        StackPane avatar = new StackPane();
        avatar.setMinWidth(48);
        avatar.setMinHeight(48);
        Circle circle = new Circle(24, Color.web(p.color() + "33"));
        Label initials = new Label(p.initials());
        initials.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + p.color() + ";");
        avatar.getChildren().addAll(circle, initials);

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.name());
        name.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        name.getStyleClass().add("schedule-name");

        Label role = new Label(p.role());
        role.getStyleClass().add("schedule-loc");

        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label dept = new Label(p.dept());
        dept.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2 6 2 6;" +
            "-fx-background-radius:4;-fx-background-color:#f1f5f9;-fx-text-fill:#64748b;");
        Label email = new Label(p.email());
        email.setStyle("-fx-font-size:11px;-fx-text-fill:#06b6d4;");
        meta.getChildren().addAll(dept, email);

        info.getChildren().addAll(name, role, meta);

        // Buttons
        HBox btns = new HBox(8);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button emailBtn = new Button("E-mail");
        emailBtn.getStyleClass().add("btn-secondary");
        emailBtn.setStyle("-fx-background-color:transparent;-fx-border-color:#06b6d4;" +
            "-fx-border-radius:8;-fx-border-width:1;-fx-text-fill:#06b6d4;" +
            "-fx-font-weight:bold;-fx-padding:6 12;-fx-font-size:11px;-fx-cursor:hand;");
        Button profileBtn = new Button("Profil");
        profileBtn.setStyle("-fx-background-color:#06b6d4;-fx-background-radius:8;" +
            "-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 12;-fx-font-size:11px;-fx-cursor:hand;");
        btns.getChildren().addAll(emailBtn, profileBtn);

        card.getChildren().addAll(avatar, info, btns);
        return card;
    }
}
