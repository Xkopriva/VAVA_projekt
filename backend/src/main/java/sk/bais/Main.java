package sk.bais;

import sk.bais.dao.StudentDAO;
// Importuj ostatné DAO podľa potreby

public class Main {

    public static void main(String[] args) {
        System.out.println("Backend sa spúšťa...");

        // 1. Simulácia prihlásenia (v reále údaje z UI alebo konzoly)
        String username = "jan_mrkva";
        
        // 2. Zistenie typu užívateľa z DB
        // Tu použiješ nejakú centrálnu metódu na overenie
        String userType = getPlayerTypeFromDB(username); 

        System.out.println("Prihlásený používateľ: " + username + " (Typ: " + userType + ")");

        // 3. Logika vetvenia podľa typu
        switch (userType.toLowerCase()) {
            case "student":
                handleStudentLogic(username);
                break;
            case "teacher":
                // handleTeacherLogic(username);
                break;
            case "admin":
                // handleAdminLogic(username);
                break;
            default:
                System.out.println("Neznámy typ používateľa!");
        }
    }

    private static String getPlayerTypeFromDB(String username) {
        // Tu by bola tvoja SQL logika: SELECT type FROM users WHERE username = ...
        return "student"; // Simulovaný výsledok
    }

    private static void handleStudentLogic(String username) {
        // Tu inicializuješ DAO pre študenta a spustíš jeho funkcionalitu
        StudentDAO studentDAO = new StudentDAO();
        System.out.println("Načítavam dáta pre študenta...");
        // studentDAO.findAll();
    }
}