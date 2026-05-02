package com.example.bais;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

public class SchoolCalendar extends Application {

    private boolean isDarkMode = false;
    private BorderPane root;
    private Scene scene;

    // ── Colour tokens ──────────────────────────────────────────────
    // Light
    private static final String LIGHT_BG          = "#f5f6fa";
    private static final String LIGHT_NAV_BG      = "#ffffff";
    private static final String LIGHT_CARD_BG     = "#ffffff";
    private static final String LIGHT_HEADER_TEXT = "#1a1a2e";
    private static final String LIGHT_SUB_TEXT    = "#6b7280";
    private static final String LIGHT_COL_HEADER  = "#f0f2f8";
    private static final String LIGHT_TODAY_COL   = "#dbeafe";
    private static final String LIGHT_TODAY_NUM   = "#2563eb";
    private static final String LIGHT_BORDER      = "#e5e7eb";

    // Dark
    private static final String DARK_BG           = "#0f1729";
    private static final String DARK_NAV_BG       = "#0f1729";
    private static final String DARK_CARD_BG      = "#1a2744";
    private static final String DARK_HEADER_TEXT  = "#e2e8f0";
    private static final String DARK_SUB_TEXT     = "#94a3b8";
    private static final String DARK_COL_HEADER   = "#1e2d4a";
    private static final String DARK_TODAY_COL    = "#1e3a6e";
    private static final String DARK_TODAY_NUM    = "#60a5fa";
    private static final String DARK_BORDER       = "#2d3f5e";

    // Event colours (work for both themes)
    private static final String COLOR_PREDNASKA   = "#93c5fd"; // blue
    private static final String COLOR_CVICENIE    = "#86efac"; // green
    private static final String COLOR_ODOVZDANIE  = "#fde68a"; // yellow/amber
    private static final String COLOR_TASK_DUE    = "#fca5a5"; // red/pink

    // ── Event data ─────────────────────────────────────────────────
    record CalEvent(String type, String title, String subtitle, String time, int dayIndex) {}

    private final List<CalEvent> events = List.of(
        // Pondelok
        new CalEvent("CVIČENIE", "MAT", "", "08:00 - 09:40", 0),
        new CalEvent("PREDNÁŠKA", "PAS", "", "10:00 - 12:40", 0),
        new CalEvent("PREDNÁŠKA", "MAT", "", "13:00 - 15:40", 0),

        // Utorok
        new CalEvent("CVIČENIE", "PSI", "", "08:00 - 09:40", 1),
        new CalEvent("PREDNÁŠKA", "PSI", "", "10:00 - 12:40", 1),
        new CalEvent("CVIČENIE", "FYZ", "", "13:00 - 14:40", 1),

        // Streda
        new CalEvent("PREDNÁŠKA", "DBS", "", "09:00 - 11:40", 2),
        new CalEvent("CVIČENIE", "DBS", "", "12:00 - 13:40", 2),
        new CalEvent("PRIPOMIENKA", "Štúdium DBS", "", "14:00 - 15:40", 2),

        // Štvrtok
        new CalEvent("PREDNÁŠKA", "VAVA", "", "08:00 - 10:40", 3),
        new CalEvent("CVIČENIE", "PAS", "", "11:00 - 12:40", 3),
        new CalEvent("ODOVZDANIE", "Figma skice", "", "13:00 - 14:40", 3),

        // Piatok
        new CalEvent("CVIČENIE", "VAVA JAVA", "", "08:00 - 09:40", 4),
        new CalEvent("PREDNÁŠKA", "FYZ", "", "10:00 - 12:40", 4),
        new CalEvent("TASK DUE", "Live coding", "", "13:00 - 14:40", 4)
    );

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        scene = new Scene(root, 980, 620);
        applyTheme();

        stage.setTitle("Lepší Akademický systém");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // ── Full rebuild on theme change ────────────────────────────────
    private void applyTheme() {
        root.setTop(buildNavBar());
        root.setCenter(buildContent());
        root.setStyle("-fx-background-color: " + bg() + ";");
    }

    // ── Helpers ─────────────────────────────────────────────────────
    private String bg()         { return isDarkMode ? DARK_BG          : LIGHT_BG; }
    private String navBg()      { return isDarkMode ? DARK_NAV_BG      : LIGHT_NAV_BG; }
    private String cardBg()     { return isDarkMode ? DARK_CARD_BG     : LIGHT_CARD_BG; }
    private String headerText() { return isDarkMode ? DARK_HEADER_TEXT : LIGHT_HEADER_TEXT; }
    private String subText()    { return isDarkMode ? DARK_SUB_TEXT    : LIGHT_SUB_TEXT; }
    private String colHeader()  { return isDarkMode ? DARK_COL_HEADER  : LIGHT_COL_HEADER; }
    private String todayCol()   { return isDarkMode ? DARK_TODAY_COL   : LIGHT_TODAY_COL; }
    private String todayNum()   { return isDarkMode ? DARK_TODAY_NUM   : LIGHT_TODAY_NUM; }
    private String border()     { return isDarkMode ? DARK_BORDER      : LIGHT_BORDER; }

    // ── NAV BAR ─────────────────────────────────────────────────────
    private HBox buildNavBar() {
        HBox nav = new HBox(16);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(10, 20, 10, 16));
        nav.setStyle("-fx-background-color: " + navBg() + "; " +
                "-fx-border-color: " + border() + "; -fx-border-width: 0 0 1 0;");

        // Logo
        StackPane logo = new StackPane();
        logo.setMinSize(34, 34);
        logo.setMaxSize(34, 34);
        logo.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 8;");
        Label logoLbl = new Label("L");
        logoLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        logoLbl.setTextFill(Color.WHITE);
        logo.getChildren().add(logoLbl);

        VBox brand = new VBox(1);
        Label appName = new Label("Lepší Akademický systém");
        appName.setFont(Font.font("System", FontWeight.BOLD, 12));
        appName.setTextFill(Color.web(headerText()));
        Label appSub = new Label("AKADEMICKÝ KOM JAVA");
        appSub.setFont(Font.font("System", 9));
        appSub.setTextFill(Color.web(subText()));
        brand.getChildren().addAll(appName, appSub);

        // Search
        TextField search = new TextField();
        search.setPromptText("Search academic records...");
        search.setPrefWidth(220);
        search.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#1e2d4a" : "#f0f2f8") + ";" +
                        "-fx-border-color: " + border() + "; -fx-border-radius: 20; -fx-background-radius: 20;" +
                        "-fx-padding: 5 12 5 12; -fx-text-fill: " + headerText() + "; -fx-prompt-text-fill: " + subText() + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Icon buttons (🔔 📅 ⚙ or ☾)
        Button notifBtn = iconButton("🔔");
        Button calBtn   = iconButton("📅");

        // Dark/Light toggle
        Button toggleBtn = iconButton(isDarkMode ? "☀" : "☾");
        toggleBtn.setOnAction(e -> { isDarkMode = !isDarkMode; applyTheme(); });
        styleIconButton(toggleBtn);

        nav.getChildren().addAll(logo, brand, search, spacer, notifBtn, calBtn, toggleBtn);
        return nav;
    }

    private Button iconButton(String icon) {
        Button btn = new Button(icon);
        styleIconButton(btn);
        return btn;
    }

    private void styleIconButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 15; " +
                        "-fx-cursor: hand; -fx-padding: 4 8 4 8;"
        );
        btn.setTextFill(Color.web(subText()));
    }

    // ── MAIN CONTENT ────────────────────────────────────────────────
    private VBox buildContent() {
        VBox content = new VBox(32);
        content.setPadding(new Insets(72, 64, 72, 64));
        content.setStyle("-fx-background-color: #f8f9ff;");

        // Title row
        HBox titleRow = new HBox();
        titleRow.setPrefHeight(72);
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        
        VBox titleBlock = new VBox(8);
        titleBlock.setPrefWidth(326);
        titleBlock.setAlignment(Pos.BOTTOM_LEFT);
        
        Label title = new Label("Kalendár školy");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: #000000; -fx-letter-spacing: -0.9px;");
        
        Label sub = new Label("Siedmy týždeň");
        sub.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 16px; -fx-font-weight: 400; -fx-text-fill: #000000;");
        
        titleBlock.getChildren().addAll(title, sub);

        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(titleBlock, tSpacer);

        // Bento Container
        VBox bentoContainer = new VBox(4);
        bentoContainer.setPadding(new Insets(4));
        bentoContainer.setPrefSize(1201, 411);
        bentoContainer.setStyle("-fx-background-color: #edf5ff; -fx-background-radius: 8;");

        // Calendar grid
        GridPane grid = buildCalendarGrid();
        VBox.setVgrow(grid, Priority.ALWAYS);
        bentoContainer.getChildren().add(grid);

        // Legend
        HBox legend = buildLegend();

        content.getChildren().addAll(titleRow, bentoContainer, legend);
        return content;
    }

    // ── VIEW TOGGLE BUTTONS ──────────────────────────────────────────
    private HBox buildViewToggle() {
        HBox box = new HBox(0);
        box.setStyle("-fx-border-color: " + border() + "; -fx-border-radius: 8; -fx-background-radius: 8;");
        String[] labels = {"Týždeň", "Mesiac", "Agenda"};
        for (int i = 0; i < labels.length; i++) {
            Button b = new Button(labels[i]);
            boolean first = i == 0;
            b.setStyle(
                    "-fx-background-color: " + (first ? (isDarkMode ? "#2d3f5e" : "#e5e7eb") : "transparent") + ";" +
                            "-fx-font-size: 12; -fx-padding: 5 14 5 14; -fx-cursor: hand;" +
                            "-fx-text-fill: " + (first ? headerText() : subText()) + ";" +
                            "-fx-background-radius: " + (i == 0 ? "8 0 0 8" : i == 2 ? "0 8 8 0" : "0") + ";"
            );
            box.getChildren().add(b);
        }
        return box;
    }

    // ── CALENDAR GRID ────────────────────────────────────────────────
    private static final String[] DAYS     = {"MON", "TUE", "WED", "THU", "FRI"};
    private static final String[] NUMS     = {"12",  "13",  "14",  "15",  "16"};
    private static final int TODAY_IDX     = 2; // WED 14 is highlighted

    private GridPane buildCalendarGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setStyle("-fx-background-color: transparent;");

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

        for (int d = 0; d < 5; d++) {
            grid.add(buildDayRowHeader(d), 0, d + 1);
            
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
        GridPane.setMargin(card, new Insets(2, 2, 2, 2));
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
        label.setStyle("-fx-background-color: #FFFFFF; -fx-font-weight: 600; -fx-font-size: 10px; -fx-text-fill: #464832; -fx-text-transform: uppercase; -fx-letter-spacing: 1px;");
        return label;
    }

    private VBox buildDayRowHeader(int d) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #FFFFFF;");

        Label dayLbl = new Label(DAYS[d]);
        dayLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 600; -fx-font-size: 10px; -fx-text-fill: #464832; -fx-letter-spacing: 1px; -fx-text-transform: uppercase;");
        
        Label numLbl = new Label(NUMS[d]);
        numLbl.setStyle("-fx-font-family: 'Inter'; -fx-font-weight: 600; -fx-font-size: 20px; -fx-text-fill: #1B1D0F;");

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

    private String eventColor(String type) {
        return switch (type) {
            case "PREDNÁŠKA"  -> COLOR_PREDNASKA;
            case "CVIČENIE"   -> COLOR_CVICENIE;
            case "ODOVZDANIE" -> COLOR_ODOVZDANIE;
            case "TASK DUE"   -> COLOR_TASK_DUE;
            default           -> "#e2e8f0";
        };
    }

    // ── LEGEND ──────────────────────────────────────────────────────
    private HBox buildLegend() {
        HBox legend = new HBox(24);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(0));
        legend.setPrefHeight(16);

        legend.getChildren().addAll(
            legendItem("#2563EB", "Prednášky"),
            legendItem("#16A34A", "Cvičenia"),
            legendItem("#DC2626", "Odovzdania")
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

    public static void main(String[] args) {
        launch(args);
    }
}