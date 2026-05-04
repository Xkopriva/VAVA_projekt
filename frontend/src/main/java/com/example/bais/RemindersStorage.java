package com.example.bais;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Ulúči a načítava pripomienky z loklního úložiště (JSON soubor).
 */
public class RemindersStorage {
    private static final String REMINDERS_DIR = System.getProperty("user.home") + "/.bais/reminders";
    private static final String REMINDERS_FILE = REMINDERS_DIR + "/reminders.txt";

    static {
        try {
            Files.createDirectories(Paths.get(REMINDERS_DIR));
        } catch (IOException e) {
            System.err.println("Could not create reminders directory: " + e.getMessage());
        }
    }

    /**
     * Ulož pripomienky do súboru (jeden riadok = jedna pripomienka)
     * Format: type|title|time|dayIndex|userId
     */
    public static void saveReminders(List<CalendarReminder> reminders, String userId) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(REMINDERS_FILE))) {
            for (CalendarReminder r : reminders) {
                String line = String.join("|",
                    r.type,
                    r.title,
                    r.time,
                    String.valueOf(r.dayIndex),
                    userId
                );
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error saving reminders: " + e.getMessage());
        }
    }

    /**
     * Načítaj pripomienky z súboru
     */
    public static List<CalendarReminder> loadReminders(String userId) {
        List<CalendarReminder> reminders = new ArrayList<>();
        Path filePath = Paths.get(REMINDERS_FILE);

        if (!Files.exists(filePath)) {
            return reminders;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|");
                if (parts.length >= 5 && parts[4].equals(userId)) {
                    reminders.add(new CalendarReminder(
                        parts[0],  // type
                        parts[1],  // title
                        parts[2],  // time
                        Integer.parseInt(parts[3])  // dayIndex
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading reminders: " + e.getMessage());
        }

        return reminders;
    }

    /**
     * Vymaž všetky pripomienky konkrétneho používateľa
     */
    public static void clearReminders(String userId) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(REMINDERS_FILE))) {
            List<String> lines = Files.readAllLines(Paths.get(REMINDERS_FILE));
            for (String line : lines) {
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5 && !parts[4].equals(userId)) {
                        writer.println(line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error clearing reminders: " + e.getMessage());
        }
    }

    /**
     * Vnútorná trieda reprezentujúca jednu pripomienku
     */
    public static class CalendarReminder {
        public final String type;
        public final String title;
        public final String time;
        public final int dayIndex;

        public CalendarReminder(String type, String title, String time, int dayIndex) {
            this.type = type;
            this.title = title;
            this.time = time;
            this.dayIndex = dayIndex;
        }
    }
}

