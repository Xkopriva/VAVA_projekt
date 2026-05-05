package com.example.bais.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.bais.components.NotificationWindow;
import com.example.bais.models.UserSession;
import com.example.bais.services.WebSocketClientService;
import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DashboardController implements Initializable {

    // Header
    @FXML
    private Label headerTitle;
    @FXML
    private Label headerSubtitle;
    @FXML
    private TextField searchField;
    @FXML
    private Button darkModeToggle;
    @FXML
    private StackPane notificationsAction;
    @FXML
    private Label calendarAction;

    // Nav labels
    @FXML
    private Label navDashboard;
    @FXML
    private Label navGrades;
    @FXML
    private Label navCalendar;
    @FXML
    private Label navProgress;
    @FXML
    private Label navAlgebra;
    @FXML
    private Label navCourses;
    @FXML
    private Label navAdminSubjects;
    @FXML
    private Label navAdminGuarantors;
    @FXML
    private Label navAdminCreateTeacher;
    @FXML
    private Label navSettings;
    @FXML
    private Label navLogout;

    // Dashboard content labels
    @FXML
    private Label welcomeTitle;
    @FXML
    private Label welcomeSub;
    @FXML
    private Label schedTitle;
    @FXML
    private Label alertsTitle;
    @FXML
    private Label perfTitle;
    @FXML
    private Label degreeTitle;
    @FXML
    private Label quickTitle;

    // Added for dynamic translation
    @FXML
    private Label tileCalendarText;
    @FXML
    private Label tileGradesText;
    @FXML
    private Label tileAssignmentsText;
    @FXML
    private Label viewAllPerfText;
    @FXML
    private Button viewPlanBtn;
    @FXML
    private Label gameTitleText;

    // Admin tile labels
    @FXML
    private HBox adminTilesRow;
    @FXML
    private HBox studentTilesRow;
    @FXML
    private Label tileAdminSubjectsText;
    @FXML
    private Label tileAdminGuarantorsText;
    @FXML
    private Label tileAdminCreateTeacherText;
    @FXML
    private Label tileAdminCalendarText;

    // Nav items
    @FXML
    private VBox dashboardItem;
    @FXML
    private VBox gradesItem;
    @FXML
    private VBox calendarItem;
    @FXML
    private VBox progressItem;
    @FXML
    private VBox algebraItem;
    @FXML
    private VBox coursesItem;
    @FXML
    private VBox adminSubjectsItem;
    @FXML
    private VBox adminGuarantorsItem;
    @FXML
    private VBox adminCreateTeacherItem;
    @FXML
    private VBox settingsItem;
    @FXML
    private VBox logoutItem;

    @FXML
    private ScrollPane mainScroll;

    @FXML
    private VBox dashboardMiniCalendar;
    @FXML
    private VBox dashboardMiniTasks;
    @FXML
    private VBox dashboardAlertsContainer;
    @FXML
    private javafx.scene.layout.HBox dashboardPerfContainer;
    @FXML
    private Label dashboardDateLabel;
    @FXML
    private Label dashboardProgressLabel;
    @FXML
    private Label dashboardProgressPct;

    // Minigame
    @FXML
    private javafx.scene.layout.Pane gameArea;
    @FXML
    private Button gameTarget;
    @FXML
    private Label gameScoreLabel;
    private int gameScore = 0;

    private Node dashboardContent;
    private boolean isDarkMode = false;
    private boolean isEnglish = false;

    // Subscription IDs for dashboard data
    private String subProfile;
    private String subDashEnrollments;
    private String subUnreadNotifs;
    private String subDashCalendar;
    private String subDashMarks;

    private void loadSettings() {
        try {
            java.io.File file = new java.io.File(System.getProperty("user.home"), ".bais-settings.json");
            if (file.exists()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(file);
                this.isDarkMode = root.path("isDarkMode").asBoolean(false);
            }
        } catch (Exception e) {
        }
    }

    private void saveSettingsToJson() {
        try {
            java.io.File file = new java.io.File(System.getProperty("user.home"), ".bais-settings.json");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode root;
            if (file.exists()) {
                root = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(file);
            } else {
                root = mapper.createObjectNode();
            }
            root.put("isEnglish", isEnglish);
            root.put("isDarkMode", isDarkMode);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSettings();
        isEnglish = UserSession.get().isEnglish();

        updateLanguage();
        updateDarkModeButton();
        setActiveNavItem("dashboard");

        if (UserSession.get().isTeacher() || UserSession.get().isAdmin()) {
            applyTeacherUI();
        }

        if (searchField != null) {
            searchField.setOnKeyReleased(event -> {
                String query = searchField.getText().toLowerCase().trim();
                if (event.getCode().toString().equals("ENTER") && !query.isEmpty()) {
                    handleSearch(query);
                }
            });
        }

        Platform.runLater(() -> {
            if (mainScroll != null)
                dashboardContent = mainScroll.getContent();
        });

        // Spojenie musí byť plne pripravené predtým než pýtame dáta
        WebSocketClientService.getInstance().connectAsync().thenRun(() -> {
            // Load user profile if needed
            if (UserSession.get().getFullName().equals(UserSession.get().getUserEmail())
                    || UserSession.get().getFirstName().isEmpty()) {
                loadUserProfile();
            }

            if (!UserSession.get().isAdmin()) {
                loadDashboardData();
                loadUnreadNotifications();
            }
        });

        // Initialize Minigame
        if (gameTarget != null && gameArea != null && gameScoreLabel != null) {
            gameTarget.setOnAction(e -> {
                gameScore++;
                gameScoreLabel.setText(String.valueOf(gameScore));

                double maxX = Math.max(20, gameArea.getWidth() - 35);
                double maxY = Math.max(20, gameArea.getHeight() - 35);
                double x = new java.util.Random().nextDouble() * maxX;
                double y = new java.util.Random().nextDouble() * maxY;
                gameTarget.setLayoutX(x);
                gameTarget.setLayoutY(y);
            });

            Platform.runLater(() -> {
                double maxX = Math.max(20, gameArea.getWidth() - 35);
                double maxY = Math.max(20, gameArea.getHeight() - 35);
                gameTarget.setLayoutX(maxX / 2);
                gameTarget.setLayoutY(maxY / 2);
            });
        }
    }

    /** Načíta meno používateľa z backendu a aktualizuje welcome banner. */
    private void loadUserProfile() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        subProfile = ws.subscribe("USER_PROFILE", this::handleProfileMessage);
        ws.sendAction("GET_USER_PROFILE", null);
    }

    private void handleProfileMessage(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subProfile);
        JsonNode data = node.path("data");
        String firstName = data.path("firstName").asText("");
        String lastName = data.path("lastName").asText("");
        UserSession.get().setFirstName(firstName);
        UserSession.get().setLastName(lastName);
        Platform.runLater(this::updateWelcomeBanner);
    }

    /** Načíta reálne hodnotenia a enrollmenty pre dashboard. */
    private void loadDashboardData() {
        WebSocketClientService ws = WebSocketClientService.getInstance();

        // Načítaj enrollmenty (počet predmetov → welcome subtitle)
        subDashEnrollments = ws.subscribe("MY_ENROLLMENTS", this::handleDashboardEnrollments);
        ws.sendAction("GET_MY_ENROLLMENTS", null);

        // Načítaj kalendár
        subDashCalendar = ws.subscribe("MY_CALENDAR_EVENTS", this::handleDashboardCalendar);
        ws.sendAction("GET_MY_CALENDAR", null);

        // Načítaj hodnotenia
        subDashMarks = ws.subscribe("MY_INDEX_RECORDS", this::handleDashboardMarks);
        ws.sendAction("GET_MY_MARKS", null);

    }

    private void loadUnreadNotifications() {
        WebSocketClientService ws = WebSocketClientService.getInstance();
        subUnreadNotifs = ws.subscribe("UNREAD_NOTIFICATIONS_LIST", this::handleUnreadNotifs);
        ws.sendAction("GET_UNDREAD_NOTIFICATIONS", null);

        ws.subscribe("ALL_NOTIFICATIONS_MARKED_READ", node -> {
            Platform.runLater(() -> {
                if (notificationsAction != null) {
                    notificationsAction.getChildren().stream()
                            .filter(n -> n instanceof javafx.scene.control.Label lbl
                                    && lbl.getStyleClass().contains("notification-badge"))
                            .findFirst()
                            .ifPresent(n -> n.setVisible(false));
                }
            });
        });
    }

    private void handleUnreadNotifs(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subUnreadNotifs);
        JsonNode data = node.path("data");
        int count = data.isArray() ? data.size() : 0;
        Platform.runLater(() -> {
            if (notificationsAction != null) {
                notificationsAction.getChildren().stream()
                        .filter(n -> n instanceof javafx.scene.control.Label lbl
                                && lbl.getStyleClass().contains("notification-badge"))
                        .findFirst()
                        .ifPresent(n -> {
                            javafx.scene.control.Label badge = (javafx.scene.control.Label) n;
                            if (count > 0) {
                                badge.setText(String.valueOf(Math.min(count, 9)));
                                badge.setVisible(true);
                            } else {
                                badge.setVisible(false);
                            }
                        });
            }

            if (dashboardAlertsContainer != null) {
                dashboardAlertsContainer.getChildren().clear();
                if (count == 0) {
                    Label l = new Label(isEnglish ? "No new alerts." : "Žiadne nové upozornenia.");
                    l.setStyle("-fx-text-fill: #94a3b8;");
                    dashboardAlertsContainer.getChildren().add(l);
                } else {
                    int i = 0;
                    for (JsonNode n : data) {
                        if (i++ >= 3)
                            break; // Zobrazi len 3 notifikacie
                        VBox alertBox = new VBox(4);
                        alertBox.getStyleClass().addAll("alert-item", "alert-blue");
                        Label t = new Label("🔔 " + n.path("title").asText("Upozornenie"));
                        t.getStyleClass().add("alert-title");
                        Label b = new Label(n.path("message").asText(""));
                        b.getStyleClass().add("alert-body");
                        b.setWrapText(true);
                        alertBox.getChildren().addAll(t, b);
                        dashboardAlertsContainer.getChildren().add(alertBox);
                    }
                }
            }
        });
    }

    private void handleDashboardEnrollments(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subDashEnrollments);
        JsonNode data = node.path("data");
        int active = 0;
        int credits = 0;
        if (data.isArray()) {
            for (JsonNode e : data) {
                if ("ACTIVE".equals(e.path("status").asText()))
                    active++;
                if ("PASSED".equals(e.path("status").asText()))
                    credits += e.path("credits").asInt(0);
            }
        }
        final int activeCount = active;
        final int passedCredits = credits;
        Platform.runLater(() -> {
            if (welcomeSub != null) {
                boolean en = isEnglish;
                welcomeSub.setText(activeCount > 0
                        ? (en ? "You have " + activeCount + " active subjects this semester."
                                : "Máš " + activeCount + " aktívnych predmetov tento semester.")
                        : (en ? "You have upcoming deadlines this week."
                                : "Máš nadchádzajúce termíny tento týždeň."));
            }
            if (dashboardProgressLabel != null) {
                dashboardProgressLabel.setText(isEnglish
                        ? "Completed " + passedCredits + " out of 180 required credits."
                        : "Dokončil si " + passedCredits + " z 180 požadovaných kreditov pre Bc. Informatiku.");
                if (dashboardProgressPct != null) {
                    dashboardProgressPct.setText(Math.round((passedCredits / 180.0) * 100) + "%");
                }
            }
        });
    }

    private void handleDashboardCalendar(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subDashCalendar);
        JsonNode data = node.path("data");
        Platform.runLater(() -> {
            if (dashboardMiniCalendar != null)
                dashboardMiniCalendar.getChildren().clear();
            if (dashboardMiniTasks != null)
                dashboardMiniTasks.getChildren().clear();

            int calCount = 0;
            int taskCount = 0;

            if (data.isArray()) {
                // Sort by time
                java.util.List<JsonNode> sorted = new java.util.ArrayList<>();
                data.forEach(sorted::add);
                sorted.sort((a, b) -> a.path("scheduledAt").asText("").compareTo(b.path("scheduledAt").asText("")));

                for (JsonNode ev : sorted) {
                    String type = ev.path("type").asText("EVENT");
                    String title = ev.path("title").asText("Neznáma");
                    String time = ev.path("scheduledAt").asText("");
                    if (time.isEmpty())
                        continue;

                    try {
                        java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(time);
                        // Len buduce
                        if (odt.isBefore(java.time.OffsetDateTime.now()))
                            continue;

                        String dateStr = odt.format(java.time.format.DateTimeFormatter.ofPattern("d.M. HH:mm"));

                        if ((type.equals("PREDNASKA") || type.equals("CVICENIE")) && calCount < 2) {
                            Label l = new Label(title + " (" + dateStr + ")");
                            l.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 13px; -fx-font-weight: bold;");
                            l.setWrapText(true);
                            if (dashboardMiniCalendar != null)
                                dashboardMiniCalendar.getChildren().add(l);
                            calCount++;
                        }
                        if ((type.equals("ODOVZDANIE") || type.equals("TASK_DUE") || type.equals("PISOMKA")
                                || type.equals("EXAM")) && taskCount < 2) {
                            Label l = new Label(title + " (" + dateStr + ")");
                            l.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 13px; -fx-font-weight: bold;");
                            l.setWrapText(true);
                            if (dashboardMiniTasks != null)
                                dashboardMiniTasks.getChildren().add(l);
                            taskCount++;
                        }
                    } catch (Exception e) {
                    }
                }
            }

            if (calCount == 0 && dashboardMiniCalendar != null) {
                Label l = new Label("Voľno");
                l.setStyle("-fx-text-fill: #94a3b8;");
                dashboardMiniCalendar.getChildren().add(l);
            }
            if (taskCount == 0 && dashboardMiniTasks != null) {
                Label l = new Label("Žiadne zadania");
                l.setStyle("-fx-text-fill: #94a3b8;");
                dashboardMiniTasks.getChildren().add(l);
            }
        });
    }

    private void handleDashboardMarks(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subDashMarks);
        JsonNode data = node.path("data");
        Platform.runLater(() -> {
            if (dashboardPerfContainer != null) {
                dashboardPerfContainer.getChildren().clear();
                if (data.isArray() && data.size() > 0) {
                    int i = 0;
                    for (JsonNode m : data) {
                        if (i++ >= 4)
                            break;
                        VBox pCard = new VBox(4);
                        pCard.getStyleClass().add("perf-card");
                        pCard.setMinWidth(220); // Make grades wider so text fits
                        pCard.setMaxWidth(Double.MAX_VALUE);
                        javafx.scene.layout.HBox.setHgrow(pCard, javafx.scene.layout.Priority.ALWAYS);

                        Label crs = new Label("Predmet " + m.path("enrollmentId").asText());
                        crs.getStyleClass().add("perf-course");
                        Label gr = new Label(m.path("finalMark").asText());
                        gr.getStyleClass().add("perf-grade");
                        Label itm = new Label(isEnglish ? "Final Grade" : "Záverečná známka");
                        itm.getStyleClass().add("perf-item");
                        javafx.scene.layout.Region bar = new javafx.scene.layout.Region();
                        bar.getStyleClass().addAll("perf-bar", "perf-bar-hi");

                        pCard.getChildren().addAll(crs, gr, itm, bar);
                        dashboardPerfContainer.getChildren().add(pCard);
                    }
                } else {
                    Label l = new Label(isEnglish ? "No recent grades." : "Žiadne nedávne hodnotenia.");
                    l.setStyle("-fx-text-fill: #94a3b8;");
                    dashboardPerfContainer.getChildren().add(l);
                }
            }
        });
    }

    /** Aktualizuje welcome banner s menom používateľa. */
    private void updateWelcomeBanner() {
        boolean en = isEnglish;
        boolean teacher = UserSession.get().isTeacher();
        boolean admin = UserSession.get().isAdmin();
        String name = UserSession.get().getFullName();

        if (welcomeTitle == null)
            return;

        if (admin) {
            welcomeTitle.setText(en ? "Welcome, Administrator 👋" : "Vitajte, Administrátor 👋");
        } else if (teacher) {
            welcomeTitle.setText((en ? "Welcome, " : "Vitajte, ") + name + " 👋");
        } else {
            welcomeTitle.setText((en ? "Welcome back, " : "Vitaj späť, ") + name + " 👋");
        }
    }

    // ── NAV HANDLERS ──────────────────────────────────────────────

    @FXML
    private void handleDashboard() {
        setActiveNavItem("dashboard");
        if (mainScroll != null && dashboardContent != null)
            mainScroll.setContent(dashboardContent);
    }

    @FXML
    private void handleGrades() {
        setActiveNavItem("grades");
        if (UserSession.get().isAdmin()) {
            loadView("admin-panel-view.fxml");
        } else if (UserSession.get().isTeacher()) {
            loadView("teacher-grades-view.fxml");
        } else {
            loadView("grades-view.fxml");
        }
    }

    @FXML
    private void handleCalendar() {
        setActiveNavItem("calendar");
        loadView("school-calendar-view.fxml");
    }

    @FXML
    private void handleProgress() {
        setActiveNavItem("progress");
        loadView("progress-view.fxml");
    }

    @FXML
    private void handleAlgebra() {
        if (UserSession.get().isTeacher()) {
            handleGrades();
            return;
        }
        setActiveNavItem("algebra");
        loadView("submissions-view.fxml");
    }

    @FXML
    private void handleCourses() {
        setActiveNavItem("courses");
        loadView("courses-view.fxml");
    }

    @FXML
    private void handleAdminSubjects() {
        setActiveNavItem("adminSubjects");
        loadView("admin-subjects-view.fxml");
    }

    @FXML
    private void handleAdminGuarantors() {
        setActiveNavItem("adminGuarantors");
        loadView("admin-guarantors-view.fxml");
    }

    @FXML
    private void handleAdminCreateTeacher() {
        setActiveNavItem("adminCreateTeacher");
        loadView("admin-create-teacher-view.fxml");
    }

    @FXML
    private void handleSettings() {
        setActiveNavItem("settings");
        loadView("settings-view.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.get().setRole(UserSession.Role.STUDENT);
            UserSession.get().setFirstName("");
            UserSession.get().setLastName("");
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/example/bais/login-view.fxml"));
            loader.setCharset(java.nio.charset.StandardCharsets.UTF_8);
            Scene loginScene = new Scene(loader.load(), 1280, 800);
            loginScene.getStylesheets().add(
                    getClass().getResource(isDarkMode ? "/dark.css" : "/light.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) darkModeToggle.getScene().getWindow();
            stage.setTitle("BAIS – Lepší Akademický Systém");
            stage.setScene(loginScene);
            if (!stage.isMaximized())
                stage.setMaximized(true);

            // Workaround pre fullscreen problém po odhlásení
            Platform.runLater(() -> {
                stage.setMaximized(false); // Zmenší okno na normálny stav
                stage.setMaximized(true); // Okamžite ho znova maximalizuje
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Helper: načítaj FXML do scroll pane ──────────────────────

    private SettingsController currentSettingsController;

    private void loadView(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/bais/" + fxmlName));
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof SettingsController sc) {
                currentSettingsController = sc;
                sc.setDashboardController(this);
            } else {
                currentSettingsController = null;
            }

            if (mainScroll != null)
                mainScroll.setContent(view);
        } catch (IOException e) {
            System.err.println("Chyba pri načítaní: " + fxmlName + " – " + e.getMessage());
            e.printStackTrace();
            showPlaceholder(fxmlName + " – načítanie zlyhalo");
        }
    }

    private void showPlaceholder(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("content-placeholder");
        StackPane sp = new StackPane(lbl);
        sp.getStyleClass().add("main-content");
        if (mainScroll != null)
            mainScroll.setContent(sp);
    }

    // ── Aktívna nav položka ───────────────────────────────────────

    private void setActiveNavItem(String active) {
        VBox[] all = { dashboardItem, gradesItem, calendarItem, progressItem,
                algebraItem, coursesItem, adminSubjectsItem, adminGuarantorsItem,
                adminCreateTeacherItem, settingsItem };
        for (VBox v : all) {
            if (v != null)
                v.getStyleClass().removeAll("nav-item-active");
        }
        VBox target = switch (active) {
            case "dashboard"          -> dashboardItem;
            case "grades"             -> gradesItem;
            case "calendar"           -> calendarItem;
            case "progress"           -> progressItem;
            case "algebra"            -> algebraItem;
            case "courses"            -> coursesItem;
            case "adminSubjects"      -> adminSubjectsItem;
            case "adminGuarantors"    -> adminGuarantorsItem;
            case "adminCreateTeacher" -> adminCreateTeacherItem;
            case "settings"           -> settingsItem;
            default -> null;
        };
        if (target != null)
            target.getStyleClass().add("nav-item-active");
    }

    // ── Téma a jazyk ──────────────────────────────────────────────

    public void toggleLanguage() {
        isEnglish = !isEnglish;
        UserSession.get().setEnglish(isEnglish);
        updateLanguage();
        if (UserSession.get().isTeacher() || UserSession.get().isAdmin())
            applyTeacherUI();
        updateWelcomeBanner();

        // Reload active view
        if (dashboardItem.getStyleClass().contains("nav-item-active"))
            handleDashboard();
        else if (gradesItem.getStyleClass().contains("nav-item-active"))
            handleGrades();
        else if (calendarItem.getStyleClass().contains("nav-item-active"))
            handleCalendar();
        else if (progressItem != null && progressItem.getStyleClass().contains("nav-item-active"))
            handleProgress();
        else if (algebraItem != null && algebraItem.getStyleClass().contains("nav-item-active"))
            handleAlgebra();
        else if (coursesItem != null && coursesItem.getStyleClass().contains("nav-item-active"))
            handleCourses();
        else if (adminSubjectsItem != null && adminSubjectsItem.getStyleClass().contains("nav-item-active"))
            handleAdminSubjects();
        else if (adminGuarantorsItem != null && adminGuarantorsItem.getStyleClass().contains("nav-item-active"))
            handleAdminGuarantors();
        else if (adminCreateTeacherItem != null && adminCreateTeacherItem.getStyleClass().contains("nav-item-active"))
            handleAdminCreateTeacher();
        else if (settingsItem.getStyleClass().contains("nav-item-active"))
            handleSettings();

        saveSettingsToJson();
        if (currentSettingsController != null)
            currentSettingsController.syncFromDashboard();
    }

    public void toggleDarkMode() {
        setDarkMode(!isDarkMode);
    }

    public void setDarkMode(boolean enableDark) {
        isDarkMode = enableDark;
        UserSession.get().setDarkMode(enableDark);
        Scene scene = darkModeToggle.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                    getClass().getResource(isDarkMode ? "/dark.css" : "/light.css").toExternalForm());
        }
        updateDarkModeButton();
        saveSettingsToJson();
        if (currentSettingsController != null)
            currentSettingsController.syncFromDashboard();
    }

    private void applyTeacherUI() {
        boolean en = isEnglish;
        UserSession.Role role = UserSession.get().getRole();

        if (headerSubtitle != null) {
            headerSubtitle.setText(role == UserSession.Role.ADMIN
                    ? (en ? "Admin Portal" : "Portál administrátora")
                    : (en ? "Teacher Portal" : "Portál učiteľa"));
        }

        // Hide student-only nav items
        if (progressItem != null) { progressItem.setVisible(false); progressItem.setManaged(false); }
        if (algebraItem  != null) { algebraItem.setVisible(false);  algebraItem.setManaged(false);  }
        if (coursesItem  != null) { coursesItem.setVisible(false);  coursesItem.setManaged(false);  }

        if (navGrades != null) {
            if (role == UserSession.Role.ADMIN) {
                navGrades.setText("Admin Panel");
            } else {
                navGrades.setText("Teacher Panel");
            }
        }

        if (role == UserSession.Role.ADMIN) {
            // Show admin nav items
            if (adminSubjectsItem != null)      { adminSubjectsItem.setVisible(true);      adminSubjectsItem.setManaged(true);      }
            if (adminGuarantorsItem != null)     { adminGuarantorsItem.setVisible(true);     adminGuarantorsItem.setManaged(true);     }
            if (adminCreateTeacherItem != null)  { adminCreateTeacherItem.setVisible(true);  adminCreateTeacherItem.setManaged(true);  }

            // Swap tile rows
            if (studentTilesRow != null) { studentTilesRow.setVisible(false); studentTilesRow.setManaged(false); }
            if (adminTilesRow   != null) { adminTilesRow.setVisible(true);    adminTilesRow.setManaged(true);    }

            if (welcomeSub != null)
                welcomeSub.setText(en
                    ? "System is running normally. You have full access."
                    : "Systém beží normálne. Máte plný prístup.");
        } else {
            if (welcomeSub != null)
                welcomeSub.setText(en
                    ? "Click 'Student Grades' to view results for your subjects."
                    : "Kliknite na 'Známky študentov' pre prehľad výsledkov vašich predmetov.");
        }

        if (perfTitle != null)   { hideParentCard(perfTitle); }
        if (degreeTitle != null) { hideParentCard(degreeTitle); }
    }

    private void hideParentCard(javafx.scene.Node node) {
        javafx.scene.Node n = node;
        while (n != null && !(n instanceof VBox vb && vb.getStyleClass().contains("section-card"))) {
            n = n.getParent();
        }
        if (n != null) {
            n.setVisible(false);
            n.setManaged(false);
        }
    }

    private void updateLanguage() {
        boolean en = isEnglish;
        boolean teacher = UserSession.get().isTeacher();

        if (headerTitle != null)
            headerTitle.setText(en ? "Academic Info" : "Akademické Info");
        if (headerSubtitle != null)
            headerSubtitle.setText(
                    teacher ? (en ? "Teacher Portal" : "Portál učiteľa")
                            : (en ? "Student Portal" : "Študentský Portál"));
        if (searchField != null)
            searchField.setPromptText(en ? "Search AIS Desktop..." : "Hľadať AIS Desktop...");

        if (navDashboard != null)
            navDashboard.setText(en ? "Dashboard" : "Prehľad");
        if (navGrades != null)
            navGrades.setText(
                    UserSession.get().isAdmin() ? (en ? "Admin Panel" : "Admin Panel")
                            : teacher ? (en ? "Student Grades" : "Známky študentov")
                                    : (en ? "Grades" : "Hodnotenia"));
        if (navCalendar != null)
            navCalendar.setText(en ? "Calendar" : "Kalendár");
        if (navProgress != null)
            navProgress.setText(en ? "Progress" : "Progres");
        if (navAlgebra != null)
            navAlgebra.setText(en ? "Assignments" : "Odovzdanie");
        if (navCourses != null)
            navCourses.setText(en ? "Courses" : "Kurzy");
        if (navAdminSubjects != null)
            navAdminSubjects.setText(en ? "Subjects" : "Predmety");
        if (navAdminGuarantors != null)
            navAdminGuarantors.setText(en ? "Guarantors" : "Garanti");
        if (navAdminCreateTeacher != null)
            navAdminCreateTeacher.setText(en ? "Create Teacher" : "Vytvoriť učiteľa");
        if (navSettings != null)
            navSettings.setText(en ? "Settings" : "Nastavenia");
        if (navLogout != null)
            navLogout.setText(en ? "Logout" : "Odhlásiť");

        // Welcome banner — meno sa dopĺňa z updateWelcomeBanner() po načítaní profilu
        String name = UserSession.get().getFullName();
        if (welcomeTitle != null) {
            boolean admin = UserSession.get().isAdmin();
            if (admin) {
                welcomeTitle.setText(en ? "Welcome, Administrator 👋" : "Vitajte, Administrátor 👋");
            } else {
                welcomeTitle.setText((en ? (teacher ? "Welcome, " : "Welcome back, ")
                        : (teacher ? "Vitajte, " : "Vitaj späť, ")) + name + " 👋");
            }
        }
        if (welcomeSub != null && !UserSession.get().isTeacher() && !UserSession.get().isAdmin()) {
            welcomeSub.setText(en
                    ? "You have upcoming deadlines this week."
                    : "Máš nadchádzajúce termíny tento týždeň.");
        }
        if (schedTitle != null)
            schedTitle.setText(en ? "Today's Schedule" : "Rozvrh dnes");
        if (alertsTitle != null)
            alertsTitle.setText(en ? "Alerts" : "Upozornenia");
        if (perfTitle != null)
            perfTitle.setText(en ? "Recent Performance" : "Výsledky predmetov");
        if (degreeTitle != null)
            degreeTitle.setText(en ? "Degree Progress" : "Progres štúdium");
        if (quickTitle != null)
            quickTitle.setText(en ? "Quick Links" : "Rýchle odkazy");

        if (tileCalendarText != null)
            tileCalendarText.setText(en ? "Calendar" : "Kalendár");
        if (tileGradesText != null)
            tileGradesText.setText(en ? "Grades" : "Známky");
        if (tileAssignmentsText != null) {
            if (UserSession.get().isTeacher()) {
                tileAssignmentsText.setText(en ? "Teacher Panel" : "Panel učiteľa");
            } else {
                tileAssignmentsText.setText(en ? "Assignments" : "Odovzdania");
            }
        }
        if (tileAdminSubjectsText != null)
            tileAdminSubjectsText.setText(en ? "Subjects" : "Predmety");
        if (tileAdminGuarantorsText != null)
            tileAdminGuarantorsText.setText(en ? "Guarantors" : "Garanti");
        if (tileAdminCreateTeacherText != null)
            tileAdminCreateTeacherText.setText(en ? "Create Teacher" : "Vytvoriť učiteľa");
        if (tileAdminCalendarText != null)
            tileAdminCalendarText.setText(en ? "Calendar" : "Kalendár");
        if (viewAllPerfText != null)
            viewAllPerfText.setText(en ? "View all →" : "Zobraziť všetky →");
        if (viewPlanBtn != null)
            viewPlanBtn.setText(en ? "View Plan" : "Zobraziť plán");
        if (gameTitleText != null)
            gameTitleText.setText(en ? "Unwind" : "Odreagovanie");

        if (dashboardProgressLabel != null && !UserSession.get().isTeacher() && !UserSession.get().isAdmin()) {
            String txt = dashboardProgressLabel.getText();
            try {
                int creds = Integer.parseInt(txt.replaceAll("[^0-9]", " ").trim().split("\\s+")[0]);
                dashboardProgressLabel.setText(en
                        ? "You have completed " + creds + " of 180 required credits for BSc. Informatics."
                        : "Dokončil si " + creds + " z 180 požadovaných kreditov pre Bc. Informatiku.");
            } catch (Exception e) {
            }
        }
    }

    private void updateDarkModeButton() {
        if (darkModeToggle != null)
            darkModeToggle.setText(isDarkMode ? "☀" : "🌙");
    }

    @FXML
    private void handleNotifications() {
        NotificationWindow.show();
    }

    private void handleSearch(String query) {
        // Placeholder for search functionality
        System.out.println("Searching for: " + query);
        // For now, show a placeholder message
        showPlaceholder("Search for '" + query + "' - feature in development");
    }
}
