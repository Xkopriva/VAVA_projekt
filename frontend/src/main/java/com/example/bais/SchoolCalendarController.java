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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SchoolCalendarController implements Initializable {

    @FXML private VBox calendarRoot;

    record CalEvent(String type, String title, String time, int dayIndex) {}

    private final List<CalEvent> userReminders = new ArrayList<>();
    private final List<CalEvent> backendEvents = new ArrayList<>();

    private static final String[] DAYS_SK = {"PON", "UTO", "STR", "ŠTV", "PIA"};
    private static final String[] DAYS_EN = {"MON", "TUE", "WED", "THU", "FRI"};

    private final String[] weekNums = new String[5];
    private final String[] fullDates = new String[5];
    private int todayIdx = -1;

    private GridPane calendarGrid;
    private VBox gridContainer;

    private String eventSubId;
    private boolean eventsLoaded = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        computeCurrentWeek();
        loadReminders();
        buildCalendarUI();
        fetchEvents();

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (!eventsLoaded) {
                if (eventSubId != null)
                    WebSocketClientService.getInstance().unsubscribe(eventSubId);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void computeCurrentWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("d");
        DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("d.M.");
        for (int i = 0; i < 5; i++) {
            LocalDate d = monday.plusDays(i);
            weekNums[i] = d.format(dayFmt);
            fullDates[i] = d.format(fullFmt);
            if (d.equals(today)) todayIdx = i;
        }
    }

    private void fetchEvents() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        eventSubId = ws.subscribe("MY_CALENDAR_EVENTS", this::handleCalendarEvents);
        ws.sendAction("GET_MY_CALENDAR", null);
    }

    private void handleCalendarEvents(JsonNode message) {
        WebSocketClientService.getInstance().unsubscribe(eventSubId);
        eventsLoaded = true;
        JsonNode data = message.path("data");
        List<CalEvent> fetched = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode node : data) {
                CalEvent ev = parseNode(node);
                if (ev != null) fetched.add(ev);
            }
        }
        Platform.runLater(() -> {
            backendEvents.clear();
            backendEvents.addAll(fetched);
            refreshGrid();
        });
    }

    private CalEvent parseNode(JsonNode node) {
        try {
            String rawType = node.path("type").asText("PREDNASKA");
            String title;
            String scheduledAt;
            int durationMinutes = node.path("durationMinutes").asInt(90);

            String calType = switch (rawType) {
                case "PREDNASKA"  -> "PREDNÁŠKA";
                case "CVICENIE"   -> "CVIČENIE";
                case "ODOVZDANIE" -> "ODOVZDANIE";
                case "TASK", "TASK_DUE" -> "TASK DUE";
                case "EXAM", "PISOMKA"  -> "ODOVZDANIE";
                default           -> "PRIPOMIENKA";
            };

            if (rawType.equals("TASK") || rawType.equals("TASK_DUE") || rawType.equals("ODOVZDANIE") || rawType.equals("EXAM") || rawType.equals("PISOMKA")) {
                title = node.path("title").asText("Úloha");
                scheduledAt = node.path("scheduledAt").asText();
                durationMinutes = node.path("durationMinutes").asInt(30);
            } else {
                title = node.path("title").asText(node.path("subjectCode").asText("Event"));
                scheduledAt = node.path("scheduledAt").asText();
            }

            if (scheduledAt != null && !scheduledAt.isBlank()) {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(scheduledAt);
                int dayIndex = odt.getDayOfWeek().getValue() - 1;
                if (dayIndex >= 0 && dayIndex <= 4) {
                    String time;
                    if (calType.equals("TASK DUE") || calType.equals("ODOVZDANIE")) {
                        time = String.format("%02d:%02d", odt.getHour(), odt.getMinute());
                    } else {
                        int hour = odt.getHour();
                        int minute = odt.getMinute();
                        int endTotalMin = hour * 60 + minute + durationMinutes;
                        time = String.format("%02d:%02d - %02d:%02d", hour, minute, endTotalMin / 60, endTotalMin % 60);
                    }
                    return new CalEvent(calType, title, time, dayIndex);
                }
            }
        } catch (Exception e) {
            System.err.println("[Calendar] Could not parse event: " + e.getMessage());
        }
        return null;
    }

    private void buildCalendarUI() {
        boolean en = UserSession.get().isEnglish();
        calendarRoot.getChildren().clear();
        calendarRoot.setSpacing(24);
        calendarRoot.setPadding(new Insets(32, 40, 80, 40));

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label title = new Label(en ? "School Calendar" : "Kalendár školy");
        title.getStyleClass().add("calendar-title");

        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate friday = monday.plusDays(4);
        DateTimeFormatter df = DateTimeFormatter.ofPattern(en ? "MMM d" : "d.M.");
        String weekRange = monday.format(df) + " – " + friday.format(df) + " " + today.getYear();
        Label sub = new Label(weekRange);
        sub.getStyleClass().add("calendar-subtitle");

        titleBlock.getChildren().addAll(title, sub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        // MODERNEJSÍ DIZAJN TLAČIDLA
        Button addBtn = new Button(en ? "+ Add Reminder" : "+ Pridať pripomienku");
        addBtn.setCursor(javafx.scene.Cursor.HAND);
        addBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2563EB, #1D4ED8); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12 24; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(37, 99, 235, 0.4), 10, 0, 0, 4);"
        );
        addBtn.setOnAction(e -> showAddReminderDialog());

        titleRow.getChildren().addAll(titleBlock, addBtn);

        gridContainer = new VBox(4);
        gridContainer.setPadding(new Insets(4));
        gridContainer.getStyleClass().add("calendar-grid-container");

        calendarGrid = buildGrid();
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        gridContainer.getChildren().add(calendarGrid);

        HBox legend = buildLegend();
        calendarRoot.getChildren().addAll(titleRow, gridContainer, legend);
    }

    private void showAddReminderDialog() {
        boolean en = UserSession.get().isEnglish();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(en ? "New Reminder" : "Nová pripomienka");
        stage.setResizable(false);

        VBox dialog = new VBox(14);
        dialog.setPadding(new Insets(24));
        dialog.setPrefWidth(400);
        dialog.getStyleClass().add("add-reminder-dialog");

        Label dlgTitle = new Label(en ? "New Reminder" : "Nová pripomienka");
        dlgTitle.getStyleClass().add("calendar-title");

        Label titleLbl = new Label(en ? "Title" : "Názov");
        titleLbl.getStyleClass().add("input-label");
        TextField titleField = new TextField();
        titleField.setPromptText(en ? "e.g. Study for DBS" : "napr. Štúdium na DBS");

        Label dayLbl = new Label(en ? "Day" : "Deň");
        dayLbl.getStyleClass().add("input-label");
        ComboBox<String> dayCombo = new ComboBox<>();
        String[] dayNames = en
                ? new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"}
                : new String[]{"Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok"};

        for (int i = 0; i < 5; i++) {
            dayCombo.getItems().add(dayNames[i] + " " + fullDates[i]);
        }
        dayCombo.getSelectionModel().select(0);
        dayCombo.setMaxWidth(Double.MAX_VALUE);

        Label timeLbl = new Label(en ? "Time (HH:MM)" : "Čas (HH:MM)");
        timeLbl.getStyleClass().add("input-label");
        TextField timeField = new TextField("14:00");

        Label durLbl = new Label(en ? "Duration (min)" : "Trvanie (min)");
        durLbl.getStyleClass().add("input-label");
        TextField durField = new TextField("60");

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("error-label");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        Button cancelBtn = new Button(en ? "Cancel" : "Zrušiť");
        cancelBtn.getStyleClass().addAll("button", "cancel-button");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button(en ? "Add to Calendar" : "Pridať do kalendára");
        saveBtn.getStyleClass().addAll("button", "save-button");
        saveBtn.setPrefWidth(180);

        saveBtn.setOnAction(e -> {
            String evTitle = titleField.getText().trim();
            String evTime  = timeField.getText().trim();
            if (evTitle.isEmpty()) {
                errorLbl.setText(en ? "Title is required." : "Zadajte názov.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            String displayTime;
            try {
                int dur = Integer.parseInt(durField.getText().trim());
                if (evTime.contains(":")) {
                    String[] tp = evTime.split(":");
                    int sh = Integer.parseInt(tp[0]);
                    int sm = Integer.parseInt(tp[1]);
                    int endTotal = sh * 60 + sm + dur;
                    displayTime = String.format("%02d:%02d - %02d:%02d", sh, sm, endTotal / 60, endTotal % 60);
                } else { displayTime = evTime; }
            } catch (Exception ex) { displayTime = evTime; }

            userReminders.add(new CalEvent(en ? "REMINDER" : "PRIPOMIENKA", evTitle, displayTime, dayCombo.getSelectionModel().getSelectedIndex()));
            saveReminders();
            refreshGrid();
            stage.close();
        });

        HBox btnRow = new HBox(12, cancelBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        dialog.getChildren().addAll(dlgTitle, new Separator(), titleLbl, titleField, dayLbl, dayCombo, timeLbl, timeField, durLbl, durField, errorLbl, btnRow);
        stage.setScene(new Scene(dialog));
        stage.showAndWait();
    }

    private void refreshGrid() {
        if (gridContainer == null) return;
        gridContainer.getChildren().remove(calendarGrid);
        calendarGrid = buildGrid();
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        gridContainer.getChildren().add(calendarGrid);
    }

    private GridPane buildGrid() {
         GridPane grid = new GridPane();
         grid.setHgap(2);
         grid.setVgap(0);
         grid.getStyleClass().add("calendar-grid");

         ColumnConstraints c0 = new ColumnConstraints(110);
         grid.getColumnConstraints().add(c0);

         for (int i = 0; i < 26; i++) {
             ColumnConstraints cc = new ColumnConstraints();
             cc.setHgrow(Priority.ALWAYS);
             cc.setMinWidth(38);
             grid.getColumnConstraints().add(cc);
         }

         for (int h = 8; h <= 20; h++) {
             Label hourLbl = new Label(String.format("%02d:00", h));
             hourLbl.getStyleClass().add("calendar-time-header");
             hourLbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
             hourLbl.setAlignment(Pos.CENTER);
             grid.add(hourLbl, (h - 8) * 2 + 1, 0, 2, 1);
         }

         for (int i = 0; i < 5; i++) {
             RowConstraints rc = new RowConstraints();
             rc.setVgrow(Priority.ALWAYS);
             rc.setMinHeight(140);
             grid.getRowConstraints().add(rc);
         }

         boolean en = UserSession.get().isEnglish();
         String[] days = en ? DAYS_EN : DAYS_SK;
         List<CalEvent> allEvents = new ArrayList<>(backendEvents);
         allEvents.addAll(userReminders);

         for (int d = 0; d < 5; d++) {
             grid.add(buildDayRowHeader(d, days), 0, d + 1);
             for (CalEvent ev : allEvents) {
                 if (ev.dayIndex() == d) {
                     addEventToGrid(grid, ev, d + 1);
                 }
             }
         }
         return grid;
     }

    private VBox buildDayRowHeader(int d, String[] days) {
        boolean today = (d == todayIdx);
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.getStyleClass().add(today ? "calendar-day-header-box-today" : "calendar-day-header-box");

        Label dayLbl = new Label(days[d]);
        dayLbl.getStyleClass().add(today ? "calendar-day-name-today" : "calendar-day-name");
        dayLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label numLbl = new Label(fullDates[d]);
        numLbl.getStyleClass().add(today ? "calendar-day-num-today" : "calendar-day-num");
        numLbl.setStyle("-fx-font-size: 12px;");

        box.getChildren().addAll(dayLbl, numLbl);
        return box;
    }

    private void addEventToGrid(GridPane grid, CalEvent ev, int row) {
        try {
            String[] parts = ev.time().split("-");
            double startMin = parseToMinutes(parts[0].trim());
            double endMin = parts.length > 1 ? parseToMinutes(parts[parts.length-1].trim()) : startMin + 90;

            double startOffset = Math.max(0, startMin - 480);
            double duration = Math.max(30, endMin - startMin);

            int startCol = (int) Math.round(startOffset / 30) + 1;
            int colSpan = Math.max(1, (int) Math.round(duration / 30));

            if (startCol >= 1 && startCol <= 26) {
                VBox card = buildEventCard(ev);
                grid.add(card, startCol, row, Math.min(colSpan, 27 - startCol), 1);
                GridPane.setMargin(card, new Insets(2));
            }
        } catch (Exception e) { }
    }

    private double parseToMinutes(String timeStr) {
        try {
            String clean = timeStr.trim().toUpperCase().replaceAll("\\s", "");
            boolean pm = clean.endsWith("PM");
            boolean am = clean.endsWith("AM");
            clean = clean.replace("PM", "").replace("AM", "");
            String[] parts = clean.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (pm && h < 12) h += 12;
            if (am && h == 12) h = 0;
            return h * 60 + m;
        } catch (Exception e) { return 480; }
    }

    private VBox buildEventCard(CalEvent ev) {
         VBox card = new VBox(3);
         card.setPadding(new Insets(8));
         card.getStyleClass().add("event-card");

         String typeClass = switch (ev.type()) {
             case "PREDNÁŠKA" -> "event-card-prednaska";
             case "CVIČENIE" -> "event-card-cvicenie";
             case "ODOVZDANIE", "TASK DUE" -> "event-card-odovzdanie";
             default -> "event-card-reminder";
         };
         card.getStyleClass().add(typeClass);

         Label typeLbl = new Label(ev.type());
         typeLbl.getStyleClass().add("event-card-type");
         Label titleLbl = new Label(ev.title());
         titleLbl.getStyleClass().add("event-card-title");
         titleLbl.setWrapText(true);
         Label timeLbl = new Label(ev.time());
         timeLbl.getStyleClass().add("event-card-time");

         card.getChildren().addAll(typeLbl, titleLbl, timeLbl);

         // Pridaj context menu na vymazanie pre pripomienky
         boolean isReminder = "PRIPOMIENKA".equals(ev.type()) || "REMINDER".equals(ev.type());
         if (isReminder) {
             ContextMenu contextMenu = new ContextMenu();
             MenuItem deleteItem = new MenuItem(UserSession.get().isEnglish() ? "Delete" : "Vymazať");
             deleteItem.setOnAction(action -> {
                 userReminders.removeIf(e -> e.equals(ev));
                 saveReminders();
                 refreshGrid();
             });
             contextMenu.getItems().add(deleteItem);
             card.setOnContextMenuRequested(event -> {
                 contextMenu.show(card, event.getScreenX(), event.getScreenY());
             });
             card.setStyle(card.getStyle() + ";-fx-cursor:context-menu;");
         }

         return card;
     }

    private HBox buildLegend() {
        boolean en = UserSession.get().isEnglish();
        HBox legend = new HBox(24);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(12, 0, 0, 0));
        legend.getChildren().addAll(
                legendItem("#2563EB", en ? "Lectures" : "Prednášky"),
                legendItem("#16A34A", en ? "Labs" : "Cvičenia"),
                legendItem("#DC2626", en ? "Assignments" : "Odovzdania"),
                legendItem("#7c3aed", en ? "Reminders" : "Pripomienky")
        );
        return legend;
    }

    private HBox legendItem(String color, String label) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        Circle circle = new Circle(6, Color.web(color));
        Label lbl = new Label(label);
        lbl.getStyleClass().add("calendar-legend-label");
        item.getChildren().addAll(circle, lbl);
        return item;
    }

    private void loadReminders() {
        String userId = UserSession.get().getUserEmail();
        List<RemindersStorage.CalendarReminder> loaded = RemindersStorage.loadReminders(userId);
        for (RemindersStorage.CalendarReminder r : loaded) {
            userReminders.add(new CalEvent(r.type, r.title, r.time, r.dayIndex));
        }
    }

    private void saveReminders() {
        String userId = UserSession.get().getUserEmail();
        List<RemindersStorage.CalendarReminder> toSave = new ArrayList<>();
        for (CalEvent ev : userReminders) {
            toSave.add(new RemindersStorage.CalendarReminder(ev.type, ev.title, ev.time, ev.dayIndex));
        }
        RemindersStorage.saveReminders(toSave, userId);
    }
}

