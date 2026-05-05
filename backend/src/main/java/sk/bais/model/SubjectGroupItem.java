package sk.bais.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model trieda mapovana na tabulku subject_group_item.
 * Kompozitny PK: (subject_group_id, subject_id) — junction medzi subject_group a subject.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectGroupItem {

    private int subjectGroupId;     // FK -> subject_group
    private int subjectId;          // FK -> subject
    @JsonProperty("isMandatory")
    private boolean isMandatory;    // ci je predmet povinny v ramci tejto skupiny, DEFAULT FALSE
}
