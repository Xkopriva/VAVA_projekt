package sk.bais.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.bais.model.Event;

@Data
@AllArgsConstructor
public class EventWithTranslationDTO {
    private Event event;        // Pôvodné dáta (id, subjectId, scheduledAt, room, ...)
    private String title;       // Preložený názov z event_translation
    private String description; // Preložený popis z event_translation
}