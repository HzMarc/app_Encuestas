package com.example.app_encuestas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SurveyDraft {
    public final String id;
    public final String title;
    public final String description;
    public final String category;
    public final long updatedAt;
    public final List<GoogleFormsApi.SurveyQuestion> questions;

    public SurveyDraft(String id, String title, String description, String category,
                       long updatedAt, List<GoogleFormsApi.SurveyQuestion> questions) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id;
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
        this.category = category == null ? "Servicios" : category;
        this.updatedAt = updatedAt;
        this.questions = new ArrayList<>(questions == null ? new ArrayList<>() : questions);
    }
}
