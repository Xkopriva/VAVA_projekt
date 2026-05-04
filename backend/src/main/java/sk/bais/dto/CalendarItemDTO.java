package sk.bais.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sk.bais.model.Event;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarItemDTO {
    // Identifikácia zdroja (môže sa hodiť pre preklik do detailu)
    private String sourceType; // "EVENT" alebo "TASK"
    private int sourceId;

    // Zjednotené dáta pre kalendár
    private String type;            // PREDNASKA, CVICENIE, TASK_DUE, atď.
    private String title;           // Preložený názov eventu alebo priamo title tasku
    private String description;
    private OffsetDateTime scheduledAt; // Mapujeme sem Event.scheduledAt ALEBO Task.dueAt
    private Integer durationMinutes;    // Pre tasky môžeme dať default napr. 0 alebo 30
    private String room;                // Len pre eventy
    private int subjectId;
}