package com.example.bais;

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

    // Nav labels (for language switching)
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Pick up language chosen on the login screen
        isEnglish = UserSession.get().isEnglish();

        updateLanguage();
        updateDarkModeButton();
        setActiveNavItem("dashboard");

        // Adapt navigation and welcome section for teacher/admin role
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
    }

    private void handleSearch(String query) {
        if (query.contains("nastaven") || query.contains("settings")) {
            handleSettings();
        } else if (query.contains("znamk") || query.contains("grad") || query.contains("hodnoten")) {
            handleGrades();
        } else if (query.contains("kalendar") || query.contains("calend")) {
            handleCalendar();
        } else if (query.contains("kurz") || query.contains("cours")) {
            handleCourses();
        } else if (query.contains("progres") || query.contains("prog")) {
            handleProgress();
        } else if (query.contains("prehlad") || query.contains("dash")) {
            handleDashboard();
        }
    }

    /** Hide student-only nav items and adjust labels for teacher/admin. */
    private void applyTeacherUI() {
        boolean en = isEnglish;
        UserSession.Role role = UserSession.get().getRole();

        // Re-label the header subtitle
        if (headerSubtitle != null) {
            if (role == UserSession.Role.ADMIN) {
                headerSubtitle.setText(en ? "Admin Portal" : "Portál administrátora");
            } else {
                headerSubtitle.setText(en ? "Teacher Portal" : "Portál učiteľa");
            }
        }

        // Teachers/Admins have no personal progress or algebra detail
        if (progressItem != null) progressItem.setVisible(false);
        if (progressItem != null) progressItem.setManaged(false);
        if (algebraItem  != null) algebraItem.setVisible(false);
        if (algebraItem  != null) algebraItem.setManaged(false);

        // Re-label the "Grades" nav item to "Student Grades"
        if (navGrades != null)
            navGrades.setText(en ? "Student Grades" : "Známky študentov");

        // Adapt the welcome banner
        if (welcomeTitle != null) {
            if (role == UserSession.Role.ADMIN) {
                welcomeTitle.setText(en ? "Welcome, Administrator 👋" : "Vitajte, Administrátor 👋");
            } else {
                welcomeTitle.setText(en ? "Welcome, Prof. Horváth 👋" : "Vitajte, Doc. Horváth 👋");
            }
        }
        
        if (welcomeSub != null) {
            if (role == UserSession.Role.ADMIN) {
                welcomeSub.setText(en 
                    ? "System is running normally. You have full access." 
                    : "Systém beží normálne. Máte plný prístup.");
            } else {
                welcomeSub.setText(en
                    ? "You teach 3 subjects this semester. Click 'Student Grades' to view results."
                    : "Tento semester vyučujete 3 predmety. Kliknite na 'Známky študentov' pre prehľad výsledkov.");
            }
        }

        // Hide student-only dashboard panels (perf / degree progress)
        if (perfTitle    != null) { hideParentCard(perfTitle); }
        if (degreeTitle  != null) { hideParentCard(degreeTitle); }
    }

    /** Walk up to the nearest section-card VBox and hide it. */
    private void hideParentCard(javafx.scene.Node node) {
        javafx.scene.Node n = node;
        while (n != null && !(n instanceof VBox vb && vb.getStyleClass().contains("section-card"))) {
            n = n.getParent();
        }
        if (n != null) { n.setVisible(false); n.setManaged(false); }
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
        loadView("course-detail-view.fxml");
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
            UserSession.get().setRole(UserSession.Role.STUDENT); // reset session
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Scene loginScene = new Scene(loader.load(), 1280, 850);
            loginScene.getStylesheets().add(
                getClass().getResource(isDarkMode ? "/dark.css" : "/light.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) darkModeToggle.getScene().getWindow();
            stage.setScene(loginScene);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── HELPER: load an FXML into the scroll pane ─────────────────

    private void loadView(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
            Node view = loader.load();
            
            // Pass reference to this controller if loading settings
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

    // ── ACTIVE NAV ────────────────────────────────────────────────

    private void setActiveNavItem(String active) {
        VBox[] all = {dashboardItem, gradesItem, calendarItem, progressItem,
                      algebraItem, coursesItem, settingsItem};
        for (VBox v : all) {
            if (v != null) v.getStyleClass().removeAll("nav-item-active");
        }
        VBox target = switch (active) {
            case "dashboard"  -> dashboardItem;
            case "grades"     -> gradesItem;
            case "calendar"   -> calendarItem;
            case "progress"   -> progressItem;
            case "algebra"    -> algebraItem;
            case "courses"    -> coursesItem;
            case "settings"   -> settingsItem;
            default           -> null;
        };
        if (target != null) target.getStyleClass().add("nav-item-active");
    }

    // ── THEME / LANGUAGE ──────────────────────────────────────────
    
    public void toggleLanguage() {
        isEnglish = !isEnglish;
        UserSession.get().setEnglish(isEnglish);
        updateLanguage();
        if (UserSession.get().isTeacher()) applyTeacherUI();
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

    private void updateLanguage() {
        boolean en = isEnglish;
        boolean teacher = UserSession.get().isTeacher();

        // Header
        if (headerTitle    != null) headerTitle.setText(en ? "Academic Info"    : "Akademické Info");
        if (headerSubtitle != null) headerSubtitle.setText(
            teacher ? (en ? "Teacher Portal" : "Portál učiteľa")
                    : (en ? "Student Portal"  : "Študentský Portál"));
        if (searchField    != null) searchField.setPromptText(en ? "Search AIS Desktop..." : "Hľadať AIS Desktop...");

        // Nav labels
        if (navDashboard != null) navDashboard.setText(en ? "Dashboard"  : "Prehľad");
        if (navGrades    != null) navGrades.setText(
            teacher ? (en ? "Student Grades" : "Známky študentov")
                    : (en ? "Grades"          : "Hodnotenia"));
        if (navCalendar  != null) navCalendar.setText(en  ? "Calendar"   : "Kalendár");
        if (navProgress  != null) navProgress.setText(en  ? "Progress"   : "Progres");
        if (navAlgebra   != null) navAlgebra.setText(en   ? "Assignments" : "Odovzdanie");
        if (navCourses   != null) navCourses.setText(en   ? "Courses"    : "Kurzy");
        if (navSettings  != null) navSettings.setText(en  ? "Settings"   : "Nastavenia");
        if (navLogout    != null) navLogout.setText(en    ? "Logout"     : "Odhlásiť");

        // Dashboard content (student-specific labels may be null for teacher)
        if (welcomeTitle != null) welcomeTitle.setText(
            teacher ? (en ? "Welcome, Prof. Horváth 👋" : "Vitajte, Doc. Horváth 👋")
                    : (en ? "Welcome back, Adam Novák 👋" : "Vitaj späť, Adam Novák 👋"));
        if (welcomeSub != null) welcomeSub.setText(
            teacher
                ? (en ? "You teach 3 subjects this semester. Click 'Student Grades' to view results."
                       : "Tento semester vyučujete 3 predmety. Kliknite na 'Známky študentov' pre prehľad výsledkov.")
                : (en ? "You have 3 upcoming deadlines this week. Semester is 74% complete."
                       : "Máš 3 nadchádzajúce termíny tento týždeň. Semester je z 74% hotový."));
        if (schedTitle   != null) schedTitle.setText(en   ? "Today's Schedule"   : "Rozvrh dnes");
        if (alertsTitle  != null) alertsTitle.setText(en  ? "Alerts"             : "Upozornenia");
        if (perfTitle    != null) perfTitle.setText(en    ? "Recent Performance"  : "Výsledky predmetov");
        if (degreeTitle  != null) degreeTitle.setText(en  ? "Degree Progress"     : "Progres štúdium");
        if (quickTitle   != null) quickTitle.setText(en   ? "Quick Links"         : "Rýchle odkazy");
    }

    private void updateDarkModeButton() {
        if (darkModeToggle != null) darkModeToggle.setText(isDarkMode ? "☀" : "🌙");
    }

    @FXML private void handleNotifications() {
        NotificationWindow.show();
    }
}
