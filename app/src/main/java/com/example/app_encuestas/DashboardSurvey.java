package com.example.app_encuestas;

public class DashboardSurvey {
    private final String title;
    private final String subtitle;

    public DashboardSurvey(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
