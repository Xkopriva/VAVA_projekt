package com.example.bais;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CoursesController implements Initializable {

    @FXML private VBox coursesRoot;

    record Course(String code, String name, String teacher, int credits, String type, String time, String room, String accent) {}

    private final List<Course> courses = List.of(
        new Course("PAS",  "Programovanie a softvér I",     "doc. Blahová",     6, "Prednáška + Cvičenie", "Po 08:30, St 10:00", "D-105", "#93c5fd"),
        new Course("DBS",  "Databázové systémy",             "Ing. Horváth",     4, "Prednáška + Cvičenie", "Ut 10:00, Št 12:00", "A-204", "#86efac"),
        new Course("PSI",  "Počítačové siete I",             "RNDr. Mináč",      4, "Prednáška",            "St 13:00",           "B-301", "#fca5a5"),
        new Course("VAVA", "Vývoj aplikácií",                "Mgr. Šimková",     6, "Prednáška + Cvičenie", "Ut 08:30, Pi 15:30", "D-101", "#fde68a"),
        new Course("ALM",  "Algebra a matematika",           "prof. Kováčová",   5, "Prednáška",            "Po 13:00",           "B-301", "#c4b5fd")
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildUI();
    }

    private void buildUI() {
        coursesRoot.getChildren().clear();
        coursesRoot.setSpacing(16);
        coursesRoot.setPadding(new Insets(24, 28, 24, 28));

        // Title + action buttons
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label("Zapísané kurzy");
        title.setStyle("-fx-font-size:32px;-fx-font-weight:bold;");
        title.getStyleClass().add("welcome-title");
        Label sub = new Label("Zimný semester 2024/25 • 5 predmetov • 25 kreditov");
        sub.getStyleClass().add("welcome-sub");
        titleBlock.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);


        titleRow.getChildren().add(titleBlock);

        // Summary stats
        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard("5",    "Predmetov"),
            statCard("25",   "Kreditov"),
            statCard("12h",  "Týždenné hodiny"),
            statCard("2",    "Laboratóriá")
        );

        // Course cards grid
        VBox cardList = new VBox(12);
        for (Course c : courses) {
            cardList.getChildren().add(buildCourseCard(c));
        }

        coursesRoot.getChildren().addAll(titleRow, stats, cardList);
    }

    private HBox buildCourseCard(Course c) {
        HBox card = new HBox(0);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(0));
        card.setStyle("-fx-background-radius:12;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        // Color accent bar
        Region bar = new Region();
        bar.setMinWidth(6);
        bar.setPrefWidth(6);
        bar.setStyle("-fx-background-color:" + c.accent() + ";-fx-background-radius:12 0 0 12;");

        // Content
        HBox content = new HBox(16);
        content.setPadding(new Insets(16, 18, 16, 18));
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Code badge
        VBox codeBadge = new VBox();
        codeBadge.setAlignment(Pos.CENTER);
        codeBadge.setMinWidth(60);
        codeBadge.setMinHeight(60);
        codeBadge.setStyle("-fx-background-color:" + c.accent() + ";-fx-background-radius:12;");
        Label codeLbl = new Label(c.code());
        codeLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        codeBadge.getChildren().add(codeLbl);

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(c.name());
        name.setStyle("-fx-font-size:15px;-fx-font-weight:bold;");
        name.getStyleClass().add("schedule-name");

        Label teacher = new Label("👤  " + c.teacher());
        teacher.getStyleClass().add("schedule-loc");

        HBox meta = new HBox(14);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label type = new Label("📖  " + c.type());
        type.getStyleClass().add("schedule-loc");

        Label time = new Label("⏰  " + c.time());
        time.getStyleClass().add("schedule-loc");

        Label room = new Label("📍  " + c.room());
        room.getStyleClass().add("schedule-loc");

        meta.getChildren().addAll(type, time, room);
        info.getChildren().addAll(name, teacher, meta);

        // Right: credits badge
        VBox credBox = new VBox(4);
        credBox.setAlignment(Pos.CENTER);
        credBox.setMinWidth(70);
        Label credNum = new Label(String.valueOf(c.credits()));
        credNum.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label credLbl = new Label("kreditov");
        credLbl.getStyleClass().add("perf-course");
        credBox.getChildren().addAll(credNum, credLbl);

        content.getChildren().addAll(codeBadge, info, credBox);
        card.getChildren().addAll(bar, content);
        return card;
    }

    private VBox statCard(String value, String label) {
        VBox card = new VBox(4);
        card.getStyleClass().add("section-card");
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label val = new Label(value);
        val.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#06b6d4;");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("perf-course");
        card.getChildren().addAll(val, lbl);
        return card;
    }
}
