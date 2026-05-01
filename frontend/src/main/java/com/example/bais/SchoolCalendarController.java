package com.example.bais;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SchoolCalendarController implements Initializable {

    @FXML private VBox calendarRoot;

    // ── Event model ────────────────────────────────────────────────
    record CalEvent(String type, String title, String time, int dayIndex) {}

    /** Mutable list – user reminders are appended at runtime. */
    private final List<CalEvent> events = new ArrayList<>(List.of(
        // Pondelok
        new CalEvent("CVIČENIE", "MAT", "08:00 - 09:40", 0),
        new CalEvent("PREDNÁŠKA", "PAS", "10:00 - 12:40", 0),
        new CalEvent("PREDNÁŠKA", "MAT", "13:00 - 15:40", 0),

        // Utorok
        new CalEvent("CVIČENIE", "PSI", "08:00 - 09:40", 1),
        new CalEvent("PREDNÁŠKA", "PSI", "10:00 - 12:40", 1),
        new CalEvent("CVIČENIE", "FYZ", "13:00 - 14:40", 1),

        // Streda
        new CalEvent("PREDNÁŠKA", "DBS", "09:00 - 11:40", 2),
        new CalEvent("CVIČENIE", "DBS", "12:00 - 13:40", 2),
        new CalEvent("PRIPOMIENKA", "Štúdium DBS", "14:00 - 15:40", 2),

        // Štvrtok
        new CalEvent("PREDNÁŠKA", "VAVA", "08:00 - 10:40", 3),
        new CalEvent("CVIČENIE", "PAS", "11:00 - 12:40", 3),
        new CalEvent("ODOVZDANIE", "Figma skice", "13:00 - 14:40", 3),

        // Piatok
        new CalEvent("CVIČENIE", "VAVA JAVA", "08:00 - 09:40", 4),
        new CalEvent("PREDNÁŠKA", "FYZ", "10:00 - 12:40", 4),
        new CalEvent("TASK DUE", "Live coding", "13:00 - 14:40", 4)
    ));

    private static final String[] DAYS_SK = {"PON", "UTO", "STR", "ŠTV", "PIA"};
    private static final String[] DAYS_EN = {"MON", "TUE", "WED", "THU", "FRI"};
    private static final String[] NUMS    = {"12",  "13",  "14",  "15",  "16"};
    private static final int TODAY_IDX    = 2;

    // Grid reference kept so we can refresh it without rebuilding the whole page
    private GridPane calendarGrid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildCalendarUI();
    }

    // ── Full UI build ──────────────────────────────────────────────

    private void buildCalendarUI() {
        boolean en = UserSession.get().isEnglish();

        calendarRoot.getChildren().clear();
        calendarRoot.setSpacing(32);
        calendarRoot.setPadding(new Insets(72, 64, 72, 64));
        calendarRoot.setStyle("-fx-background-color: #f8f9ff;");

        // ── Title row ──────────────────────────────────────────────
        HBox titleRow = new HBox();
        titleRow.setPrefWidth(1201);
        titleRow.setPrefHeight(72);
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        titleRow.setSpacing(127.13);

        VBox titleBlock = new VBox(8);
        titleBlock.setPrefWidth(326);
        titleBlock.setAlignment(Pos.BOTTOM_LEFT);

        Label title = new Label(en ? "School Calendar" : "Kalendár školy");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: #000000; -fx-letter-spacing: -0.9px;");
        
        Label sub = new Label(en ? "Week 7" : "Siedmy týždeň");
        sub.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 16px; -fx-font-weight: 400; -fx-text-fill: #000000;");
        
        titleBlock.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── "+ Add Reminder" button ────────────────────────────────
        Button addBtn = new Button(en ? "+ Add Reminder" : "+ Pridať pripomienku");
        addBtn.setStyle(
            "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-font-size: 12px;" +
            "-fx-padding: 8 18; -fx-background-radius: 10; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddReminderDialog());

        titleRow.getChildren().addAll(titleBlock, spacer, addBtn);

        // ── Grid Container ────────────────────────────────────────
        VBox gridContainer = new VBox(4);
        gridContainer.setPadding(new Insets(4));
        gridContainer.setPrefSize(1201, 411);
        gridContainer.setStyle("-fx-background-color: #edf5ff; -fx-background-radius: 8;");

        calendarGrid = buildGrid();
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        gridContainer.getChildren().add(calendarGrid);

        // ── Legend ────────────────────────────────────────────────
        HBox legend = buildLegend();

        calendarRoot.getChildren().addAll(titleRow, gridContainer, legend);
    }

    // ── "Add Reminder" dialog ──────────────────────────────────────

    private void showAddReminderDialog() {
        boolean en = UserSession.get().isEnglish();

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(en ? "New Reminder" : "Nová pripomienka");
        stage.setResizable(false);

        VBox dialog = new VBox(14);
        dialog.setPadding(new Insets(24));
        dialog.setPrefWidth(400);
        dialog.setStyle("-fx-background-color: #f8fafc;");

        // ── Dialog title ──────────────────────────────────────────
        Label dlgTitle = new Label(en ? "New Reminder" : "Nová pripomienka");
        dlgTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        // ── Title field ───────────────────────────────────────────
        Label titleLbl = new Label(en ? "Title" : "Názov");
        titleLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        TextField titleField = new TextField();
        titleField.setPromptText(en ? "e.g. Study for DBS exam" : "napr. Štúdium na skúšku DBS");
        titleField.setStyle(
            "-fx-background-radius: 8; -fx-border-radius: 8;" +
            "-fx-border-color: #e2e8f0; -fx-border-width: 1;" +
            "-fx-padding: 8 10; -fx-font-size: 13px; -fx-background-color: white;");

        // ── Day picker ────────────────────────────────────────────
        Label dayLbl = new Label(en ? "Day" : "Deň");
        dayLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        ComboBox<String> dayCombo = new ComboBox<>();
        dayCombo.getItems().addAll(en
            ? new String[]{"Monday (12)", "Tuesday (13)", "Wednesday (14)", "Thursday (15)", "Friday (16)"}
            : new String[]{"Pondelok (12)", "Utorok (13)", "Streda (14)", "Štvrtok (15)", "Piatok (16)"});
        dayCombo.getSelectionModel().select(0);
        dayCombo.setMaxWidth(Double.MAX_VALUE);

        // ── Slot (row) picker ─────────────────────────────────────
        Label rowLbl = new Label(en ? "Slot" : "Slot");
        rowLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        ComboBox<String> rowCombo = new ComboBox<>();
        rowCombo.getItems().addAll(
            en ? "Row 1 (morning)"   : "Riadok 1 (dopoludnie)",
            en ? "Row 2 (afternoon)" : "Riadok 2 (popoludnie)");
        rowCombo.getSelectionModel().select(0);
        rowCombo.setMaxWidth(Double.MAX_VALUE);

        // ── Time field ────────────────────────────────────────────
        Label timeLbl = new Label(en ? "Time" : "Čas");
        timeLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        TextField timeField = new TextField();
        timeField.setPromptText("14:00");
        timeField.setStyle(
            "-fx-background-radius: 8; -fx-border-radius: 8;" +
            "-fx-border-color: #e2e8f0; -fx-border-width: 1;" +
            "-fx-padding: 8 10; -fx-font-size: 13px; -fx-background-color: white;");

        // ── Error label ───────────────────────────────────────────
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        // ── Buttons ───────────────────────────────────────────────
        Button cancelBtn = new Button(en ? "Cancel" : "Zrušiť");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setStyle(
            "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;" +
            "-fx-font-weight: bold; -fx-padding: 9 20; -fx-background-radius: 10; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button(en ? "Add to Calendar" : "Pridať do kalendára");
        saveBtn.setPrefWidth(160);
        saveBtn.setStyle(
            "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
            "-fx-font-weight: bold; -fx-padding: 9 20; -fx-background-radius: 10; -fx-cursor: hand;");

        HBox btnRow = new HBox(10, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(6, 0, 0, 0));

        dialog.getChildren().addAll(
            dlgTitle,
            new Separator(),
            titleLbl, titleField,
            dayLbl,   dayCombo,
            rowLbl,   rowCombo,
            timeLbl,  timeField,
            errorLbl, btnRow
        );

        // ── Save action ───────────────────────────────────────────
        saveBtn.setOnAction(e -> {
            String evTitle = titleField.getText().trim();
            String evTime  = timeField.getText().trim();
            int    dayIdx  = dayCombo.getSelectionModel().getSelectedIndex();

            if (evTitle.isEmpty()) {
                errorLbl.setText(en ? "Please enter a title." : "Zadajte názov pripomienky.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            String typeLabel   = en ? "REMINDER" : "PRIPOMIENKA";
            String displayTime = evTime.isEmpty() ? "12:00 - 13:00" : evTime;
            if (!displayTime.contains("-")) displayTime = displayTime + " - " + displayTime;

            events.add(new CalEvent(typeLabel, evTitle, displayTime, dayIdx));
            refreshGrid();
            stage.close();
        });

        Scene scene = new Scene(dialog);
        stage.setScene(scene);
        stage.showAndWait();
    }

    /** Rebuild only the grid (keeps the title row and legend intact). */
    private void refreshGrid() {
        int gridIndex = calendarRoot.getChildren().indexOf(calendarGrid);
        calendarGrid = buildGrid();
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        if (gridIndex >= 0) calendarRoot.getChildren().set(gridIndex, calendarGrid);
    }

    // ── Grid ──────────────────────────────────────────────────────

    private HBox buildViewToggle() {
        HBox box = new HBox(0);
        box.getStyleClass().add("view-toggle-group");
        String[] labels = UserSession.get().isEnglish()
            ? new String[]{"Week", "Month", "Agenda"}
            : new String[]{"Týždeň", "Mesiac", "Agenda"};
        for (int i = 0; i < labels.length; i++) {
            Button b = new Button(labels[i]);
            b.setPadding(new Insets(5, 14, 5, 14));
            b.getStyleClass().add(i == 0 ? "view-toggle-active" : "view-toggle-btn");
            b.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-border-color: transparent;");
            box.getChildren().add(b);
        }
        return box;
    }

    private GridPane buildGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.getStyleClass().add("calendar-grid");
        grid.setStyle("-fx-background-color: #181f38;");

        // Column 0: Days
        ColumnConstraints c0 = new ColumnConstraints(120);
        grid.getColumnConstraints().add(c0);

        // Columns 1-26: 30-min slots from 08:00 to 21:00 (13 hours * 2 = 26 slots)
        for (int i = 0; i < 26; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(40);
            grid.getColumnConstraints().add(cc);
        }

        // Header Row for times (Hours)
        RowConstraints hr = new RowConstraints(40);
        grid.getRowConstraints().add(hr);
        
        for (int h = 8; h <= 20; h++) {
            Label hourLbl = buildTimeHeader(String.format("%02d:00", h));
            grid.add(hourLbl, (h - 8) * 2 + 1, 0, 2, 1);
        }

        // Rows for days
        for (int i = 0; i < 5; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(100);
            grid.getRowConstraints().add(rc);
        }

        boolean en = UserSession.get().isEnglish();
        String[] days = en ? DAYS_EN : DAYS_SK;
        // Add a background container for each day's events
        for (int d = 0; d < 5; d++) {
            grid.add(buildDayRowHeader(d, days), 0, d + 1);
            
            final int dayIdx = d;
            for (CalEvent ev : events) {
                if (ev.dayIndex() == dayIdx) {
                    addEventToGrid(grid, ev, d + 1);
                }
            }
        }

        return grid;
    }

    private void addEventToGrid(GridPane grid, CalEvent ev, int row) {
        String[] parts = ev.time().split("-");
        if (parts.length < 1) return;
        
        double startMinutes = parseToMinutes(parts[0].trim());
        double endMinutes = (parts.length > 1) ? parseToMinutes(parts[parts.length - 1].trim()) : startMinutes + 100; // default 1:40
        
        double startOffset = Math.max(0, startMinutes - (8 * 60));
        double duration = endMinutes - startMinutes;
        if (duration <= 0) duration = 100;

        int startCol = (int) Math.round(startOffset / 30) + 1;
        int colSpan = (int) Math.round(duration / 30);
        
        if (startCol < 1) startCol = 1;
        if (startCol > 26) return;
        if (startCol + colSpan > 27) colSpan = 27 - startCol;

        VBox card = buildEventCard(ev);
        grid.add(card, startCol, row, colSpan, 1);
    }

    private double parseToMinutes(String timeStr) {
        try {
            String clean = timeStr.toUpperCase().replace(" ", "");
            boolean pm = clean.endsWith("PM");
            boolean am = clean.endsWith("AM");
            clean = clean.replace("PM", "").replace("AM", "");
            
            String[] parts = clean.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
            
            if (pm && h < 12) h += 12;
            if (am && h == 12) h = 0;
            
            return h * 60 + m;
        } catch (Exception e) {
            return 8 * 60; // Default to 8 AM
        }
    }

    private Label buildTimeHeader(String time) {
        Label label = new Label(time);
        label.setAlignment(Pos.CENTER);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        boolean dark = !UserSession.get().isEnglish() && false; // Placeholder for theme check if needed
        // We need a better way to check dark mode here. SchoolCalendarController doesn't seem to have isDarkMode.
        // But the requirement says "detaily ako calendar pod : #181f38" - assuming this is for the background of calendar cells or headers.
        label.setStyle("-fx-background-color: transparent; -fx-font-weight: 600; -fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-text-transform: uppercase; -fx-letter-spacing: 1px;");
        return label;
    }

    private VBox buildDayRowHeader(int d, String[] days) {
        boolean today = (d == TODAY_IDX);
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.getStyleClass().add(today ? "calendar-day-header-today" : "calendar-day-header");
        box.setStyle("-fx-background-color: transparent;");

        Label dayLbl = new Label(days[d]);
        dayLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 600; -fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1px; -fx-text-transform: uppercase;");
        
        Label numLbl = new Label(NUMS[d]);
        numLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 600; -fx-font-size: 20px; -fx-text-fill: #e2e8f0;");

        box.getChildren().addAll(dayLbl, numLbl);
        return box;
    }


    private VBox buildEventCard(CalEvent ev) {
        boolean isReminder = ev.type().equals("PRIPOMIENKA") || ev.type().equals("REMINDER");
        boolean isCvicenie = ev.type().equals("CVIČENIE");
        boolean isPrednaska = ev.type().equals("PREDNÁŠKA");
        boolean isOdovzdanie = ev.type().equals("ODOVZDANIE") || ev.type().equals("TASK DUE");

        VBox card = new VBox(3.3);
        card.setPadding(new Insets(12));
        card.setPrefHeight(83.05);

        String bgColor = "#DBEAFE";
        String borderColor = "#2563EB";
        String typeColor = "#1E40AF";
        String titleColor = "#1E3A8A";
        String timeColor = "#1D4ED8";

        if (isCvicenie) {
            bgColor = "#DCFCE7"; borderColor = "#16A34A"; typeColor = "#166534"; titleColor = "#14532D"; timeColor = "#15803D";
        } else if (isOdovzdanie) {
            bgColor = "#FEE2E2"; borderColor = "#DC2626"; typeColor = "#991B1B"; titleColor = "#7F1D1D"; timeColor = "#B91C1C";
        } else if (isReminder) {
            bgColor = "#EDEFFF"; borderColor = "#7c3aed"; typeColor = "#7c3aed"; titleColor = "#4c1d95"; timeColor = "#4c1d95";
        }

        card.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: transparent transparent transparent " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-background-radius: 4; -fx-border-radius: 4;");

        Label typeLbl = new Label(ev.type());
        typeLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 600; -fx-font-size: 10px; -fx-text-fill: " + typeColor + "; -fx-text-transform: uppercase;");

        Label titleLbl = new Label(ev.title());
        titleLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: " + titleColor + ";");
        titleLbl.setWrapText(true);

        Label timeLbl = new Label(ev.time());
        timeLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 400; -fx-font-size: 10px; -fx-text-fill: " + timeColor + ";");

        card.getChildren().addAll(typeLbl, titleLbl, timeLbl);
        return card;
    }

    private String cssClass(String type) {
        return switch (type) {
            case "PREDNÁŠKA"  -> "prednaska";
            case "CVIČENIE"   -> "cvicenie";
            case "ODOVZDANIE" -> "odovzdanie";
            case "TASK DUE"   -> "taskdue";
            default           -> "default";
        };
    }

    // ── Legend ────────────────────────────────────────────────────

    private HBox buildLegend() {
        boolean en = UserSession.get().isEnglish();
        HBox legend = new HBox(24);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(0));
        legend.setPrefHeight(16);

        legend.getChildren().addAll(
            legendItem("#2563EB", en ? "Lectures" : "Prednášky"),
            legendItem("#16A34A", en ? "Labs" : "Cvičenia"),
            legendItem("#DC2626", en ? "Assignments" : "Odovzdania")
        );
        return legend;
    }

    private HBox legendItem(String color, String label) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        Circle circle = new Circle(6, Color.web(color));
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 500; -fx-font-size: 12px; -fx-text-fill: #000000;");
        item.getChildren().addAll(circle, lbl);
        return item;
    }
}