package com.example.bais;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // Header
    @FXML private Label     headerTitle;
    @FXML private Label     headerSubtitle;
    @FXML private TextField searchField;
    @FXML private Button    darkModeToggle;
    @FXML private StackPane notificationsAction;
    @FXML private Label     calendarAction;

    // Nav labels
    @FXML private Label navDashboard;
    @FXML private Label navGrades;
    @FXML private Label navCalendar;
    @FXML private Label navProgress;
    @FXML private Label navAlgebra;
    @FXML private Label navCourses;
    @FXML private Label navSettings;
    @FXML private Label navLogout;

    // Dashboard content labels
    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSub;
    @FXML private Label schedTitle;
    @FXML private Label alertsTitle;
    @FXML private Label perfTitle;
    @FXML private Label degreeTitle;
    @FXML private Label quickTitle;

    // Nav items
    @FXML private VBox dashboardItem;
    @FXML private VBox gradesItem;
    @FXML private VBox calendarItem;
    @FXML private VBox progressItem;
    @FXML private VBox algebraItem;
    @FXML private VBox coursesItem;
    @FXML private VBox settingsItem;
    @FXML private VBox logoutItem;

    @FXML private ScrollPane mainScroll;

    private Node    dashboardContent;
    private boolean isDarkMode = false;
    private boolean isEnglish  = false;

    // Subscription IDs for dashboard data
    private String subProfile;
    private String subDashMarks;
    private String subDashEnrollments;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
            if (mainScroll != null) dashboardContent = mainScroll.getContent();
        });

        // Load user profile if needed
        if (UserSession.get().getFullName().equals(UserSession.get().getUserEmail())
                || UserSession.get().getFirstName().isEmpty()) {
            loadUserProfile();
        }

        // Load real grades and enrollments for dashboard
        if (!UserSession.get().isAdmin()) {
            loadDashboardData();
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
        JsonNode data    = node.path("data");
        String firstName = data.path("firstName").asText("");
        String lastName  = data.path("lastName").asText("");
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

        // Načítaj záverečné hodnotenia → perf cards
        subDashMarks = ws.subscribe("MY_INDEX_RECORDS", this::handleDashboardMarks);
        ws.sendAction("GET_MY_MARKS", null);
    }

    private void handleDashboardEnrollments(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subDashEnrollments);
        JsonNode data = node.path("data");
        int active = 0;
        if (data.isArray()) {
            for (JsonNode e : data) {
                if ("ACTIVE".equals(e.path("status").asText())) active++;
            }
        }
        final int activeCount = active;
        Platform.runLater(() -> {
            if (welcomeSub != null) {
                boolean en = isEnglish;
                welcomeSub.setText(activeCount > 0
                    ? (en ? "You have " + activeCount + " active subjects this semester."
                          : "Máš " + activeCount + " aktívnych predmetov tento semester.")
                    : (en ? "You have upcoming deadlines this week."
                          : "Máš nadchádzajúce termíny tento týždeň."));
            }
        });
    }

    private void handleDashboardMarks(JsonNode node) {
        WebSocketClientService.getInstance().unsubscribe(subDashMarks);
        // Marks are displayed in GradesController – here we just update badge count
        JsonNode data = node.path("data");
        int count = data.isArray() ? data.size() : 0;
        Platform.runLater(() -> {
            // Update notification badge with real marks count if > 0
            if (notificationsAction != null && count > 0) {
                // Find badge label and update
                notificationsAction.getChildren().stream()
                    .filter(n -> n instanceof javafx.scene.control.Label lbl
                        && lbl.getStyleClass().contains("notification-badge"))
                    .findFirst()
                    .ifPresent(n -> ((javafx.scene.control.Label) n).setText(String.valueOf(Math.min(count, 9))));
            }
        });
    }

    /** Aktualizuje welcome banner s menom používateľa. */
    private void updateWelcomeBanner() {
        boolean en      = isEnglish;
        boolean teacher = UserSession.get().isTeacher();
        boolean admin   = UserSession.get().isAdmin();
        String  name    = UserSession.get().getFullName();

        if (welcomeTitle == null) return;

        if (admin) {
            welcomeTitle.setText(en ? "Welcome, Administrator 👋" : "Vitajte, Administrátor 👋");
        } else if (teacher) {
            welcomeTitle.setText((en ? "Welcome, " : "Vitajte, ") + name + " 👋");
        } else {
            welcomeTitle.setText((en ? "Welcome back, " : "Vitaj späť, ") + name + " 👋");
        }
    }

    // ── NAV HANDLERS ──────────────────────────────────────────────

    @FXML private void handleDashboard() {
        setActiveNavItem("dashboard");
        if (mainScroll != null && dashboardContent != null)
            mainScroll.setContent(dashboardContent);
    }

    @FXML private void handleGrades() {
        setActiveNavItem("grades");
        if (UserSession.get().isTeacher() || UserSession.get().isAdmin()) {
            loadView("teacher-grades-view.fxml");
        } else {
            loadView("grades-view.fxml");
        }
    }

    @FXML private void handleCalendar() {
        setActiveNavItem("calendar");
        loadView("school-calendar-view.fxml");
    }

    @FXML private void handleProgress() {
        setActiveNavItem("progress");
        loadView("progress-view.fxml");
    }

    @FXML private void handleAlgebra() {
        setActiveNavItem("algebra");
        loadView("submissions-view.fxml"); // Changed from course-detail-view.fxml
    }

    @FXML private void handleCourses() {
        setActiveNavItem("courses");
        loadView("courses-view.fxml");
    }

    @FXML private void handleSettings() {
        setActiveNavItem("settings");
        loadView("settings-view.fxml");
    }

    @FXML private void handleLogout() {
        try {
            UserSession.get().setRole(UserSession.Role.STUDENT);
            UserSession.get().setFirstName("");
            UserSession.get().setLastName("");
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("login-view.fxml"));
            loader.setCharset(java.nio.charset.StandardCharsets.UTF_8);
            Scene loginScene = new Scene(loader.load(), 1280, 800);
            loginScene.getStylesheets().add(
                getClass().getResource(isDarkMode ? "/dark.css" : "/light.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) darkModeToggle.getScene().getWindow();
            stage.setTitle("BAIS – Lepší Akademický Systém");
            stage.setScene(loginScene);
            if (!stage.isMaximized()) stage.setMaximized(true);

            // Workaround pre fullscreen problém po odhlásení
            Platform.runLater(() -> {
                stage.setMaximized(false); // Zmenší okno na normálny stav
                stage.setMaximized(true);  // Okamžite ho znova maximalizuje
            });

        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Helper: načítaj FXML do scroll pane ──────────────────────

    private void loadView(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof SettingsController sc) {
                sc.setDashboardController(this);
            }

            if (mainScroll != null) mainScroll.setContent(view);
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
        if (mainScroll != null) mainScroll.setContent(sp);
    }

    // ── Aktívna nav položka ───────────────────────────────────────

    private void setActiveNavItem(String active) {
        VBox[] all = {dashboardItem, gradesItem, calendarItem, progressItem,
                      algebraItem, coursesItem, settingsItem};
        for (VBox v : all) {
            if (v != null) v.getStyleClass().removeAll("nav-item-active");
        }
        VBox target = switch (active) {
            case "dashboard" -> dashboardItem;
            case "grades"    -> gradesItem;
            case "calendar"  -> calendarItem;
            case "progress"  -> progressItem;
            case "algebra"   -> algebraItem;
            case "courses"   -> coursesItem;
            case "settings"  -> settingsItem;
            default          -> null;
        };
        if (target != null) target.getStyleClass().add("nav-item-active");
    }

    // ── Téma a jazyk ──────────────────────────────────────────────

    public void toggleLanguage() {
        isEnglish = !isEnglish;
        UserSession.get().setEnglish(isEnglish);
        updateLanguage();
        if (UserSession.get().isTeacher() || UserSession.get().isAdmin()) applyTeacherUI();
        updateWelcomeBanner();
    }

    public void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        Scene scene = darkModeToggle.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(
                getClass().getResource(isDarkMode ? "/dark.css" : "/light.css").toExternalForm());
        }
        updateDarkModeButton();
    }

    private void applyTeacherUI() {
        boolean en   = isEnglish;
        UserSession.Role role = UserSession.get().getRole();

        if (headerSubtitle != null) {
            headerSubtitle.setText(role == UserSession.Role.ADMIN
                ? (en ? "Admin Portal" : "Portál administrátora")
                : (en ? "Teacher Portal" : "Portál učiteľa"));
        }

        if (progressItem != null) { progressItem.setVisible(false); progressItem.setManaged(false); }
        if (algebraItem  != null) { algebraItem.setVisible(false);  algebraItem.setManaged(false); }

        if (navGrades != null)
            navGrades.setText(en ? "Student Grades" : "Známky študentov");

        if (welcomeSub != null) {
            if (role == UserSession.Role.ADMIN) {
                welcomeSub.setText(en
                    ? "System is running normally. You have full access."
                    : "Systém beží normálne. Máte plný prístup.");
            } else {
                welcomeSub.setText(en
                    ? "Click 'Student Grades' to view results for your subjects."
                    : "Kliknite na 'Známky študentov' pre prehľad výsledkov vašich predmetov.");
            }
        }

        if (perfTitle   != null) { hideParentCard(perfTitle); }
        if (degreeTitle != null) { hideParentCard(degreeTitle); }
    }

    private void hideParentCard(javafx.scene.Node node) {
        javafx.scene.Node n = node;
        while (n != null && !(n instanceof VBox vb && vb.getStyleClass().contains("section-card"))) {
            n = n.getParent();
        }
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }

    private void updateLanguage() {
        boolean en      = isEnglish;
        boolean teacher = UserSession.get().isTeacher();

        if (headerTitle    != null) headerTitle.setText(en ? "Academic Info"  : "Akademické Info");
        if (headerSubtitle != null) headerSubtitle.setText(
            teacher ? (en ? "Teacher Portal" : "Portál učiteľa")
                    : (en ? "Student Portal"  : "Študentský Portál"));
        if (searchField    != null) searchField.setPromptText(en ? "Search AIS Desktop..." : "Hľadať AIS Desktop...");

        if (navDashboard != null) navDashboard.setText(en ? "Dashboard"   : "Prehľad");
        if (navGrades    != null) navGrades.setText(
            teacher ? (en ? "Student Grades" : "Známky študentov")
                    : (en ? "Grades"          : "Hodnotenia"));
        if (navCalendar  != null) navCalendar.setText(en ? "Calendar"    : "Kalendár");
        if (navProgress  != null) navProgress.setText(en ? "Progress"    : "Progres");
        if (navAlgebra   != null) navAlgebra.setText(en  ? "Assignments" : "Odovzdanie");
        if (navCourses   != null) navCourses.setText(en  ? "Courses"     : "Kurzy");
        if (navSettings  != null) navSettings.setText(en ? "Settings"    : "Nastavenia");
        if (navLogout    != null) navLogout.setText(en   ? "Logout"      : "Odhlásiť");

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
        if (schedTitle  != null) schedTitle.setText(en  ? "Today's Schedule"   : "Rozvrh dnes");
        if (alertsTitle != null) alertsTitle.setText(en ? "Alerts"             : "Upozornenia");
        if (perfTitle   != null) perfTitle.setText(en   ? "Recent Performance" : "Výsledky predmetov");
        if (degreeTitle != null) degreeTitle.setText(en ? "Degree Progress"    : "Progres štúdium");
        if (quickTitle  != null) quickTitle.setText(en  ? "Quick Links"        : "Rýchle odkazy");
    }

    private void updateDarkModeButton() {
        if (darkModeToggle != null) darkModeToggle.setText(isDarkMode ? "☀" : "🌙");
    }

    @FXML private void handleNotifications() {
        NotificationWindow.show();
    }

    private void handleSearch(String query) {
        // Placeholder for search functionality
        System.out.println("Searching for: " + query);
        // For now, show a placeholder message
        showPlaceholder("Search for '" + query + "' - feature in development");
    }
}
